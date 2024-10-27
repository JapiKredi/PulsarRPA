package ai.platon.pulsar.skeleton.crawl.parse.html

import ai.platon.pulsar.skeleton.crawl.parse.ParseResult
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

/**
 * Created by vincent on 17-7-28.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class ParseContext(
    val page: WebPage,
    var parseResult: ParseResult = ParseResult(),
) {
    val document get() = parseResult.document

    constructor(page: WebPage, document: FeaturedDocument) : this(page, ParseResult.success(document))
}
