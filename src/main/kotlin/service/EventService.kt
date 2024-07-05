package service

import com.charleskorn.kaml.Yaml
import dto.EventDto
import dto.EventSet
import enumeration.EventType
import kotlinx.serialization.decodeFromString
import org.koin.core.annotation.Single
import persistence.dao.DailyEventsDaoService
import persistence.dao.GroupUserDaoService
import persistence.entity.DailyEventsEntity
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

@Single
class EventService(
    private val groupUserDaoService: GroupUserDaoService,
    private val dailyEventsDaoService: DailyEventsDaoService
) {

    private val eventSet = javaClass.classLoader.getResource("events.yml")?.readText()
        ?.let { Yaml.default.decodeFromString<EventSet>(it) }!!
    private val random = Random.Default

    fun chooseEvent(event: DailyEventsEntity): EventDto? {
        if (random.nextDouble() > 0.5) {
            println("there is no event for group ${event.groupId} today")
            event.apply {
                lastEventDate = LocalDate.now()
            }
            return null
        }

        println("choosing event")
        val users = groupUserDaoService.findAllByGroup(event.groupId).toList().takeIf { it.size > 1 } ?: return null
        val userMoney = users.map { it.money }
        val standardDeviation = sqrt(userMoney.sumOf { (it - userMoney.average()).pow(2) } / userMoney.size)
        val poorUsers = users.filter { userMoney.average() - it.money > standardDeviation }
        val richUsers = users.filter { it.money - userMoney.average() > standardDeviation }
        val event = when {
            poorUsers.isNotEmpty() && richUsers.isNotEmpty() -> eventSet.events.random()
            poorUsers.isNotEmpty() -> eventSet.events
                .filter { it.type == EventType.POSITIVE }
                .random()
            richUsers.isNotEmpty() -> eventSet.events
                .filter { it.type == EventType.NEGATIVE }
                .random()
            else -> null
        } ?: return null

        return if (event.type == EventType.POSITIVE) {
            EventDto(
                user = poorUsers.random(),
                event = event,
                regard = round(standardDeviation * REGARD_MULTIPLIER)
            )
        } else {
            EventDto(
                user = richUsers.random(),
                event = event,
                regard = round(-standardDeviation * REGARD_MULTIPLIER)
            )
        }
    }

    fun createEventIfNeed(groupId: Long) {
        if (!dailyEventsDaoService.eventExists(groupId)) {
            dailyEventsDaoService.createEvent(groupId)
        }
    }

    fun updateEvent(groupId: Long) = dailyEventsDaoService.updateEvent(groupId)

    fun findUnprocessedEvents() = dailyEventsDaoService.findUnprocessedEvents()

    companion object {
        private const val REGARD_MULTIPLIER = 0.25
    }
}