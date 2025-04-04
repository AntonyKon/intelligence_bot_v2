package persistence.dao

import org.jetbrains.exposed.sql.and
import org.koin.core.annotation.Property
import org.koin.core.annotation.Single
import persistence.entity.GroupUserEntity
import persistence.table.GroupUsersTable

@Single
class GroupUserDaoService(
    @Property("CREATOR_ID")
    private val creatorTelegramId: String
) {

    fun createNewUser(telegramId: Long, groupId: Long, isCreator: Boolean) = GroupUserEntity.new {
        this.telegramId = telegramId
        this.groupId = groupId
        this.isAdmin = telegramId == creatorTelegramId.toLong() || isCreator
    }

    fun userExists(telegramId: Long, groupId: Long) = GroupUserEntity.find {
        (GroupUsersTable.groupId eq groupId) and (GroupUsersTable.telegramId eq telegramId)
    }.empty().not()

    fun findAllByGroup(groupId: Long) = GroupUserEntity.find {
        GroupUsersTable.groupId eq groupId
    }

    fun changeBalance(user: GroupUserEntity, newBalance: Double) = user.apply {
        money = newBalance
    }

    fun setOrRemoveAdminFlag(user: GroupUserEntity, isAdmin: Boolean) = user.apply {
        this.isAdmin = isAdmin
    }

    fun findById(id: Long) = GroupUserEntity.findById(id)

    fun findByUserIdAndChatId(userId: Long, chatId: Long) = GroupUserEntity.find {
        (GroupUsersTable.groupId eq chatId) and (GroupUsersTable.telegramId eq userId)
    }

    fun findAllGroups() = GroupUserEntity.all()
        .distinctBy { it.groupId }
        .map { it.groupId }
}