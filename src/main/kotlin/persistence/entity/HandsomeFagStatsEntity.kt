package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.HandsomeFagStatsTable

class HandsomeFagStatsEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<HandsomeFagStatsEntity>(HandsomeFagStatsTable)

    var user by GroupUserEntity referencedOn HandsomeFagStatsTable.user
    var fagCount by HandsomeFagStatsTable.fagCount
    var handsomeCount by HandsomeFagStatsTable.handsomeCount
}