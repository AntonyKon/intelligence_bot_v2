package service

import com.charleskorn.kaml.Yaml
import dto.FishCatch
import dto.Fishing
import exception.TelegramBusinessException
import exception.TelegramError
import kotlinx.serialization.decodeFromString
import org.koin.core.annotation.Single
import persistence.dao.FishingStatsDaoService
import persistence.entity.GroupUserEntity
import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random


@Single
class FishingService(
    private val fishingStatsDaoService: FishingStatsDaoService
) {

    private val random = Random.Default
    private val fishing = javaClass.classLoader.getResource("fishing.yml")?.readText()
        ?.let { Yaml.default.decodeFromString<Fishing>(it) }

    fun gainFish(user: GroupUserEntity): FishCatch? {
        val userFishingStats = fishingStatsDaoService.findByUserOrCreate(user).apply {
            if (Duration.between(lastCatchTime, LocalDateTime.now()).toHours() >= 24) {
                catchAmount = 0.0
            }
        }

        if (userFishingStats.catchAmount >= CATCH_LIMIT) {
            return null
        }

        var accumulationSum = 0.0
        val catches = fishing?.catches
            ?.filterNot { user.money == 0.0 && it.regard < 0 }
            ?.map {
                accumulationSum += it.probability
                it to accumulationSum
            } ?: throw TelegramBusinessException(TelegramError.FISHING_NOT_FOUND)

        val randomNum = random.nextDouble() * accumulationSum
        return catches.first { it.second >= randomNum }.first
            .also {
                fishingStatsDaoService.updateFishingStats(
                    userFishingStats,
                    userFishingStats.catchAmount + it.esteem
                )
            }
    }

    companion object {
        private const val CATCH_LIMIT = 5.0
    }
}