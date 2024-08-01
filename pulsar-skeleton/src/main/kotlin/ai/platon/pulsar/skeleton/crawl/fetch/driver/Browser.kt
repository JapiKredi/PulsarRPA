package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

/**
 * The Browser defines methods and events to manipulate a real browser.
 */
interface Browser: AutoCloseable {
    /**
     * The unique browser id
     * */
    val id: BrowserId
    /**
     * The user agent. A user agent is a string that a browser sends to each website you visit.
     * It's created when the browser first connected to the remote browser.
     * */
    val userAgent: String
    /**
     * The navigation history.
     * */
    val navigateHistory: NavigateHistory
    /**
     * The created drivers by this browser.
     * */
    val drivers: Map<String, WebDriver>
    /**
     * The associated data.
     * */
    val data: MutableMap<String, Any?>
    /**
     * Check if this browser is idle.
     * */
    val isIdle: Boolean
    /**
     * Create a new driver.
     * */
    @Throws(WebDriverException::class)
    fun newDriver(): WebDriver
    /**
     * Create a new driver.
     * */
    @Throws(WebDriverException::class)
    fun newDriver(url: String): WebDriver
    /**
     * List all drivers, each driver is associated with a Chrome tab.
     * */
    @Throws(WebDriverException::class)
    suspend fun listDrivers(): List<WebDriver>
    /**
     * Find a driver by url.
     * */
    @Throws(WebDriverException::class)
    suspend fun findDriver(url: String): WebDriver?
    /**
     * Find drivers by url regex.
     * */
    @Throws(WebDriverException::class)
    suspend fun findDrivers(urlRegex: Regex): WebDriver?
    /**
     * Destroy the web driver, close the associated browser tabs.
     * */
    @Throws(WebDriverException::class)
    fun destroyDriver(driver: WebDriver)
    /**
     * Destroy the browser forcibly, kill the associated browser processes, release all allocated resources,
     * regardless of whether the browser is closed or not.
     * */
    @Throws(WebDriverException::class)
    fun destroyForcibly()
    /**
     * Clear all cookies.
     * Notice: even if we clear all cookies, the website still has some technology to track a session.
     * */
    @Throws(WebDriverException::class)
    suspend fun clearCookies()
}
