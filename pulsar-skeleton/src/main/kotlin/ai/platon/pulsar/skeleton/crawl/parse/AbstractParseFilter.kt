/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.skeleton.crawl.parse

import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.skeleton.crawl.parse.html.ParseContext
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractParseFilter(
        final override val id: Int = instanceSequencer.incrementAndGet(),
        override var parent: ParseFilter? = null
): ParseFilter {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    override val children = mutableListOf<ParseFilter>()

    override fun initialize() {
    }

    override fun isRelevant(parseContext: ParseContext): CheckState {
        val status = parseContext.page.protocolStatus
        return if (status.isSuccess) {
            CheckState()
        } else {
            CheckState(status.minorCode, message = status.toString())
        }
    }

    override fun filter(parseContext: ParseContext): FilterResult {
        if (isRelevant(parseContext).isNotOK) {
            return FilterResult()
        }

        onBeforeFilter(parseContext)
        var result = doFilter(parseContext)
        onAfterFilter(parseContext)

        if (result.shouldContinue) {
            children.forEach {
                if (result.shouldContinue) {
                    result = it.filter(parseContext)
                }
            }
        }

        return result
    }

    protected abstract fun doFilter(parseContext: ParseContext): FilterResult

    override fun onBeforeFilter(parseContext: ParseContext) {

    }

    override fun onAfterFilter(parseContext: ParseContext) {

    }

    override fun addFirst(child: ParseFilter) {
        child.parent = this
        children.add(0, child)
    }

    override fun addLast(child: ParseFilter) {
        child.parent = this
        children.add(child)
    }

    override fun close() {
        children.forEach { it.close() }
        children.clear()
        super.close()
    }
}
