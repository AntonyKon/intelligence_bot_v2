package persistence.dao

import org.koin.core.annotation.Single
import persistence.entity.FishingStatsEntity
import persistence.entity.GroupUserEntity
import persistence.table.FishingStatsTable
import java.time.LocalDateTime

@Single
class FishingStatsDaoService {

    fun findByUserOrCreate(user: GroupUserEntity) = FishingStatsEntity.find {
        FishingStatsTable.user eq user.id
    }.firstOrNull() ?: FishingStatsEntity.new {
        this.user = user
    }

    fun updateFishingStats(fishingStats: FishingStatsEntity, catchAmount: Double) = fishingStats.apply {
        this.catchAmount = catchAmount
        lastCatchTime = LocalDateTime.now()
    }
}