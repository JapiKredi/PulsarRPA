package ai.platon.pulsar.skeleton.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.*
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import java.time.Instant

val WebPage.event: PageEventHandlers?
    get() = this.options.rawEvent

val WebPage.crawlEvent: CrawlEventHandlers?
    get() = this.options.rawEvent?.crawlEventHandlers

val WebPage.loadEvent: LoadEventHandlers?
    get() = this.options.rawEvent?.loadEventHandlers

val WebPage.browseEvent: BrowseEventHandlers?
    get() = this.options.rawEvent?.browseEventHandlers

/**
 * Get or create a LoadOptions from the args
 *
 * @return a LoadOptions object.
 */
val WebPage.options: LoadOptions
    get() {
        return variables.variables.computeIfAbsent(VAR_LOAD_OPTIONS) {
            LoadOptions.parse(args, conf)
        } as LoadOptions
    }

/**
 * Get the page label
 */
val WebPage.label: String get() = options.label

fun WebPage.updateFetchTime(prevFetchTime: Instant, fetchTime: Instant) {
    this.prevFetchTime = prevFetchTime
    // the next time supposed to fetch
    this.fetchTime = fetchTime

    val pageExt = WebPageExt(this)
    pageExt.updateFetchTimeHistory(fetchTime)
}
