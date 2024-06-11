package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.FishingStatsTable

class FishingStatsEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<FishingStatsEntity>(FishingStatsTable)

    var user by GroupUserEntity referencedOn FishingStatsTable.user
    var catchAmount by FishingStatsTable.catchAmount
    var lastCatchTime by FishingStatsTable.lastCatchTime
}