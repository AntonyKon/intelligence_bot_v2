package persistence.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import persistence.table.DuelTable

class DuelEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DuelEntity>(DuelTable)

    var attacking by GroupUserEntity referencedOn DuelTable.attacking
    var defending by GroupUserEntity optionalReferencedOn DuelTable.defending
    var victorious by GroupUserEntity optionalReferencedOn DuelTable.victorious
    var groupId by DuelTable.groupId
    var duelDate by DuelTable.duelDate
}