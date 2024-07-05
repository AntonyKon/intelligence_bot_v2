package dto

import persistence.entity.GroupUserEntity

data class EventDto(
    val user: GroupUserEntity,
    val event: Event,
    val regard: Double
)
