package persistence.table

import enumeration.FeatureToggle
import org.jetbrains.exposed.dao.id.LongIdTable

object FeatureToggleTable : LongIdTable("feature_toggle", "id") {
    val groupId = long("group_id")
    val feature = enumerationByName<FeatureToggle>("feature", 255)
    val value = varchar("feature_value", 255)
}