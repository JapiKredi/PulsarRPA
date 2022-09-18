package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.crawl.fetch.driver.rpa.BrowseRPA
import ai.platon.pulsar.crawl.fetch.driver.rpa.DefaultBrowseRPA

abstract class AbstractLoadEvent(
    override val onFilter: UrlFilterEventHandler = UrlFilterEventHandler(),
    override val onNormalize: UrlFilterEventHandler = UrlFilterEventHandler(),
    override val onWillLoad: UrlEventHandler = UrlEventHandler(),
    override val onWillFetch: WebPageEventHandler = WebPageEventHandler(),
    override val onWillLaunchBrowser: WebPageEventHandler = WebPageEventHandler(),
    override val onBrowserLaunched: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFetched: WebPageEventHandler = WebPageEventHandler(),
    override val onWillParse: WebPageEventHandler = WebPageEventHandler(),
    override val onWillParseHTMLDocument: WebPageEventHandler = WebPageEventHandler(),
    override val onWillExtractData: WebPageEventHandler = WebPageEventHandler(),
    override val onDataExtracted: HTMLDocumentEventHandler = HTMLDocumentEventHandler(),
    override val onHTMLDocumentParsed: HTMLDocumentEventHandler = HTMLDocumentEventHandler(),
    override val onParsed: WebPageEventHandler = WebPageEventHandler(),
    override val onLoaded: WebPageEventHandler = WebPageEventHandler()
): LoadEvent {

    override fun combine(other: LoadEvent): AbstractLoadEvent {
        onFilter.addLast(other.onFilter)
        onNormalize.addLast(other.onNormalize)
        onWillLoad.addLast(other.onWillLoad)
        onWillFetch.addLast(other.onWillFetch)
        onWillLaunchBrowser.addLast(other.onWillLaunchBrowser)
        onBrowserLaunched.addLast(other.onBrowserLaunched)
        onFetched.addLast(other.onFetched)
        onWillParse.addLast(other.onWillParse)
        onWillParseHTMLDocument.addLast(other.onWillParseHTMLDocument)
        onWillExtractData.addLast(other.onWillExtractData)
        onDataExtracted.addLast(other.onDataExtracted)
        onHTMLDocumentParsed.addLast(other.onHTMLDocumentParsed)
        onParsed.addLast(other.onParsed)
        onLoaded.addLast(other.onLoaded)

        return this
    }
}

abstract class AbstractCrawlEvent(
    override val onFilter: UrlAwareEventFilter = UrlAwareEventFilter(),
    override val onNormalize: UrlAwareEventFilter = UrlAwareEventFilter(),
    override val onWillLoad: UrlAwareEventHandler = UrlAwareEventHandler(),
    override val onLoad: UrlAwareEventHandler = UrlAwareEventHandler(),
    override val onLoaded: UrlAwareWebPageEventHandler = UrlAwareWebPageEventHandler()
): CrawlEvent {
    override fun combine(other: CrawlEvent): CrawlEvent {
        onFilter.addLast(other.onFilter)
        onNormalize.addLast(other.onNormalize)
        onWillLoad.addLast(other.onWillLoad)
        onLoad.addLast(other.onLoad)
        onLoaded.addLast(other.onLoaded)
        return this
    }
}

abstract class AbstractSimulateEvent(
    override val onWillFetch: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFetched: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillNavigate: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onNavigated: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillCheckDOMState: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onDOMStateChecked: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillComputeFeature: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onFeatureComputed: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillInteract: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onDidInteract: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),

    override val onWillStopTab: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler(),
    override val onTabStopped: WebPageWebDriverEventHandler = WebPageWebDriverEventHandler()
): SimulateEvent {

    override fun combine(other: SimulateEvent): SimulateEvent {
        onWillFetch.addLast(other.onWillFetch)
        onFetched.addLast(other.onFetched)

        onWillNavigate.addLast(other.onWillNavigate)
        onNavigated.addLast(other.onNavigated)

        onWillCheckDOMState.addLast(other.onWillCheckDOMState)
        onDOMStateChecked.addLast(other.onDOMStateChecked)
        onWillComputeFeature.addLast(other.onWillComputeFeature)
        onFeatureComputed.addLast(other.onFeatureComputed)

        onWillInteract.addLast(other.onWillInteract)
        onDidInteract.addLast(other.onDidInteract)
        onWillStopTab.addLast(other.onWillStopTab)
        onTabStopped.addLast(other.onTabStopped)

        return this
    }
}

abstract class AbstractEmulateEvent(
    override val onSniffPageCategory: PageDatumEventHandler = PageDatumEventHandler(),
    override val onCheckHtmlIntegrity: PageDatumEventHandler = PageDatumEventHandler(),
): EmulateEvent {
    override fun combine(other: EmulateEvent): EmulateEvent {
        onSniffPageCategory.addLast(other.onSniffPageCategory)
        onCheckHtmlIntegrity.addLast(other.onCheckHtmlIntegrity)
        return this
    }
}

abstract class AbstractPageEvent(
    override val loadEvent: LoadEvent,
    override val simulateEvent: SimulateEvent,
    override val crawlEvent: CrawlEvent
): PageEvent {

    override fun combine(other: PageEvent): PageEvent {
        loadEvent.combine(other.loadEvent)
        simulateEvent.combine(other.simulateEvent)
        crawlEvent.combine(other.crawlEvent)
        return this
    }
}