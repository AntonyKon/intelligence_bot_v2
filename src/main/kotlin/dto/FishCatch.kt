package dto

import enumeration.CatchType
import kotlinx.serialization.Serializable


@Serializable
data class FishCatch(
    val name: CatchType,
    val message: String,
    val probability: Double,
    val regard: Double,
    val esteem: Double
)

@Serializable
data class Fishing(
    val catches: List<FishCatch>
)
