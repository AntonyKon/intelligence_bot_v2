package exception

class TelegramBusinessException(
    private val error: TelegramError
) : RuntimeException(error.definition)

enum class TelegramError(
    val definition: String
) {
    USER_LIST_IS_EMPTY("Список пользователей пуст, невозможно выполнить действие"),
    UNKNOWN_MESSAGE_TYPE("Неопознанный тип сообщения"),
    UNKNOWN_FEATURE("Неизвестный фичатогл"),
    USER_IS_NOT_FOUND("Пользователь не найден")
}
