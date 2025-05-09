import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatEvent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDice
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.extensions.utils.ifNewChatMembers
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.chat.member.ChatMember
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
import filter.NicknameMoneyFilter
import filter.OnOffFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import persistence.table.START_MONEY_VALUE
import java.io.File
import kotlin.math.abs

suspend fun BehaviourContext.handleStartOrJoinCommand() =
    onCommand("(start)|(join)".toRegex()) {
        runCatching {
            loggedTelegramTransaction {
                withChatIdAndUserId(it) { chatId, userId ->
                    val isCreator =
                        getChatMember(chatId.toChatId(), userId.toChatId()).status == ChatMember.Status.Creator
                    val replyText =
                        if (telegramService.createUserIfNotExist(chatId, userId, isCreator)) {
                            "Привет, ${it.fullNameOfUserOrBlank().ifBlank { "неизвестный" }}!\n\n" +
                                "Ты подключился к боту! Для начала тебе выдано ${START_MONEY_VALUE.toTelegramMoney()}. " +
                                "Трать их с умом!"
                        } else {
                            "Ты уже подключен к боту!"
                        }
                    reply(it, replyText)
                }
            }
        }.onFailure { t ->
            t.printStackTrace()
            reply(it, t.buildReplyMessage())
        }
    }

suspend fun BehaviourContext.handlePidorCommand() = onCommand("pidor") {
    runCatching {
        loggedTelegramTransaction {
            ifKnownUser(it) { chatId, _ ->
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
            }
        }
    }.onFailure { t ->
        t.printStackTrace()
        reply(it, t.buildReplyMessage())
    }
}

suspend fun BehaviourContext.handleHandsomeCommand() =
    onCommand("handsome") {
        runCatching {
            loggedTelegramTransaction {
                ifKnownUser(it) { chatId, _ ->
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
                }
            }
        }.onFailure { t ->
            t.printStackTrace()
            reply(it, t.buildReplyMessage())
        }
    }

suspend fun BehaviourContext.handleDice() = onDice {
    runCatching {
        loggedTelegramTransaction {
            ifKnownUser(it) { chatId, userId ->
                if (
                    featureToggleDaoService.readValueForEnum(chatId, FeatureToggle.MONEY_FOR_SLOT_MACHINE)
                        .toBoolean() &&
                    DiceType.getType(it.content.dice) == DiceType.SLOT_MACHINE &&
                    it.forwardOrigin == null
                ) {
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
                }
            }
        }
    }.onFailure { t ->
        t.printStackTrace()
        reply(it, t.buildReplyMessage())
    }
}

suspend fun BehaviourContext.handleEnableMoneyForSlots() =
    onCommandWithArgs("slots", OnOffFilter) { message: CommonMessage<TextContent>, params: Array<String> ->
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

suspend fun BehaviourContext.handleDbCommand() = onCommand("db") {
    loggedTelegramTransaction {
        ifAdmin(it) { _, userId ->
            if (userId == koin.getProperty<String>("CREATOR_ID")?.toLong()) {
                runCatching {
                    val dbSuffix = ".mv.db"
                    val dbName = koin.getProperty<String>("database.url")?.removePrefix("jdbc:h2:file:")
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
}

suspend fun BehaviourContext.handleUserMoney() = onCommand("money") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
            val user = telegramService.findUserByGroupIdAndUserId(chatId, userId)!!
            reply(it, "У тебя на балансе ${user.money.toTelegramMoney()}")
        }
    }
}

suspend fun BehaviourContext.handleMoneyRate() = onCommand("money_rating") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
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
                .also {
                    if (chatId != userId) eventService.createEventIfNeed(chatId)
                }
        }
    }
}

suspend fun BehaviourContext.handleFishing() = onCommand("fishing") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, userId ->
            val (user, catch) = telegramService.processFishing(chatId, userId)

            if (catch != null) {
                val replyText = when (catch.name) {
                    CatchType.LARCENY -> telegramService.findAllUsersByGroupId(chatId)
                        .minus(user)
                        .random()
                        .also { telegramService.addMoney(it, abs(catch.regard)) }
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

suspend fun BehaviourContext.handleGiveAdmin() =
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

suspend fun BehaviourContext.handleRemoveAdmin() =
    onCommandWithArgs("remove_admin", NicknameFilter) { message: CommonMessage<TextContent>, params: Array<String> ->
        loggedTelegramTransaction {
            ifAdmin(message) { chatId, userId ->
                val nickname = params.first()
                val creatorId = koin.getProperty<String>("CREATOR_ID")
                telegramService.findAllUsersByGroupId(chatId)
                    .map { it to getChatMember(chatId.toChatId(), it.telegramId.toChatId()) }
                    .filter { it.first.telegramId != creatorId?.toLong() }
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

suspend fun BehaviourContext.handleChatEvent() =
    onChatEvent {
        val chatId = it.chat.id
        it.chatEvent.ifNewChatMembers {
            it.members.find {
                it.username == getMe().username
            }?.let { sendMessage(chatId, GREETINGS_MESSAGE) }
        }
    }

suspend fun BehaviourContext.handleDuel() = onCommandWithArgs(
    "duel",
    initialFilter = NicknameFilter
) { message: CommonMessage<TextContent>, params: Array<String> ->
    loggedTelegramTransaction {
        ifKnownUser(message) { chatId, userId ->

        }
    }
}

suspend fun BehaviourContext.handleFagStats() = onCommand("pidor_stats") {
    loggedTelegramTransaction {
        ifKnownUser(it) { chatId, _ ->
            var maxNameLen: Int

            telegramService.getFagStats(chatId)
                .map {
                    getChatMember(chatId.toChatId(), it.first.telegramId.toChatId()) to it.second
                }.also {
                    maxNameLen = it.maxOf { it.first.fullNameOrBlank().ifBlank { "Без имени" }.length } + 5
                }.sortedByDescending {
                    it.second
                }.mapIndexed { index, pair ->
                    "${index + 1}. " +
                        pair.first.fullNameOrBlank().ifBlank { "Без имени" }.let {
                            it.plus(" ".repeat(maxNameLen - it.length))
                        } + String.format("%15s", "${pair.second}x пидор")
                }.joinToString("\n")
                .let { sendMessage(chatId.toChatId(), pre(it).markdown, parseMode = Markdown) }
        }
    }
}

suspend fun BehaviourContext.handleHandsomeStats() =
    onCommand("handsome_stats") {
        loggedTelegramTransaction {
            ifKnownUser(it) { chatId, _ ->
                var maxNameLen: Int

                telegramService.getHandsomeStats(chatId)
                    .map {
                        getChatMember(chatId.toChatId(), it.first.telegramId.toChatId()) to it.second
                    }.also {
                        maxNameLen = it.maxOf { it.first.fullNameOrBlank().ifBlank { "Без имени" }.length } + 5
                    }.sortedByDescending {
                        it.second
                    }.mapIndexed { index, pair ->
                        "${index + 1}. " +
                            pair.first.fullNameOrBlank().ifBlank { "Без имени" }.let {
                                it.plus(" ".repeat(maxNameLen - it.length))
                            } + String.format("%15s", "${pair.second}x красавчик")
                    }.joinToString("\n")
                    .let { sendMessage(chatId.toChatId(), pre(it).markdown, parseMode = Markdown) }
            }
        }
    }

suspend fun BehaviourContext.handleGiveMoney() = onCommandWithArgs(
    "give_money",
    initialFilter = NicknameMoneyFilter
) { message: CommonMessage<TextContent>, params: Array<String> ->
    runCatching {
        loggedTelegramTransaction {
            ifKnownUser(message) { chatId, userId ->
                val paramsList = params.filterNot { it.isEmpty() }
                val nickname = paramsList[0]
                val present = paramsList[1].toDouble()
                val user = telegramService.findUserByGroupIdAndUserId(chatId, userId)?.takeIf { it.money >= present }
                    ?: reply(message, "У тебя недостаточно денег для такого подарка!").let { return@ifKnownUser }
                telegramService.findAllUsersByGroupId(chatId)
                    .minus(user)
                    .associateBy {
                        getChatMember(
                            it.groupId.toChatId(),
                            it.telegramId.toChatId()
                        ).usernameOrBlank()
                    }[nickname]
                    ?.let { telegramService.addMoney(it, present) }
                    ?.let { telegramService.addMoney(user, -present) }
                    ?.also { reply(message, "Вы успешно передали $nickname ${present.toTelegramMoney()}!") }
            }
        }
    }.onFailure { t ->
        t.printStackTrace()
        reply(message, t.buildReplyMessage())
    }
}

suspend fun BehaviourContext.handleBroadcast() = onCommand("broadcast") {
    runCatching {
        loggedTelegramTransaction {
            ifCreator(it) { chatId, userId ->
                val message = waitTextMessage(
                    SendTextMessage(
                        chatId.toChatId(),
                        "Введите сообщение для отправки в чаты"
                    )
                ).first {
                    val (messageChatId, messageUserId) = it.getChatIdAndUserId()
                    messageChatId == chatId && messageUserId == userId
                }.content.text

                telegramService.findAllChatGroups()
                    .mapNotNull {
                        runCatching {
                            getChat(it.toChatId())
                        }.getOrNull()
                    }.forEach {
                        runCatching {
                            sendMessage(it.id, message)
                        }
                    }.also {
                        sendMessage(chatId.toChatId(), "Все сообщения успешно отправлены")
                    }
            }
        }
    }.onFailure { it.printStackTrace() }
}

private const val GREETINGS_MESSAGE = "Всем добрый денек (кроме Клюшкина)!!!\n\n" +
    "Для разблокировки всех функций бота ему надо выдать права админа.\n" +
    "Каждый участник бота, кто хочет получить доступ к функциям, должен присоединиться к системе.\n" +
    "Для этого надо ввести команду /join"