package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.FeatureToggleTable

class FeatureToggleEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<FeatureToggleEntity>(FeatureToggleTable)

    var groupId by FeatureToggleTable.groupId
    var feature by FeatureToggleTable.feature
    var value by FeatureToggleTable.value
}