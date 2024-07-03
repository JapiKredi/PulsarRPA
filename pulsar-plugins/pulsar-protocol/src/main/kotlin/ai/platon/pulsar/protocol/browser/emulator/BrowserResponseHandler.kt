package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.event.EventEmitter
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.util.ChainedHtmlIntegrityChecker
import ai.platon.pulsar.protocol.browser.emulator.util.ChainedPageCategorySniffer
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import ai.platon.pulsar.protocol.browser.emulator.util.PageCategorySniffer

enum class BrowserResponseEvents {
    initPageCategorySniffer,
    initHTMLIntegrityChecker,
    willCreateResponse,
    responseCreated,
    browseTimeout,
}

/**
 * The browser response handler. It's a component of the browser emulator, it's used to handle the response from
 * the browser.
 * */
interface BrowserResponseHandler: EventEmitter<BrowserResponseEvents> {
    /**
     * TODO: a better extension point to add sniffers
     * */
    val pageCategorySniffer: ChainedPageCategorySniffer
    /**
     * TODO: a better extension point to add checkers
     * */
    val htmlIntegrityChecker: ChainedHtmlIntegrityChecker

    /**
     * Normalize the page source.
     *
     * The browser has already converted source code to be UTF-8, so we replace the charset meta tags to be UTF-8.
     * TODO: or we insert a new metadata to indicate the charset
     */
    fun normalizePageSource(url: String, pageSource: String): StringBuilder

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error.
     * */
    fun createBrowserErrorResponse(message: String): BrowserErrorResponse

    fun createProtocolStatusForBrokenContent(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus

    fun checkErrorPage(page: WebPage, status: ProtocolStatus): ProtocolStatus

    fun onInitPageCategorySniffer(sniffer: PageCategorySniffer)

    fun onInitHTMLIntegrityChecker(checker: HtmlIntegrityChecker)

    fun onWillCreateResponse(task: FetchTask, driver: WebDriver)

    fun onResponseCreated(task: FetchTask, driver: WebDriver, response: Response)

    fun onBrowseTimeout(task: NavigateTask)
}
