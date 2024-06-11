import config.DatabaseConfig
import dev.inmo.tgbotapi.extensions.api.bot.getMe
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
import dev.inmo.tgbotapi.extensions.utils.usernameOrNull
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.message.Markdown
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.pre
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.userLink
import enumeration.CatchType
import enumeration.DiceType
import enumeration.FeatureToggle
import exception.TelegramBusinessException
import exception.TelegramError
import filter.NicknameFilter
import filter.OnOffFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.environmentProperties
import org.koin.fileProperties
import org.koin.ksp.generated.*
import persistence.dao.FeatureToggleDaoService
import persistence.table.START_MONEY_VALUE
import service.FishingService
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

fun main(): Unit = runBlocking {
    val telegramService = koin.get<TelegramService>()
    val featureToggleDaoService = koin.get<FeatureToggleDaoService>()
    koin.get<DatabaseConfig>().start()

    val bot = telegramBot(ENV_TOKEN)
    bot.buildBehaviourWithLongPolling {
        handleStartOrJoinCommand(telegramService)

        handlePidorCommand(telegramService)

        handleHandsomeCommand(telegramService)

        handleDice(telegramService, featureToggleDaoService)

        handleEnableMoneyForSlots(featureToggleDaoService)

        handleUserMoney(telegramService)

        handleMoneyRate(telegramService)

        handleFishing(telegramService)

        handleGiveAdmin(telegramService)

        handleRemoveAdmin(telegramService)

        handleChatEvent()

        handleDbCommand()
    }.join()
}

private suspend fun BehaviourContext.handleStartOrJoinCommand(telegramService: TelegramService) =
    onCommand("(start)|(join)".toRegex()) {
        loggedTelegramTransaction {
            withChatIdAndUserId(it) { chatId, userId ->
                runCatching {
                    val replyText =
                        if (telegramService.createUserIfNotExist(chatId, userId)) {
                            "Привет, ${it.fullNameOfUserOrBlank().ifBlank { "неизвестный" }}!\n\n" +
                                "Ты подключился к боту! Для начала тебе выдано ${START_MONEY_VALUE.toTelegramMoney()}. " +
                                "Трать их с умом!"
                        } else {
                            "Ты уже подключен к боту!"
                        }
                    reply(it, replyText)
                }.onFailure { t ->
                    t.printStackTrace()
                    reply(it, t.buildReplyMessage())
                }
            }
        }
    }

private suspend fun BehaviourContext.handlePidorCommand(telegramService: TelegramService) = onCommand("pidor") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, _ ->
            runCatching {
                val (user, isNeedToChangeBalance) = telegramService.findFagUser(chatId)
                val chatMember = getChatMember(chatId = chatId.toChatId(), userId = user.telegramId.toChatId())
                val nameAndUsername = listOf(
                    chatMember.fullNameOrBlank().ifBlank { "пользователь без имени" },
                    chatMember.user.username?.username ?: chatMember.user.userLink
                ).joinToString(" ")

                if (isNeedToChangeBalance) {
                    val (updatedUser, subtractedMoney) = telegramService.updateBalanceForFag(user)
                    val replyText = "Сегодняшний пидор дня - $nameAndUsername!!!\n\n" +
                        "В качестве наказания у тебя было отобрано ${abs(subtractedMoney).toTelegramMoney()}," +
                        " и сейчас у тебя ${updatedUser.money.toTelegramMoney()}"
                    sendMessage(chatId.toChatId(), replyText)
                } else {
                    sendMessage(chatId.toChatId(), "Пидор дня уже определен: это $nameAndUsername!!!")
                }
            }.onFailure { t ->
                t.printStackTrace()
                reply(it, t.buildReplyMessage())
            }
        }
    }
}

private suspend fun BehaviourContext.handleHandsomeCommand(telegramService: TelegramService) =
    onCommand("handsome") {
        loggedTelegramTransaction {
            ifKnownUser(it) { chatId, _ ->
                runCatching {
                    val (user, isNeedToChangeBalance) = telegramService.findHandsomeUser(chatId)
                    val chatMember = getChatMember(chatId = chatId.toChatId(), userId = user.telegramId.toChatId())
                    val nameAndUsername = listOf(
                        chatMember.fullNameOrBlank().ifBlank { "пользователь без имени" },
                        chatMember.user.username?.username ?: chatMember.user.userLink
                    ).joinToString(" ")

                    if (isNeedToChangeBalance) {
                        val (updatedUser, subtractedMoney) = telegramService.updateBalanceForHandsome(user)
                        val replyText = "Сегодняшний красавчик дня - $nameAndUsername!!!\n\n" +
                            "В качестве награды тебе было выдано ${abs(subtractedMoney).toTelegramMoney()}," +
                            " и сейчас у тебя ${updatedUser.money.toTelegramMoney()}"
                        sendMessage(chatId.toChatId(), replyText)
                    } else {
                        sendMessage(chatId.toChatId(), "Красавчик дня уже определен: это $nameAndUsername!!!")
                    }
                }.onFailure { t ->
                    t.printStackTrace()
                    reply(it, t.buildReplyMessage())
                }
            }
        }
    }

private suspend fun BehaviourContext.handleDice(
    telegramService: TelegramService,
    featureToggleDaoService: FeatureToggleDaoService
) = onDice {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
            if (
                featureToggleDaoService.readValueForEnum(chatId, FeatureToggle.MONEY_FOR_SLOT_MACHINE).toBoolean() &&
                DiceType.getType(it.content.dice) == DiceType.SLOT_MACHINE &&
                it.forwardOrigin == null
            ) {
                runCatching {
                    val (user, fee) = telegramService.takeMoneyForSlot(chatId, userId)

                    if (fee != null) {
                        telegramService.processSlotMachineResult(user, it.content.dice)
                            ?.let { pair ->
                                val (updatedUser, prize) = pair
                                val replyText =
                                    "На слот-машину было потрачено ${fee.toTelegramMoney()}.\n" +
                                        "Ты выиграл $prize\uD83C\uDF89\uD83C\uDF89\uD83C\uDF89\n" +
                                        "Теперь у тебя ${updatedUser.money.toTelegramMoney()}"
                                reply(it, replyText)
                                setMessageReaction(it, "\uD83D\uDD25")
                            } ?: (
                            "На слот-машину было потрачено ${fee.toTelegramMoney()}.\n" +
                                "К сожалению, ты ничего не выиграл(\n" +
                                "У тебя ${user.money.toTelegramMoney()}"
                            ).let { replyText -> reply(it, replyText) }
                    } else {
                        sendMessage(
                            chatId.toChatId(),
                            "${it.fullNameOfUserOrBlank()}, У тебя недостаточно средств на слот машину("
                        )
                        delete(it)
                    }
                }.onFailure { t ->
                    t.printStackTrace()
                    reply(it, t.buildReplyMessage())
                }
            }
        }
    }
}

private suspend fun BehaviourContext.handleEnableMoneyForSlots(
    featureToggleDaoService: FeatureToggleDaoService
) = onCommandWithArgs("slots", OnOffFilter) { message: CommonMessage<TextContent>, params: Array<String> ->
    loggedTelegramTransaction {
        ifAdmin(message) { chatId, _ ->
            val on = params.first().equals("on", ignoreCase = true)
            featureToggleDaoService.updateOrCreateValueForFeatureToggle(
                FeatureToggle.MONEY_FOR_SLOT_MACHINE,
                chatId,
                on.toString()
            )

            reply(message, "Получение денег за слот машину ${if (on) "включено" else "выключено"}!")
        }
    }
}

private suspend fun BehaviourContext.handleDbCommand() = onCommand("db") {
    loggedTelegramTransaction {
        ifAdmin(it) { _, _ ->
            runCatching {
                val dbSuffix = ".mv.db"
                val dbName = koin.getProperty<String>("database.url")
                    ?.let { it.removePrefix("jdbc:h2:file:") }
                    ?: throw TelegramBusinessException(TelegramError.DB_NOT_FOUND)
                val file = File(dbName + dbSuffix).takeIf { it.exists() }
                    ?: throw TelegramBusinessException(TelegramError.DB_NOT_FOUND)
                sendDocument(
                    it.chat,
                    file.readBytes().asMultipartFile(dbName + dbSuffix)
                )
            }.onFailure { t ->
                reply(it, t.buildReplyMessage())
            }
        }
    }
}

private suspend fun BehaviourContext.handleUserMoney(telegramService: TelegramService) = onCommand("money") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
            val user = telegramService.findUserByGroupIdAndUserId(chatId, userId)!!
            reply(it, "У тебя на балансе ${user.money.toTelegramMoney()}")
        }
    }
}

private suspend fun BehaviourContext.handleMoneyRate(telegramService: TelegramService) = onCommand("money_rating") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, _ ->
            var maxNameLen: Int

            telegramService.findAllUsersByGroupId(chatId)
                .sortedByDescending { it.money }
                .map {
                    getChatMember(chatId.toChatId(), it.telegramId.toChatId()) to it.money
                }.also {
                    maxNameLen = it.maxOf { it.first.fullNameOrBlank().ifBlank { "Без имени" }.length } + 5
                }
                .mapIndexed { index, pair ->
                    "${index + 1}. " +
                        pair.first.fullNameOrBlank().ifBlank { "Без имени" }.let {
                            it.plus(" ".repeat(maxNameLen - it.length))
                        } + String.format("%15s", pair.second.toTelegramMoney())
                }.joinToString("\n")
                .let { sendMessage(chatId.toChatId(), pre(it).markdown, parseMode = Markdown) }
        }
    }
}

private suspend fun BehaviourContext.handleFishing(telegramService: TelegramService) = onCommand("fishing") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
            val (user, catch) = telegramService.processFishing(chatId, userId)

            if (catch != null) {
                val replyText = when (catch.name) {
                    CatchType.LARCENY -> telegramService.findAllUsersByGroupId(chatId)
                        .minus(user)
                        .random()
                        .also { telegramService.addMoney(it, it.money + abs(catch.regard)) }
                        .let { getChatMember(it.groupId.toChatId(), it.telegramId.toChatId()) }
                        .user.username!!.username
                        .let { catch.message.format(it, abs(catch.regard).toTelegramMoney()) }
                    else -> catch.message.format(abs(catch.regard).toTelegramMoney())
                }
                sendMessage(chatId.toChatId(), replyText)
                sendMessage(chatId.toChatId(), "Сейчас у тебя ${user.money.toTelegramMoney()}")
            } else {
                reply(it, "Вы закидываете удочку, но, похоже, удача отвернулась от вас. Приходите на следующий день")
            }
        }
    }
}

private suspend fun BehaviourContext.handleGiveAdmin(telegramService: TelegramService) =
    onCommandWithArgs("give_admin", NicknameFilter) { message: CommonMessage<TextContent>, params: Array<String> ->
        loggedTelegramTransaction {
            ifAdmin(message) { chatId, userId ->
                val nickname = params.first()
                telegramService.findAllUsersByGroupId(chatId)
                    .map { it to getChatMember(chatId.toChatId(), it.telegramId.toChatId()) }
                    .find { it.second.user.username?.username == nickname }
                    ?.let {
                        if (it.first.isAdmin) {
                            reply(message, "${it.second.fullNameOrBlank()} уже админ!")
                        } else {
                            telegramService.setOrRemoveAdmin(it.first, true)
                            reply(message, "${it.second.user.username?.username} теперь является админом!")
                        }
                    }
            }
        }
    }

private suspend fun BehaviourContext.handleRemoveAdmin(telegramService: TelegramService) =
    onCommandWithArgs("remove_admin", NicknameFilter) { message: CommonMessage<TextContent>, params: Array<String> ->
        loggedTelegramTransaction {
            ifAdmin(message) { chatId, userId ->
                val nickname = params.first()
                telegramService.findAllUsersByGroupId(chatId)
                    .map { it to getChatMember(chatId.toChatId(), it.telegramId.toChatId()) }
                    .find { it.second.user.username?.username == nickname }
                    ?.let {
                        if (!it.first.isAdmin) {
                            reply(message, "${it.second.fullNameOrBlank()} не являлся админом!")
                        } else {
                            telegramService.setOrRemoveAdmin(it.first, false)
                            reply(message, "${it.second.user.username?.username} лишился админского статуса!")
                        }
                    }
            }
        }
    }

private suspend fun BehaviourContext.handleChatEvent() =
    onChatEvent {
        val chatId = it.chat.id
        it.chatEvent.ifNewChatMembers {
            it.members.find {
                it.username == getMe().username
            }?.let { sendMessage(chatId, GREETINGS_MESSAGE) }
        }
    }

private const val GREETINGS_MESSAGE = "Всем добрый денек (кроме Клюшкина)!!!\n\n" +
    "Для разблокировки всех функций бота ему надо выдать права админа.\n" +
    "Каждый участник бота, кто хочет получить доступ к функциям, должен присоединиться к системе.\n" +
    "Для этого надо ввести команду /join"