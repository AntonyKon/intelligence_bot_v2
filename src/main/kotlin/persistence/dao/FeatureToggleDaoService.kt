package persistence.dao

import enumeration.FeatureToggle
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.koin.core.annotation.Single
import persistence.entity.FeatureToggleEntity
import persistence.table.FeatureToggleTable

@Single
class FeatureToggleDaoService {

    fun readValueForEnum(groupId: Long, feature: FeatureToggle) = FeatureToggleEntity.find {
        (FeatureToggleTable.feature eq feature) and (FeatureToggleTable.groupId eq groupId)
    }.firstOrNull()?.value
        ?: FeatureToggleEntity.new {
            this.groupId = groupId
            this.feature = feature
            this.value = feature.defaultValue
        }.value

    fun updateOrCreateValueForFeatureToggle(feature: FeatureToggle, groupId: Long, value: String) =
        FeatureToggleEntity.findSingleByAndUpdate(
            (FeatureToggleTable.feature eq feature) and (FeatureToggleTable.groupId eq groupId)
        ) {
            it.value = value
        } ?: FeatureToggleEntity.new {
            this.feature = feature
            this.groupId = groupId
            this.value = value
        }
}