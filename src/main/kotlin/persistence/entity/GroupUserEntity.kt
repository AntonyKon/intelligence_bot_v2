package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.GroupUsersTable

class GroupUserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GroupUserEntity>(GroupUsersTable)
    var telegramId by GroupUsersTable.telegramId
    var groupId by GroupUsersTable.groupId
    var money by GroupUsersTable.money
    var isAdmin by GroupUsersTable.isAdmin
}