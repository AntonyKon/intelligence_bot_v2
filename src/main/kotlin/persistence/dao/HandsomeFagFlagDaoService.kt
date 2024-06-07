package persistence.dao

import org.jetbrains.exposed.dao.id.EntityID
import org.koin.core.annotation.Single
import persistence.entity.GroupUserEntity
import persistence.entity.HandsomeFagFlagEntity
import persistence.table.HandsomeFagFlagsTable
import java.time.LocalDate

@Single
class HandsomeFagFlagDaoService {

    fun markUserAsFag(groupId: Long, groupUser: GroupUserEntity) = HandsomeFagFlagEntity.findByIdAndUpdate(groupId) {
        it.fagFlagUser = groupUser
        it.fagFlagDate = LocalDate.now()
    }

    fun markUserAsHandsome(groupId: Long, groupUser: GroupUserEntity) = HandsomeFagFlagEntity.findByIdAndUpdate(groupId) {
        it.handsomeFlagUser = groupUser
        it.handsomeFlagDate = LocalDate.now()
    }

    fun findByGroupId(groupId: Long) = HandsomeFagFlagEntity.findById(groupId)

    fun saveByGroupId(groupId: Long) = HandsomeFagFlagEntity.new {
        this.groupId = EntityID(groupId, HandsomeFagFlagsTable)
    }
}