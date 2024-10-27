package ai.platon.pulsar.skeleton.crawl.common.url

import ai.platon.pulsar.common.urls.StatefulHyperlink
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import ai.platon.pulsar.skeleton.crawl.event.impl.PageEventHandlersFactory
import java.time.Duration
import java.time.Instant

/**
 * A stateful hyperlink that has status and contains a [PageEventHandlers] to handle page events.
 * */
open class StatefulListenableHyperlink(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text
     * */
    text: String = "",
    /**
     * The order of this hyperlink in it referrer page
     * */
    order: Int = 0,
    /**
     * The url of the referrer page
     * */
    referrer: String? = null,
    /**
     * The additional url arguments
     * */
    args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    href: String? = null,
    /**
     * The priority of this hyperlink
     * */
    priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    country: String = "*",
    /**
     * The district of this hyperlink
     * */
    district: String = "*",
    /**
     * The maximum number of retries
     * */
    nMaxRetry: Int = 3,
    /**
     * The depth of this hyperlink
     * */
    depth: Int = 0,
    /**
     * The event handler
     * */
    override var event: PageEventHandlers = PageEventHandlersFactory().create()
): StatefulHyperlink(url, text, order, referrer, args, href, priority, lang, country, district, nMaxRetry, depth),
    ListenableUrl {

    override val isPersistable: Boolean = false

    val idleTime get() = Duration.between(modifiedAt, Instant.now())
}