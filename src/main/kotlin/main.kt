import config.DatabaseConfiguration
import dev.inmo.micro_utils.coroutines.defaultSafelyExceptionHandler
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatEvent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDice
import dev.inmo.tgbotapi.extensions.utils.ifNewChatMembers
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.member.ChatMember
import dev.inmo.tgbotapi.types.message.Markdown
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.pre
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.userLink
import dto.EventDto
import enumeration.CatchType
import enumeration.DiceType
import enumeration.FeatureToggle
import exception.TelegramBusinessException
import exception.TelegramError
import filter.NicknameFilter
import filter.OnOffFilter
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.environmentProperties
import org.koin.fileProperties
import org.koin.ksp.generated.*
import persistence.dao.FeatureToggleDaoService
import persistence.table.START_MONEY_VALUE
import service.EventService
import service.TelegramService
import java.io.File
import kotlin.math.abs

val ENV_TOKEN: String = System.getenv("BOT_TOKEN")

val koin = startKoin {
    printLogger(Level.DEBUG)
    defaultModule()
    fileProperties()
    environmentProperties()
}.koin
val telegramService = koin.get<TelegramService>()
val featureToggleDaoService = koin.get<FeatureToggleDaoService>()
val eventService = koin.get<EventService>()

fun main(): Unit = runBlocking {
    koin.get<DatabaseConfiguration>().start()
    val port = koin.getProperty<String>("SERVER_PORT")!!

    embeddedServer(Netty, port = port.toInt()) {
        routing {
            get("/") {
                call.respondText("OK")
            }
        }
    }.start(wait = false)

    val bot = telegramBot(ENV_TOKEN)


    launch {
        println("start processing")
        processEvents(dailyEvents(), bot)
        println("close coroutine")
    }

    while (true) {
        runCatching {
            bot.buildBehaviourWithLongPolling(
                defaultExceptionsHandler = defaultSafelyExceptionHandler
            ) {
                handleStartOrJoinCommand()

                handlePidorCommand()

                handleHandsomeCommand()

                handleDice()

                handleEnableMoneyForSlots()

                handleUserMoney()

                handleMoneyRate()

                handleFishing()

                handleGiveAdmin()

                handleRemoveAdmin()

                handleFagStats()

                handleHandsomeStats()

                handleChatEvent()

                handleDbCommand()
            }.join()
        }.onFailure { it.printStackTrace() }
        delay(5000)
    }
}