package persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable

object HandsomeFagStatsTable : LongIdTable("handsome_fag_stats") {
    val user = reference("group_user_id", GroupUsersTable)
    val fagCount = long("fag_count").default(0L)
    val handsomeCount = long("handsome_count").default(0L)
}