import config.DatabaseConfiguration
import dev.inmo.micro_utils.coroutines.defaultSafelyExceptionHandler
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
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
import service.EventService
import service.TelegramService

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

                handleGiveMoney()

                handleBroadcast()

                handleChatEvent()

                handleDbCommand()
            }.join()
        }.onFailure { it.printStackTrace() }
        delay(5000)
    }
}