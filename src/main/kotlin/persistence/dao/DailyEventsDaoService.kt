package persistence.dao

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.koin.core.annotation.Single
import persistence.entity.DailyEventsEntity
import persistence.table.DailyEventsTable
import java.time.LocalDate

@Single
class DailyEventsDaoService {

    fun eventExists(groupId: Long) = DailyEventsEntity.find {
        DailyEventsTable.groupId eq groupId
    }.empty().not()

    fun createEvent(groupId: Long) = DailyEventsEntity.new {
        this.groupId = groupId
    }

    fun updateEvent(groupId: Long) = DailyEventsEntity.findSingleByAndUpdate(
        DailyEventsTable.groupId eq groupId
    ) {
        it.lastEventDate = LocalDate.now()
    }

    fun findUnprocessedEvents() = DailyEventsEntity.find {
        (DailyEventsTable.lastEventDate eq null) or (DailyEventsTable.lastEventDate neq LocalDate.now())
    }
}