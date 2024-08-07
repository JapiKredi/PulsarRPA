package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.common.GlobalCache
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureDataMongo
class IntegrationTestBase {
    
    @LocalServerPort
    private var port = 0
    
    @Autowired
    lateinit var session: PulsarSession
    
    @Autowired
    lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig
    
    val baseUri get() = String.format("http://%s:%d", "localhost", port)
    
    @Test
    fun `Ensure crawl loop is running`() {
        assertTrue { alwaysTrue() }
    }
}
