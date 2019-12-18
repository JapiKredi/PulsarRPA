package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.*
import org.apache.commons.lang3.StringUtils
import java.util.*
import kotlin.system.exitProcess

@Parameters(commandNames = ["FetchJob"], commandDescription = "The most important switches for fetch jobs.")
class FetchOptions(conf: ImmutableConfig) {
    // TODO: crawlId is not used since AutoStorageService is started after Option parsing
    @Parameter(names = [PulsarParams.ARG_CRAWL_ID], description = "The id to prefix the schemas to operate on")
    var crawlId = conf.get(CapabilityTypes.STORAGE_CRAWL_ID, "")
    @Parameter(names = [PulsarParams.ARG_BATCH_ID], description = "If not specified, use last generated batch id.")
    var batchId = conf.get(CapabilityTypes.BATCH_ID, defaultBatchId)
    @Parameter(names = [PulsarParams.ARG_FETCH_MODE], description = "Fetch mode")
    var fetchMode = conf.getEnum(CapabilityTypes.FETCH_MODE, FetchMode.SELENIUM)
    @Parameter(names = [PulsarParams.ARG_STRICT_DF], description = "If true, crawl the web using strict depth-first strategy")
    var strictDf = false

    @Parameter(names = [PulsarParams.ARG_ROUND], description = "The rounds")
    var round = 1

    @Parameter(names = [PulsarParams.ARG_REDUCER_TASKS], description = "Number of reducers")
    var numReduceTasks = conf.getInt(CapabilityTypes.MAPREDUCE_JOB_REDUCES, 1)

    @Parameter(names = [PulsarParams.ARG_THREADS], description = "Number of fetch threads in each reducer")
    var numFetchThreads = conf.getInt(CapabilityTypes.FETCH_THREADS_FETCH, 10)

    @Parameter(names = [PulsarParams.ARG_POOL_THREADS], description = "Number of fetcher threads per queue")
    var numPoolThreads = conf.getInt(CapabilityTypes.FETCH_THREADS_PER_POOL, 10)

    @Parameter(names = [PulsarParams.ARG_RESUME], description = "Resume interrupted job")
    var resume = false
    @Parameter(names = [PulsarParams.ARG_LIMIT], description = "Task limit")
    var limit = Int.MAX_VALUE

    @Parameter(names = [PulsarParams.ARG_INDEX], description = "Active indexer")
    var index = false
    @Parameter(names = [PulsarParams.ARG_INDEXER], description = "Indexer full url or collection only, "
            + " for example, http://localhost:8983/solr/collection or collection")
    var indexerUrl: String? = null
    @Parameter(names = [PulsarParams.ARG_COLLECTION], description = "Index server side collection name")
    var indexerCollection: String? = null
    @Parameter(names = [PulsarParams.ARG_ZK], description = "Zookeeper host string")
    var zkHostString: String? = null

    @Parameter(names = [PulsarParams.ARG_VERBOSE], description = "More logs")
    var verbose = 0
    @Parameter(names = ["-help", "-h"], help = true, description = "Print this help text")
    var isHelp = false

    fun parse(args: Array<String>) {
        val jc = JCommander(this)

        try {
            jc.parse(*args)
        } catch (e: ParameterException) {
            println(e.toString())
            println("Try '-h' or '-help' for more information.")
            exitProcess(0)
        }

        if (isHelp) {
            jc.usage()
            exitProcess(0)
        }

        indexerUrl = StringUtils.stripEnd(indexerUrl, "/")
        val indexerHost = URLUtil.getHostName(indexerUrl)
        if (indexerHost == null) {
            indexerCollection = indexerUrl
            indexerUrl = null
        }
    }

    fun toParams(): Params {
        return Params.of(
                PulsarParams.ARG_ROUND, round,
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_BATCH_ID, batchId,
                PulsarParams.ARG_FETCH_MODE, fetchMode,
                PulsarParams.ARG_STRICT_DF, strictDf,
                PulsarParams.ARG_REDUCER_TASKS, numReduceTasks,
                PulsarParams.ARG_THREADS, numFetchThreads,
                PulsarParams.ARG_POOL_THREADS, numPoolThreads,
                PulsarParams.ARG_RESUME, resume,
                PulsarParams.ARG_LIMIT, limit,
                PulsarParams.ARG_INDEX, index,
                PulsarParams.ARG_INDEXER_URL, indexerUrl,
                PulsarParams.ARG_ZK, zkHostString,
                PulsarParams.ARG_COLLECTION, indexerCollection
        )
    }

    companion object {
        val defaultBatchId = AppFiles.readBatchIdOrDefault(AppConstants.ALL_BATCHES)
    }
}
