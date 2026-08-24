/**
 * Таблица кодов ошибок сервера → человекочитаемые сообщения на русском
 */
const ERROR_CODES = {
    // ==================== AUTH / SECURITY ====================
    UNAUTHORIZED: "Требуется авторизация",
    TOKEN_EXPIRED: "Сессия истекла. Войдите заново",
    TOKEN_INVALID: "Недействительный токен",
    TOKEN_VERSION_MISMATCH: "Сессия устарела. Войдите заново",
    ACCESS_DENIED: "Доступ запрещён",

    // ==================== VALIDATION ====================
    VALIDATION_ERROR: "Ошибка валидации данных",
    INVALID_INPUT: "Некорректные входные данные",

    // ==================== USER ====================
    USER_NOT_FOUND: "Пользователь не найден",
    USER_NOT_ACTIVE: "Аккаунт не активирован",
    USER_NOT_FOUND_OR_DELETED: "Пользователь не найден или удалён",
    USER_ALREADY_EXISTS: "Пользователь уже существует",
    USERNAME_TAKEN: "Имя пользователя уже занято",
    EMAIL_TAKEN: "Электронная почта уже зарегистрирована",
    INVALID_CREDENTIALS: "Неверное имя пользователя или пароль",

    // ==================== CHAT ====================
    CHAT_NOT_FOUND: "Чат не найден",
    CHAT_DELETED: "Чат удалён",
    CHAT_NOT_FOUND_OR_DELETED: "Чат не найден или удалён",
    CHAT_NOT_GROUP: "Это не групповой чат",
    CHAT_INFO_NOT_CHANGEABLE: "Информацию о чате нельзя изменить",
    CHAT_MEMBERS_LIMIT: "Превышен лимит участников чата",

    // ==================== CHAT MEMBER ====================
    MEMBER_NOT_FOUND: "Пользователь не является участником чата",
    MEMBER_NOT_FOUND_OR_DELETED: "Пользователь не является участником чата или удалён",
    MEMBER_ALREADY_EXISTS: "Пользователь уже является участником чата",
    MEMBER_NOT_ADMIN: "Требуются права администратора группы",

    // ==================== MESSAGE ====================
    MESSAGE_NOT_FOUND: "Сообщение не найдено",
    MESSAGE_DELETED: "Сообщение удалено",
    MESSAGE_NOT_FOUND_OR_DELETED: "Сообщение не найдено или удалено",
    MESSAGE_EMPTY: "Текст сообщения не может быть пустым",
    MESSAGE_TOO_LONG: "Текст сообщения слишком длинный (макс. 10 000 символов)",
    MESSAGE_NOT_SENDER: "Вы не являетесь отправителем этого сообщения",

    // ==================== TOKEN / VERIFICATION ====================
    VERIFICATION_TOKEN_NOT_FOUND: "Недействительная ссылка подтверждения",
    VERIFICATION_TOKEN_EXPIRED: "Ссылка подтверждения истекла",
    VERIFICATION_TOKEN_TYPE_MISMATCH: "Неверный тип ссылки подтверждения",

    // ==================== GENERAL ====================
    NOT_FOUND: "Ресурс не найден",
    CONFLICT: "Конфликт данных",
    RATE_LIMITED: "Слишком много запросов. Подождите и попробуйте снова",
    INTERNAL_ERROR: "Внутренняя ошибка сервера. Попробуйте позже",
};

function getErrorMessage(code, fallback = null) {
    if (code && ERROR_CODES[code]) {
        return ERROR_CODES[code];
    }
    return fallback || "Произошла неизвестная ошибка";
}