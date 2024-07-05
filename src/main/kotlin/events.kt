import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.toChatId
import dto.EventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun CoroutineScope.dailyEvents() = produce {
    while (true) {
        if (LocalDateTime.now().hour > EVENT_HOUR) {
            newSuspendedTransaction {
                eventService.findUnprocessedEvents()
                    .asSequence()
                    .mapNotNull {
                        eventService.chooseEvent(it)
                    }
                    .forEach {
                        trySend(it)
                    }
            }
        }
        delay(HOURS_BETWEEN_EVENTS.toDuration(DurationUnit.HOURS))
    }
}

suspend fun processEvents(events: ReceiveChannel<EventDto>, bot: TelegramBot) = events.consumeEach { dto ->
    println(dto)
    newSuspendedTransaction {
        runCatching {
            telegramService.addMoney(dto.user, dto.user.money + dto.regard)
            bot.getChatMember(dto.user.groupId.toChatId(), dto.user.telegramId.toChatId())
                .let {
                    dto.event.message.format(
                        it.usernameOrBlank().ifBlank {
                            it.fullNameOrBlank().ifBlank { "no-name" }
                        },
                        abs(dto.regard)
                    )
                }
                .let { bot.sendMessage(dto.user.groupId.toChatId(), it) }
                .also { eventService.updateEvent(dto.user.groupId) }
        }.onFailure { it.printStackTrace() }
    }
}

private const val EVENT_HOUR = 8
private const val HOURS_BETWEEN_EVENTS = 3