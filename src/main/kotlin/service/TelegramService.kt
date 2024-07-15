package service

import dev.inmo.tgbotapi.types.dice.Dice
import exception.TelegramBusinessException
import exception.TelegramError
import loggedTransaction
import org.koin.core.annotation.Single
import persistence.dao.GroupUserDaoService
import persistence.dao.HandsomeFagFlagDaoService
import persistence.dao.HandsomeFagStatsDaoService
import persistence.entity.GroupUserEntity
import persistence.table.START_MONEY_VALUE
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sign

@Single
class TelegramService(
    private val groupUserDaoService: GroupUserDaoService,
    private val handsomeFagFlagDaoService: HandsomeFagFlagDaoService,
    private val handsomeFagStatsDaoService: HandsomeFagStatsDaoService,
    private val fishingService: FishingService
) {
    fun createUserIfNotExist(groupId: Long, userId: Long, isCreator: Boolean) =
        if (!groupUserDaoService.userExists(userId, groupId)) {
            groupUserDaoService.createNewUser(userId, groupId, isCreator)
            true
        } else false

    fun findFagUser(groupId: Long) = loggedTransaction {
        val handsomeFagFlag = handsomeFagFlagDaoService.findByGroupId(groupId)
            ?: handsomeFagFlagDaoService.saveByGroupId(groupId)

        val (user, isNeedToChangeBalance) = if (
            handsomeFagFlag.fagFlagDate?.isBefore(LocalDate.now()) != false ||
            handsomeFagFlag.fagFlagUser == null
        ) {
            findAllUsersByGroupId(groupId)
                .randomOrNull()
                ?.also { handsomeFagFlagDaoService.markUserAsFag(groupId, it) }
                ?.also { handsomeFagStatsDaoService.createOrUpdateStatsForUser(it, true, false) }
                ?.let { it to true }
                ?: throw TelegramBusinessException(TelegramError.USER_LIST_IS_EMPTY)
        } else {
            handsomeFagFlag.fagFlagUser!! to false
        }
        user to isNeedToChangeBalance
    }

    fun findHandsomeUser(groupId: Long): Pair<GroupUserEntity, Boolean> {
        val handsomeFagFlag = handsomeFagFlagDaoService.findByGroupId(groupId)
            ?: handsomeFagFlagDaoService.saveByGroupId(groupId)

        val (user, isNeedToChangeBalance) = if (
            handsomeFagFlag.handsomeFlagDate?.isBefore(LocalDate.now()) != false ||
            handsomeFagFlag.handsomeFlagUser == null
        ) {
            findAllUsersByGroupId(groupId)
                .randomOrNull()
                ?.also { handsomeFagFlagDaoService.markUserAsHandsome(groupId, it) }
                ?.also { handsomeFagStatsDaoService.createOrUpdateStatsForUser(it, false, true) }
                ?.let { it to true }
                ?: throw TelegramBusinessException(TelegramError.USER_LIST_IS_EMPTY)
        } else {
            handsomeFagFlag.handsomeFlagUser!! to false
        }

        return user to isNeedToChangeBalance
    }

    fun updateBalanceForFag(user: GroupUserEntity) = loggedTransaction {
        calculateMoneyToAdd(user, -HANDSOME_FAG_MONEY)
            .let { groupUserDaoService.changeBalance(user, user.money + it) to it }
    }

    fun updateBalanceForHandsome(user: GroupUserEntity) =
        calculateMoneyToAdd(user, HANDSOME_FAG_MONEY)
            .let { groupUserDaoService.changeBalance(user, user.money + it) to it }

    fun takeMoneyForSlot(chatId: Long, userId: Long) =
        groupUserDaoService.findByUserIdAndChatId(userId, chatId).firstOrNull()
            ?.let {
                if (it.money >= FEE_FOR_SLOTS) {
                    groupUserDaoService.changeBalance(
                        it,
                        it.money - FEE_FOR_SLOTS
                    ) to FEE_FOR_SLOTS
                } else {
                    it to null
                }
            }
            ?: throw TelegramBusinessException(TelegramError.USER_IS_NOT_FOUND)

    fun processSlotMachineResult(user: GroupUserEntity, dice: Dice): Pair<GroupUserEntity, Double>? {
        val value = dice.value
        val coefficients = listOf(1, 2, 3, 4)
        val nums = listOf(
            coefficients[(value - 1) and 3],
            coefficients[((value - 1) shr 2) and 3],
            coefficients[((value - 1) shr 4) and 3]
        )
        val isWin = nums.all { nums[0] == it }

        return if (isWin) {
            val percentage = (value.toDouble() / 100.0)
                .let { exp(it) / 10 * 2 }

            val prize = (START_MONEY_VALUE * percentage).roundToLong().toDouble()
            val updatedUser = groupUserDaoService.changeBalance(user, user.money + prize)
            updatedUser to prize
        } else null
    }

    fun userExists(groupId: Long, userId: Long) = groupUserDaoService.userExists(userId, groupId)

    fun isUserAdmin(groupId: Long, userId: Long) = findUserByGroupIdAndUserId(groupId, userId)?.isAdmin ?: false

    fun findUserByGroupIdAndUserId(groupId: Long, userId: Long) =
        groupUserDaoService.findByUserIdAndChatId(userId, groupId)
            .firstOrNull()

    fun findAllUsersByGroupId(groupId: Long) = groupUserDaoService.findAllByGroup(groupId).toList()

    fun addMoney(user: GroupUserEntity, moneyToAdd: Double) = calculateMoneyToAdd(user, moneyToAdd)
        .let { groupUserDaoService.changeBalance(user, user.money + it) }

    fun processFishing(groupId: Long, userId: Long) = findUserByGroupIdAndUserId(groupId, userId)!!
        .let {
            val catch = fishingService.gainFish(it) ?: return@let it to null
            val moneyToAdd = calculateMoneyToAdd(it, catch.regard)

            groupUserDaoService.changeBalance(it, it.money + moneyToAdd) to catch.copy(regard = moneyToAdd)
        }

    fun setOrRemoveAdmin(user: GroupUserEntity, isAdmin: Boolean) =
        groupUserDaoService.setOrRemoveAdminFlag(user, isAdmin)

    fun getFagStats(groupId: Long) = groupUserDaoService.findAllByGroup(groupId)
        .map {
            it to (handsomeFagStatsDaoService.findStatsByUser(it)?.fagCount ?: 0)
        }

    fun getHandsomeStats(groupId: Long) = groupUserDaoService.findAllByGroup(groupId)
        .map {
            it to (handsomeFagStatsDaoService.findStatsByUser(it)?.handsomeCount ?: 0)
        }

    fun findAllChatGroups() = groupUserDaoService.findAllGroups()

    private fun calculateMoneyToAdd(user: GroupUserEntity, moneyToAdd: Double) =
        if (user.money + moneyToAdd > 0) {
            moneyToAdd
        } else {
            user.money * sign(moneyToAdd)
        }

    companion object {
        private const val HANDSOME_FAG_MONEY = START_MONEY_VALUE * 0.2
        private const val FEE_FOR_SLOTS = START_MONEY_VALUE * 0.025
    }
}