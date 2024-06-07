package persistence.table

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.date

object HandsomeFagFlagsTable : IdTable<Long>("handsome_fag_flags") {
    val fagFlagUser = reference("fag_user", GroupUsersTable).nullable()
    val handsomeFlagUser = reference("handsome_user", GroupUsersTable).nullable()
    val fagFlagDate = date("fag_flag_date").nullable()
    val handsomeFlagDate = date("handsome_flag_date").nullable()
    override val id = long("group_id").entityId()
}