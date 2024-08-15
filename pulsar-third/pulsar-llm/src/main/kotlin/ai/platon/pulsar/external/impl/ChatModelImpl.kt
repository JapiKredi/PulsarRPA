package ai.platon.pulsar.external.impl

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.external.ChatModel
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.external.TokenUsage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.FinishReason
import org.jsoup.nodes.Element

open class ChatModelImpl(
    private val langchainModel: ChatLanguageModel
) : ChatModel {
    private val logger = getLogger(this)
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @return The response generated by the model.
     */
    override fun call(prompt: String) = call("", prompt)
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param context The text context.
     * @return The response generated by the model.
     */
    override fun call(context: String, prompt: String): ModelResponse {
        val prompt1 = if (context.isNotBlank()) prompt + "\n\n" + context else prompt
        val message = UserMessage.userMessage(prompt1)

        val response = try {
            langchainModel.generate(message)
        } catch (e: Exception) {
            logger.warn("Model call interrupted. | {}", e.message)
            return ModelResponse("", ResponseState.OTHER)
        }
        
        val u = response.tokenUsage()
        val tokenUsage = TokenUsage(u.inputTokenCount(), u.outputTokenCount(), u.totalTokenCount())
        val r = response.finishReason()
        val state = when (r) {
            FinishReason.STOP -> ResponseState.STOP
            FinishReason.LENGTH -> ResponseState.LENGTH
            FinishReason.TOOL_EXECUTION -> ResponseState.TOOL_EXECUTION
            FinishReason.CONTENT_FILTER -> ResponseState.CONTENT_FILTER
            else -> ResponseState.OTHER
        }
        return ModelResponse(response.content().text(), state, tokenUsage)
    }
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param document An array of messages.
     * @return The response generated by the model.
     */
    override fun call(document: FeaturedDocument, prompt: String) = call(document.document, prompt)
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param ele The Element to ask.
     * @return The response generated by the model.
     */
    override fun call(ele: Element, prompt: String) = call(ele.text(), prompt)
}
