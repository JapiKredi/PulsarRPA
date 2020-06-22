package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.emulator.context.BrowserPrivacyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class AsyncBrowserEmulator(
        privacyManager: BrowserPrivacyManager,
        eventHandlerFactory: BrowserEmulatorEventHandlerFactory,
        messageWriter: MiscMessageWriter,
        immutableConfig: ImmutableConfig
): BrowserEmulatorBase(privacyManager, eventHandlerFactory, messageWriter, immutableConfig) {
    private val log = LoggerFactory.getLogger(AsyncBrowserEmulator::class.java)!!

    val numDeferredNavigates = metrics.meter(prependReadableClassName(this, "deferredNavigates"))

    init {
        params.withLogger(log).info()
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalContextStateException Throw if the browser is closed or the program is closed
     * */
    @Throws(IllegalContextStateException::class)
    open suspend fun fetch(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        return takeIf { isActive }?.browseWithDriver(task, driver) ?: FetchResult.canceled(task)
    }

    open fun cancelNow(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverManager.cancel(task.url)
    }

    open suspend fun cancel(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        withContext(Dispatchers.IO) {
            driverManager.cancel(task.url)
        }
    }

    @Throws(IllegalContextStateException::class)
    protected open suspend fun browseWithDriver(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        checkState()

        if (task.nRetries > fetchMaxRetry) {
            return FetchResult.crawlRetry(task).also { log.info("Too many task retries, emit crawl retry | {}", task.url) }
        }

        var exception: Exception? = null
        var response: Response?

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: NavigateTaskCancellationException) {
            exception = e
            log.info("{}. Retry canceled task {}/{} in privacy scope later | {}", task.page.id, task.id, task.batchId, task.url)
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.takeIf { isActive }?.warn("Web driver session of #{} is closed | {}", driver.id, Strings.simplifyException(e))
            driver.retire()
            exception = e
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: org.openqa.selenium.WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                log.warn("Web driver is disconnected - {}", Strings.simplifyException(e))
            } else {
                log.warn("Unexpected WebDriver exception", e)
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PROTOCOL)
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            log.takeIf { isActive }?.warn("Web driver is disconnected", e)
            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PROTOCOL)
        } finally {
        }

        return FetchResult(task, response ?: ForwardingResponse(exception, task.page), exception)
    }

    @Throws(NavigateTaskCancellationException::class)
    private suspend fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState(task)
        checkState(driver)

        val navigateTask = NavigateTask(task, driver, driverControl)

        try {
            val interactResult = navigateAndInteract(task, driver, navigateTask.driverConfig)
            checkState(task)
            checkState(driver)
            navigateTask.pageDatum.apply {
                status = interactResult.protocolStatus
                activeDomMultiStatus = interactResult.activeDomMessage?.multiStatus
                activeDomUrls = interactResult.activeDomMessage?.urls
            }
            navigateTask.pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            navigateTask.pageDatum.status = ProtocolStatus.retry(RetryScope.PRIVACY)
        }

        return eventHandler.onAfterNavigate(navigateTask)
    }

    @Throws(NavigateTaskCancellationException::class,
            IllegalContextStateException::class,
            WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: BrowserControl): InteractResult {
        eventHandler.logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        withContext(Dispatchers.IO) {
            if (enableDelayBeforeNavigation) {
                val idleTime = Duration.between(lastNavigateTime, Instant.now())
                if (idleTime.seconds < 5) {
                    delay(2000L + Random.nextInt(0, 1000))
                }
            }

            meterNavigates.mark()
            numDeferredNavigates.mark()
            // tracer?.trace("About to navigate to #{} in {}", task.id, Thread.currentThread().name)
            lastNavigateTime = Instant.now()

            log.trace("Navigating | {}", task.url)

            driver.navigateTo(task.url)
        }

        val interactTask = InteractTask(task, driverConfig, driver)
        return takeIf { driverConfig.jsInvadingEnabled }?.interact(interactTask)?: interactNoJsInvaded(interactTask)
    }

    @Throws(NavigateTaskCancellationException::class, IllegalContextStateException::class)
    protected open suspend fun interactNoJsInvaded(interactTask: InteractTask): InteractResult {
        var pageSource = ""
        var i = 0
        do {
            withContext(Dispatchers.IO) {
                checkState(interactTask.driver)
                checkState(interactTask.fetchTask)
                counterRequests.inc()
                pageSource = interactTask.driver.pageSource

                if (pageSource.length < 20_000) {
                    delay(1000)
                }
            }
        } while (i++ < 45 && pageSource.length < 20_000)

        return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
    }

    @Throws(NavigateTaskCancellationException::class, IllegalContextStateException::class)
    protected open suspend fun interact(task: InteractTask): InteractResult {
        val result = InteractResult(ProtocolStatus.STATUS_SUCCESS, null)

        jsCheckDOMState(task, result)

        if (result.state.isContinue) {
            jsScrollDown(task, result)
        }

        if (result.state.isContinue) {
            jsComputeFeature(task, result)
        }

        return result
    }

    @Throws(NavigateTaskCancellationException::class)
    protected open suspend fun jsCheckDOMState(interactTask: InteractTask, result: InteractResult) {
        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = interactTask.driverConfig.scriptTimeout
        val fetchTask = interactTask.fetchTask

        // make sure the document is ready
        val initialScroll = 5
        val maxRound = scriptTimeout.seconds - 5 // leave 5 seconds to wait for script finish

        // TODO: wait for expected data, ni, na, nnum, nst, etc; required element
        val expression = "__utils__.waitForReady($maxRound, $initialScroll)"
        var message: Any? = null
        try {
            var msg: Any? = null
            var i = 0
            while ((msg == null || msg == false) && i++ < maxRound) {
                msg = evaluate(interactTask, expression)

                if (msg == null || msg == false) {
                    delay(500)
                }
            }
            message = msg
        } finally {
            if (message == null) {
                if (!fetchTask.isCanceled && !interactTask.driver.isQuit && !isActive) {
                    log.warn("Unexpected script result (null) | {}", interactTask.url)
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
                    result.state = FlowState.BREAK
                }
            } else if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val browserError = eventHandler.handleChromeErrorPage(message)
                status = browserError.status
                result.activeDomMessage = browserError.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                log.trace("DOM is ready {} | {}", message.toString().substringBefore("urls"), interactTask.url)
            }
        }

        result.protocolStatus = status
    }

    protected open suspend fun jsScrollDown(interactTask: InteractTask, result: InteractResult) {
        val random = ThreadLocalRandom.current().nextInt(3)
        var scrollDownCount = interactTask.driverConfig.scrollDownCount + random - 1
        scrollDownCount = scrollDownCount.coerceAtLeast(3)

        val expression = "__utils__.scrollDownN($scrollDownCount)"
        evaluate(interactTask, expression)
    }

    protected open suspend fun jsComputeFeature(interactTask: InteractTask, result: InteractResult) {
        val expression = "__utils__.compute()"
        val message = evaluate(interactTask, expression)

        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (log.isDebugEnabled) {
                val page = interactTask.fetchTask.page
                log.debug("{}. {} | {}", page.id, result.activeDomMessage?.multiStatus, interactTask.url)
            }
        }
    }

    private suspend fun evaluate(interactTask: InteractTask, expression: String): Any? {
        val scriptTimeout = interactTask.driverConfig.scriptTimeout

        return withContext(Dispatchers.IO) {
            checkState(interactTask.driver)
            checkState(interactTask.fetchTask)
            counterRequests.inc()
            withTimeout(scriptTimeout.toMillis()) {
                interactTask.driver.evaluate(expression)
            }
        }
    }
}
