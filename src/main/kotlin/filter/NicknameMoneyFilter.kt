package filter

import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.CommonMessageFilter
import dev.inmo.tgbotapi.extensions.utils.mentionTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.regularTextSourceOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

object NicknameMoneyFilter : CommonMessageFilter<TextContent> {
    override suspend fun invoke(message: CommonMessage<TextContent>): Boolean {
        return message.content.textSources
            .takeIf { it.size == 4 }
            ?.let {
                it[2].mentionTextSourceOrNull() ?: return@let false
                it[3].regularTextSourceOrNull()?.source
                    ?.toDoubleOrNull()?.takeIf { m -> m > 0 } ?: return@let false
                return@let true
            } ?: false
    }
}