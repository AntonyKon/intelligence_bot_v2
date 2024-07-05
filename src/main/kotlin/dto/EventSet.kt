package dto

import enumeration.EventType
import kotlinx.serialization.Serializable

@Serializable
data class EventSet(
    val events: List<Event>
)

@Serializable
data class Event(
    val message: String,
    val type: EventType
)
