package enumeration

import dev.inmo.tgbotapi.types.dice.Dice

enum class DiceType(
    private val emoji: String
) {

    DICE("\uD83C\uDFB2"),
    DARTS("\uD83C\uDFAF"),
    FOOTBALL("⚽"),
    SLOT_MACHINE("\uD83C\uDFB0"),
    BOWLING("\uD83C\uDFB3"),
    BASKETBALL("\uD83C\uDFC0");

    companion object {
        fun getType(dice: Dice) = values().find { it.emoji == dice.animationType.emoji }
    }
}