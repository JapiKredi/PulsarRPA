package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.browser.BrowserFiles.PID_FILE_NAME
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.common.concurrent.RuntimeShutdownHookRegistry
import ai.platon.pulsar.common.concurrent.ShutdownHookRegistry
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * The chrome launcher
 * */
class ChromeLauncher(
    val userDataDir: Path = BrowserSettings.generateUserDataDir(),
    val options: LauncherOptions = LauncherOptions(),
    private val shutdownHookRegistry: ShutdownHookRegistry = RuntimeShutdownHookRegistry()
) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(ChromeLauncher::class.java)
        private val isActive get() = AppContext.isActive && !Thread.currentThread().isInterrupted
        
        private val DEVTOOLS_LISTENING_LINE_PATTERN = Pattern.compile("^DevTools listening on ws://.+:(\\d+)/")
    }

    private val closed = AtomicBoolean()
    private val pidPath get() = userDataDir.resolveSibling(PID_FILE_NAME)
    private val temporaryUddExpiry = BrowserFiles.TEMPORARY_UDD_EXPIRY
    private var process: Process? = null
    private val shutdownHookThread = Thread {
        // System.err.println("Shutting down chrome process ...")
        
        // Shutdown by the upper layer
        // this.close()
    }

    /**
     * Launch the chrome
     * */
    fun launch(chromeBinaryPath: Path, options: ChromeOptions): RemoteChrome {
        kotlin.runCatching { prepareUserDataDir() }.onFailure {
            warnInterruptible(this, it, "Failed to prepare user data dir | {} | {}", userDataDir, it.message)
        }

        val port = launchChromeProcess(chromeBinaryPath, userDataDir, options)
        return ChromeImpl(port)
    }

    /**
     * Launch the chrome
     * */
    fun launch(options: ChromeOptions) = launch(Browsers.searchChromeBinary(), options)

    /**
     * Launch the chrome
     * */
    fun launch(headless: Boolean) = launch(Browsers.searchChromeBinary(), ChromeOptions().also { it.headless = headless })

    /**
     * Launch the chrome
     * */
    fun launch() = launch(true)

    fun destroyForcibly() {
        try {
            val pid = Files.readAllLines(pidPath).firstOrNull { it.isNotBlank() }?.toIntOrNull() ?: 0
            if (pid > 0) {
                logger.warn("Destroy chrome launcher forcibly, pid: {} | {}", pid, userDataDir)
                Runtimes.destroyProcessForcibly(pid)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            logger.warn(t.stringify())
        }
    }

    /**
     * Close the chrome process.
     * The method throws nothing by design.
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            val p = process ?: return
            this.process = null
            if (p.isAlive) {
                Runtimes.destroyProcess(p, options.shutdownWaitTime)
                kotlin.runCatching { shutdownHookRegistry.remove(shutdownHookThread) }
                        .onFailure { logger.warn("Unexpected exception", it) }
            }

            BrowserFiles.runCatching { cleanUpContextTmpDir(temporaryUddExpiry) }.onFailure { warnForClose(this, it) }
        }
    }

    /**
     * Returns an exit value. This is just proxy to [Process.exitValue].
     *
     * @return Exit value of the process if exited.
     * @throws [IllegalThreadStateException] if the subprocess has not yet terminated. [     ] If the process hasn't even started.
     */
    fun exitValue(): Int {
        checkNotNull(process) { "Chrome process has not been started" }
        return process!!.exitValue()
    }

    /**
     * Tests whether the subprocess is alive. This is just proxy to [Process.isAlive].
     *
     * @return True if the subprocess has not yet terminated.
     * @throws IllegalThreadStateException if the subprocess has not yet terminated.
     */
    val isAlive: Boolean get() = process?.isAlive == true

    /**
     * Launches a chrome process given a chrome binary and its arguments.
     *
     * Launching chrome processes is CPU consuming, so we do this in a synchronized manner
     *
     * @param chromeBinary Chrome binary path.
     * @param userDataDir Chrome user data dir.
     * @param chromeOptions Chrome arguments.
     * @return Port on which devtools is listening.
     * @throws IllegalStateException If chrome process has already been started.
     * @throws ChromeProcessException If an I/O error occurs during chrome process start.
     * @throws ChromeProcessTimeoutException If timeout expired while waiting for chrome to start.
     */
    @Throws(ChromeProcessException::class, IllegalStateException::class, ChromeProcessTimeoutException::class)
    @Synchronized
    private fun launchChromeProcess(chromeBinary: Path, userDataDir: Path, chromeOptions: ChromeOptions): Int {
        if (!isActive) {
            return 0
        }

        check(process == null) { "Chrome process has already been started" }
        check(!isAlive) { "Chrome process has already been started" }

        var supervisorProcess = options.supervisorProcess
        if (supervisorProcess != null && Runtimes.locateBinary(supervisorProcess).isEmpty()) {
            logger.warn("Supervisor program {} can not be located", options.supervisorProcess)
            supervisorProcess = null
        }

        val executable = supervisorProcess?:"$chromeBinary"
        var arguments = if (supervisorProcess == null) chromeOptions.toList() else {
            options.supervisorProcessArgs + arrayOf("$chromeBinary") + chromeOptions.toList()
        }.toMutableList()
        
        if (userDataDir.startsWith(AppPaths.USER_BROWSER_DATA_DIR_PLACEHOLDER)) {
            // Open the default browser just like a real user daily do,
            // open a blank page not to choose the profile
            val args = "--remote-debugging-port=0 --remote-allow-origins=* about:blank"
            arguments = args.split(" ").toMutableList()
        } else {
            arguments.add("--user-data-dir=$userDataDir")
        }

        return try {
            shutdownHookRegistry.register(shutdownHookThread)
            process = ProcessLauncher.launch(executable, arguments)

            process?.also {
                Files.createDirectories(userDataDir)
                Files.writeString(pidPath, it.pid().toString(), StandardOpenOption.CREATE)
            }
            waitForDevToolsServer(process!!)
        } catch (e: IllegalStateException) {
            shutdownHookRegistry.remove(shutdownHookThread)
            throw ChromeProcessException("Can not launch chrome", e)
        } catch (e: IOException) {
            // Unsubscribe from registry on exceptions.
            shutdownHookRegistry.remove(shutdownHookThread)
            throw ChromeProcessException("Failed to start chrome", e)
        } catch (e: Exception) {
            // Close the process if failed to start, it throws nothing by design.
            close()
            throw e
        }
    }

    /**
     * Waits for DevTools server is up on chrome process.
     *
     * @param process Chrome process.
     * @return DevTools listening port.
     * @throws ChromeProcessTimeoutException If timeout expired while waiting for chrome process.
     */
    @Throws(ChromeProcessTimeoutException::class)
    private fun waitForDevToolsServer(process: Process): Int {
        var port = 0
        val processOutput = StringBuilder()
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                // Wait for DevTools listening line and extract port number.
                var line: String
                while (reader.readLine().also { line = it } != null) {
                    logger.takeIf { line.isNotBlank() }?.info("[output] - $line")
                    val matcher = DEVTOOLS_LISTENING_LINE_PATTERN.matcher(line)
                    if (matcher.find()) {
                        port = matcher.group(1).toInt()
                        break
                    }
                    processOutput.appendLine(line)
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(options.startupWaitTime.toMillis())

            if (port == 0) {
                close(readLineThread)
                logger.info("Process output:>>>\n$processOutput\n<<<")
                throw ChromeProcessTimeoutException("Timeout to waiting for chrome to start")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted while waiting for devtools server, close it", e)
            close(readLineThread)
        }

        return port
    }

    private fun close(thread: Thread) {
        try {
            thread.join(options.threadWaitTime.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @Throws(IOException::class)
    private fun prepareUserDataDir() {
        val prototypeUserDataDir = AppPaths.CHROME_DATA_DIR_PROTOTYPE
        if (userDataDir == prototypeUserDataDir || userDataDir.toString().contains("/default/")) {
            logger.info("Running chrome with prototype/default data dir, no cleaning | {}", userDataDir)
            return
        }

        val lock = AppPaths.BROWSER_TMP_DIR_LOCK
        if (isActive && Files.exists(prototypeUserDataDir.resolve("Default"))) {
            FileChannel.open(lock, StandardOpenOption.APPEND).use {
                it.lock()

                if (!isActive) {
                    return
                }

                if (!Files.exists(userDataDir.resolve("Default"))) {
                    logger.info("User data dir does not exist, copy from prototype | {} <- {}", userDataDir, prototypeUserDataDir)
                    // remove dead symbolic links
                    Files.list(prototypeUserDataDir)
                        .filter { Files.isSymbolicLink(it) && !Files.exists(it) }
                        .forEach { Files.delete(it) }

                    // ISSUE#29: https://github.com/platonai/PulsarRPA/issues/29
                    // Failed to copy chrome data dir when there is a SingletonSocket symbol link
                    val fileFilter = FileFilter { !Files.isSymbolicLink(it.toPath()) }

//                    val fileFilter = { f: File -> !Files.isSymbolicLink(f.toPath())
//                        // Copy only the default profile directory
//                        && f.name == "Default"
//                    }
                    FileUtils.copyDirectory(prototypeUserDataDir.toFile(), userDataDir.toFile(), fileFilter)
                } else {
                    Files.deleteIfExists(userDataDir.resolve("Default/Cookies"))
                    val leveldb = userDataDir.resolve("Default/Local Storage/leveldb")
                    if (Files.exists(leveldb)) {
                        FileUtils.deleteDirectory(leveldb.toFile())
                    }

                    arrayOf("Default/Cookies", "Default/Local Storage/leveldb").forEach {
                        val target = userDataDir.resolve(it)
                        Files.createDirectories(target.parent)
                        Files.copy(prototypeUserDataDir.resolve(it), target, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }
}
