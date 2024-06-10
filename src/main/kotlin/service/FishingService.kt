package service

import com.charleskorn.kaml.Yaml
import dto.FishCatch
import dto.Fishing
import exception.TelegramBusinessException
import exception.TelegramError
import kotlinx.serialization.decodeFromString
import org.koin.core.annotation.Single
import persistence.entity.GroupUserEntity
import kotlin.random.Random


@Single
class FishingService {

    val random = Random.Default

    fun gainFish(user: GroupUserEntity): FishCatch {
        var accumulationSum = 0.0
        val catches = javaClass.classLoader.getResource("fishing.yml")?.readText()
            ?.let { Yaml.default.decodeFromString<Fishing>(it) }
            ?.catches
            ?.filter { !(user.money == 0.0 && it.regard < 0) }
            ?.map {
                accumulationSum += it.probability
                it to accumulationSum
            } ?: throw TelegramBusinessException(TelegramError.FISHING_NOT_FOUND)

        val randomNum = random.nextDouble() * accumulationSum

        return catches.first { it.second >= randomNum }.first
    }
}