import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.ifCommonGroupContentMessage
import dev.inmo.tgbotapi.extensions.utils.ifPrivateContentMessage
import dev.inmo.tgbotapi.types.chat.member.ChatMember
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import exception.TelegramBusinessException
import exception.TelegramError
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.entity.GroupUserEntity
import service.TelegramService

fun <T : MessageContent> CommonMessage<T>.fullNameOfUserOrBlank() = ifPrivateContentMessage {
    listOfNotNull(
        it.from.firstName,
        it.from.lastName.ifBlank { null }
    ).joinToString(" ").trim()
} ?: ifCommonGroupContentMessage {
    listOfNotNull(
        it.from.firstName,
        it.from.lastName.ifBlank { null }
    ).joinToString(" ").trim()
} ?: throw TelegramBusinessException(TelegramError.UNKNOWN_MESSAGE_TYPE)

fun <T> loggedTransaction(
    db: Database? = null,
    logger: SqlLogger = StdOutSqlLogger,
    statement: Transaction.() -> T
) = transaction(db) {
    addLogger(logger)
    statement()
}

suspend fun <T> BehaviourContext.loggedTelegramTransaction(
    db: Database? = null,
    statement: suspend Transaction.() -> T
) = newSuspendedTransaction(this.coroutineContext, db, null) {
    statement()
}

fun <T : MessageContent> CommonMessage<T>.getChatIdAndUserId() = ifPrivateContentMessage {
    it.chat.id.chatId.long to it.chat.id.chatId.long
} ?: ifCommonGroupContentMessage {
    it.chat.id.chatId.long to it.from.id.chatId.long
} ?: throw TelegramBusinessException(TelegramError.UNKNOWN_MESSAGE_TYPE)

fun ChatMember.fullNameOrBlank() = listOfNotNull(
    user.firstName,
    user.lastName.ifBlank { null }
).joinToString(" ").trim()

fun ChatMember.usernameOrBlank() = user.username?.username.orEmpty()

fun Throwable.buildReplyMessage() = "Не удалось выполнить действие\n$message"

suspend fun <T : MessageContent> BehaviourContext.withChatIdAndUserId(
    message: CommonMessage<T>,
    onEvent: suspend BehaviourContext.(chatId: Long, userId: Long) -> Unit
) = message.getChatIdAndUserId()
    .let { onEvent(it.first, it.second) }

suspend fun <T : MessageContent> BehaviourContext.ifKnownUser(
    message: CommonMessage<T>,
    onEvent: suspend BehaviourContext.(chatId: Long, userId: Long) -> Unit
) = withChatIdAndUserId(message) { chatId, userId ->
    if (koin.get<TelegramService>().userExists(chatId, userId))
        onEvent(chatId, userId)
}

suspend fun <T : MessageContent> BehaviourContext.ifAdmin(
    message: CommonMessage<T>,
    onEvent: suspend BehaviourContext.(chatId: Long, userId: Long) -> Unit
) = withChatIdAndUserId(message) { chatId, userId ->
    if (koin.get<TelegramService>().isUserAdmin(chatId, userId))
        onEvent(chatId, userId)
    else
        reply(message, "У тебя нет прав на выполнение этой команды")
}

fun Number.toTelegramMoney() = "$this$CURRENCY_SYMBOL"

const val CURRENCY_SYMBOL = '₽'