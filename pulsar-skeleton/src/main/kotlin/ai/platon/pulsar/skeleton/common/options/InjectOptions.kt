package ai.platon.pulsar.skeleton.common.options

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import com.beust.jcommander.Parameter
import java.util.*

/**
 * Command options for inject tasks.
 * Expect the list option which specify the seed or seed file, the @ sign is not supported
 */
class InjectOptions(argv: Array<String>, conf: ImmutableConfig) : CommonOptions(argv) {
    init {
        // We may read seeds from a file using @ sign, the file parsing should be handled manually
        expandAtSign = false
        // if acceptUnknownOptions is true, the main parameter (seeds) is not recognized
        acceptUnknownOptions = false
    }

    @Parameter(description = "<seeds> \nSeed urls. You can use {@code @FILE} syntax to read from file.")
    var seeds: List<String> = ArrayList()

    @Parameter(names = ["-slashed"], description = "An url can be in multiple lines in seed file if slashed is true")
    var slashed = false

    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "The crawl id, (default : \"storage.crawl.id\").")
    var crawlId = conf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "task limit")
    var limit = Int.MAX_VALUE / 2

    override fun getParams(): Params {
        return Params.of(
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_SEEDS, if (seeds.isEmpty()) "" else seeds[0],
                PulsarParams.ARG_LIMIT, limit
        )
    }
}
