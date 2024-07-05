package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.DailyEventsTable

class DailyEventsEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DailyEventsEntity>(DailyEventsTable)

    var groupId by DailyEventsTable.groupId
    var lastEventDate by DailyEventsTable.lastEventDate
}