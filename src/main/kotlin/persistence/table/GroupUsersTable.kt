package persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable

object GroupUsersTable : LongIdTable(name = "group_users", columnName = "id") {
    val telegramId = long("telegram_id")
    val groupId = long("group_id")
    val money = double("money").default(START_MONEY_VALUE)
    val isAdmin = bool("is_admin").default(false)
}

const val START_MONEY_VALUE = 1000.0