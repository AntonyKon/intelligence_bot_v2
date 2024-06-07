package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.HandsomeFagFlagsTable

class HandsomeFagFlagEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<HandsomeFagFlagEntity>(HandsomeFagFlagsTable)

    var groupId by HandsomeFagFlagsTable.id
    var fagFlagUser by GroupUserEntity optionalReferencedOn HandsomeFagFlagsTable.fagFlagUser
    var handsomeFlagUser by GroupUserEntity optionalReferencedOn HandsomeFagFlagsTable.handsomeFlagUser
    var fagFlagDate by HandsomeFagFlagsTable.fagFlagDate
    var handsomeFlagDate by HandsomeFagFlagsTable.handsomeFlagDate
}