package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.skeleton.crawl.common.WeakPageIndexer
import ai.platon.pulsar.skeleton.crawl.inject.SeedBuilder
import ai.platon.pulsar.persist.WebDBException
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 17-5-14.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class InjectComponent(
        private val seedBuilder: SeedBuilder,
        val webDb: WebDb,
        val conf: ImmutableConfig
) : Parameterized, AutoCloseable {
    private val seedIndexer: WeakPageIndexer = WeakPageIndexer(AppConstants.SEED_HOME_URL, webDb)
    private val closed = AtomicBoolean(false)

    constructor(webDb: WebDb, conf: ImmutableConfig): this(SeedBuilder(conf), webDb, conf)

    override fun getParams(): Params {
        return seedBuilder.params
    }

    @Throws(WebDBException::class)
    fun inject(urlArgs: Pair<String, String>): WebPage {
        return inject(urlArgs.first, urlArgs.second)
    }

    @Throws(WebDBException::class)
    fun inject(normURL: NormURL): WebPage {
        return inject(normURL.spec, normURL.args)
    }

    @Throws(WebDBException::class)
    fun inject(url: String, args: String): WebPage {
        var page = webDb.get(url, false)

        if (page.isNil) {
            page = seedBuilder.create(url, args)
            if (page.isSeed) {
                webDb.put(page)
                seedIndexer.index(page.url)
            }
            return page
        }

        // already exist in db, update the status and mark it as a seed
        page.args = args
        return if (inject(page)) page else WebPage.NIL
    }

    @Throws(WebDBException::class)
    fun inject(page: WebPage): Boolean {
        val success = seedBuilder.makeSeed(page)
        if (success) {
            webDb.put(page)
            seedIndexer.index(page.url)
            return true
        }

        return false
    }

    @Throws(WebDBException::class)
    fun injectAll(vararg configuredUrls: String): List<WebPage> {
        return configuredUrls.map { inject(UrlUtils.splitUrlArgs(it)) }
    }

    @Throws(WebDBException::class)
    fun injectAll(pages: Collection<WebPage>): List<WebPage> {
        val injectedPages = pages.onEach { inject(it) }.filter { it.isSeed }
        seedIndexer.indexAll(injectedPages.map { it.url })
        LOG.info("Injected " + injectedPages.size + " seeds out of " + pages.size + " pages")
        return injectedPages
    }

    @Throws(WebDBException::class)
    fun unInject(url: String): WebPage {
        val page = webDb.get(url)
        if (page.isSeed) {
            unInject(page)
        }
        return page
    }

    @Throws(WebDBException::class)
    fun unInject(page: WebPage): WebPage {
        if (!page.isSeed) {
            return page
        }
        page.unmarkSeed()
        page.marks.remove(Mark.INJECT)
        seedIndexer.remove(page.url)
        webDb.put(page)
        return page
    }

    @Throws(WebDBException::class)
    fun unInjectAll(vararg urls: String): List<WebPage> {
        val pages = urls.mapNotNull { webDb.getOrNull(it) }.filter { it.isSeed }.onEach { unInject(it) }
        LOG.debug("UnInjected " + pages.size + " urls")
        seedIndexer.removeAll(pages.map { it.url })
        return pages
    }

    @Throws(WebDBException::class)
    fun unInjectAll(pages: Collection<WebPage>): Collection<WebPage> {
        pages.forEach { this.unInject(it) }
        return pages
    }

    fun report(): String {
        val seedHome = webDb.get(AppConstants.SEED_PAGE_1_URL)
        if (seedHome.isNil) {
            val count = seedHome.liveLinks.size
            return "Total " + count + " seeds in store " + webDb.schemaName
        }
        return "No home page"
    }

    @Throws(WebDBException::class)
    fun commit() {
        webDb.flush()
        seedIndexer.commit()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { commit() }.onFailure { warnForClose(this, it) }
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(InjectComponent::class.java)
    }
}
