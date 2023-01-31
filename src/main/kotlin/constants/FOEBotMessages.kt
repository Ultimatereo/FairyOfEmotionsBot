package constants

import daily.DailyTaskExecutor
import sheets.SheetsManager

class FOEBotMessages {
    companion object {
        const val GET_TIME_FAIL: String = "Не получилось вывести время напоминания"
        const val GET_EMOTIONS_FAIL: String = "Не получилось вывести все эмоции"
        const val SHEETS_CREATION_ERROR: String = "Не получилось создать для вас таблицу :(\n" +
                "Попробуйте ещё раз создать таблицу с помощью /start"
        const val DAILY_REMINDER: String = "Это твоё ежедневное напоминание!\n" +
                "Скорее пиши /add_rate и записывай, как ты себя чувствуешь <3"
        const val SET_TIME_FAIL: String = "Данные были введены неверно. Попробуйте ещё раз."
        const val SET_TIME_SUCCESS: String = "Напоминалка успешно поставлена!"
        const val CANCEL_REMINDER_FAIL: String = "Не получилось отключить напоминалку("
        const val CANCEL_REMINDER_SUCCESS: String = "Напоминалка успешно отключена"
        const val CANCEL_REMINDER: String = "Отменяю напоминалку..."
        const val SET_TIME: String =
            "Введи, пожалуйста, время по МСК в формате HH:mm\n" +
                    "Например, 10:00"
        const val LINK_EMAIL: String =
            "Введите, пожалуйста, почту gmail или любую другую, привязанную к гуглу.\n" +
                    "Если вы введёте почту, никак не связанную с гуглом, то вы не получите доступ к таблице"
        private const val RATE_RETRY = "Введите оценку ещё раз. Оценка должна быть от 1 до 10!\n" +
                "Если вы хотите отменить запись, то просто напишите /cancel"
        const val RATE_RETRY_NOT_INT: String = "$RATE_RETRY Вы же ввели не число!"
        const val RATE_RETRY_WRONG_RANGE: String = "$RATE_RETRY Вы же ввели число не от 1 до 10!"
        const val RATE_END = "Поздравляю, чекап завершён!"
        const val EMOTION_ADD_SUCCESS = "Всё успешно добавлено!"
        const val EMOTION_ADD_FAIL = "Что-то пошло не так. И не получилось добавить эмоцию :("
        const val EMAIL_WRONG =
            "Что-то не так с вашей почтой. Введите её ещё раз.\n" +
                    "Убедитесь, что вы вписали корректную почту."
        const val RATE_IN_PROCCESS = "Запись оценки по эмоциям в процессе! " +
                "Ответьте, пожалуйста, на последний вопрос!"
        const val CANCELLATION_SUCCESS = "Команда успешно отменена!"
        const val END_REG =
            "Привязка почты прервана успешно.\n" +
                    "Если к таблице не будет привязана ваша почта, то вы не получите доступ к ней.\n" +
                    "Вы можете привязать почту через команду /link_email"
        const val RATE_EMOTIONS: String = "Начнём же оценку каждой из эмоций, дорогой друг."
        const val WRITE_EMOTION =
            "Введите через запятую все эмоции, за которыми вы хотите следить. \nДля отмены операции введите /cancel."
        const val HELP_MESSAGE =
            "Доступны следующие команды:\n\n" +
                    "/help - Вывод всех доступных команд\n" +
                    "/start - Начало работы с ботом. Создание таблицы\n" +
                    "/tables - Вывод таблицы\n" +
                    "/add_emotion - Добавить эмоцию в список трекаемых эмоций\n" +
                    "/add_rate - Добавить запись, оценку эмоций\n" +
                    "/get_emotions - Выводит список всех трекуемых эмоций через запятую\n" +
                    "/link_email - Привязать почту к таблице\n" +
                    "/set_time - Установить или изменить время напоминания\n" +
                    "/get_time - Узнать время напоминания\n" +
                    "/cancel_reminder - Отключить функцию напоминания"
        const val START_MESSAGE =
            "Приветики, для начала работы отправь, пожалуйста, свою почту на @gmail.com или привязанную почту к Google-аккаунту.\n" +
                    "Пожалуйста, указывайте реальную почту, иначе вы не получите доступа к таблице с данными\n" +
                    "Например vasya@gmail.com\n" +
                    "Если ты хочешь отменить создание таблицы, то просто напиши /cancel"
        const val UNKNOWN_MESSAGE = "Я не понимаю, чего вы добиваетесь этим сообщением."

        fun rateEmotion(emotion: String): String {
            return "Оцени, как сильно ты сегодня проявлял(а) эмоцию '$emotion' от 1 до 10"
        }

        fun accessIsGivenTo(email: String): String {
            return "Доступ к таблице успешно выдан на почту $email"
        }

        fun writeAllEmotions(sheetsId: String) =
            """
            Вот список всех эмоций, которые трекуются:
            ${SheetsManager.getAllEmotions(sheetsId).joinToString(", ")}
            """.trimIndent()

        fun createTablesMessage(sheetsId: String): String =
            """
                Вот твоя таблица со всеми эмоциями и записями:
                https://docs.google.com/spreadsheets/d/$sheetsId/edit#gid=0
            """.trimIndent()

        fun writeTime(dailyTaskExecutor: DailyTaskExecutor?): String {
            if (dailyTaskExecutor == null) {
                return "Время напоминания не установлено!"
            }
            return "Установленное время напоминания: ${dailyTaskExecutor.targetHour}:${dailyTaskExecutor.targetMin}."
        }
    }
}