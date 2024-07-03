package ai.platon.pulsar.skeleton.context.support

import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import org.springframework.context.support.AbstractApplicationContext

/**
 * The main entry point for pulsar functionality.
 *
 * A PulsarContext can be used to inject, fetch, load, parse, store Web pages.
 */
open class BasicPulsarContext(
    applicationContext: AbstractApplicationContext
) : AbstractPulsarContext(applicationContext) {

    @Throws(Exception::class)
    override fun createSession(): BasicPulsarSession {
        val session = BasicPulsarSession(this, unmodifiedConfig.toVolatileConfig())
        return session.also { sessions[it.id] = it }
    }
}
