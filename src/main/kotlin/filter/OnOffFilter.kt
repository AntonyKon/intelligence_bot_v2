package filter

import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.CommonMessageFilter
import dev.inmo.tgbotapi.extensions.utils.ifRegularTextSource
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

class OnOffFilter : CommonMessageFilter<TextContent> {
    override suspend fun invoke(message: CommonMessage<TextContent>): Boolean {
        return message.content.textSources
            .any {
                it.ifRegularTextSource {
                    it.asText.trim().equals("on", ignoreCase = true) ||
                        it.asText.trim().equals("off", ignoreCase = true)
                } ?: false
            }
    }
}