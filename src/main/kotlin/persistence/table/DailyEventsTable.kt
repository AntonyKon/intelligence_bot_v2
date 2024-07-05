package persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date

object DailyEventsTable : LongIdTable("daily_events", "id") {

    val groupId = long("group_id")
    val lastEventDate = date("last_event_date").nullable()
}