package persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object DuelTable : LongIdTable("duels", "id") {
    val attacking = reference("attacking_user", GroupUsersTable)
    val defending = reference("defending_user", GroupUsersTable).nullable()
    val victorious = reference("victorious_user", GroupUsersTable).nullable()
    val groupId = long("group_id")
    val duelDate = date("duel_date").clientDefault { LocalDate.now() }
}