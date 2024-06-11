package persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FishingStatsTable : LongIdTable("fishing_stats") {

    val user = reference("user_id", GroupUsersTable)
    val catchAmount = double("catch_amount").clientDefault { 0.0 }
    val lastCatchTime = datetime("last_catch_time").clientDefault { LocalDateTime.now() }
}