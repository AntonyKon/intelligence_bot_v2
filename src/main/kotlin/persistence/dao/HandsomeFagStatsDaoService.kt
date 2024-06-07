package persistence.dao

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.koin.core.annotation.Single
import persistence.entity.GroupUserEntity
import persistence.entity.HandsomeFagStatsEntity
import persistence.table.HandsomeFagStatsTable

@Single
class HandsomeFagStatsDaoService {

    fun createOrUpdateStatsForUser(
        user: GroupUserEntity,
        isNeedToIncrementFag: Boolean,
        isNeedToIncrementHandsome: Boolean
    ) = HandsomeFagStatsEntity.findSingleByAndUpdate(HandsomeFagStatsTable.user eq user.id) {
        it.fagCount += if (isNeedToIncrementFag) 1 else 0
        it.handsomeCount += if (isNeedToIncrementHandsome) 1 else 0
    } ?: HandsomeFagStatsEntity.new {
        this.user = user
        this.fagCount = if (isNeedToIncrementFag) 1 else 0
        this.handsomeCount = if (isNeedToIncrementHandsome) 1 else 0
    }
}