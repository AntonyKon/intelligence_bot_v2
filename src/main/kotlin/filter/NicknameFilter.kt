package filter

import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.CommonMessageFilter
import dev.inmo.tgbotapi.extensions.utils.ifMentionTextSource
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

object NicknameFilter : CommonMessageFilter<TextContent> {
    override suspend fun invoke(message: CommonMessage<TextContent>): Boolean {
        return message.content.textSources
            .takeIf { it.count() == 3 }
            ?.any {
                it.ifMentionTextSource { true } ?: false
            } ?: false
    }
}