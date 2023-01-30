interface Strings {
    companion object {
        val LINK_EMAIL: String =
            """
                Введите, пожалуйста, почту gmail или любую другую, привязанную к гуглу.
                Если вы введёте почту, никак не связанную с гуглом, то вы не получите доступ к таблице
            """.trimIndent()
        const val RATE_RETRY = "Введите оценку ещё раз. Оценка должна быть от 1 до 10!"
        const val RATE_END = "Поздравляю, чекап завершён!"
        const val EMOTION_ADD_SUCCESS = "Всё успешно добавлено!"
        val EMAIL_WRONG =
            """
            Что-то не так с вашей почтой. Введите её ещё раз.
            Убедитесь, что вы вписали корректную почту.
            """.trimIndent()

        const val RATE_IN_PROCCESS = "Запись оценки по эмоциям в процессе! " +
                "Ответьте, пожалуйста, на последний вопрос!"
        const val CANCELLATION_SUCCESS = "Команда успешно отменена!"
        val END_REG =
            """
                Привязка почты прервана успешно. 
                Если к таблице не будет привязана ваша почта, то вы не получите доступ к ней.
                Вы можете привязать почту через команду /link_email
            """.trimIndent()
        const val START = "Введите /start для начала работы с ботом!"
        fun rateEmotion(emotion: String): String {
            return "Оцени, как сильно ты сегодня проявлял(а) эмоцию '$emotion' от 1 до 10"
        }

        val RATE_EMOTIONS: String =
            """
            Начнём же оценку каждой из эмоций, дорогой друг.
            """.trimIndent()

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

        fun accessIsGivenTo(email: String): String {
            return "Доступ к таблице успешно выдан на почту $email"
        }

        const val WRITE_EMOTION =
            "Введите через запятую все эмоции, за которыми вы хотите следить. \nДля отмены операции введите /cancel."
        val HELP_MESSAGE =
            """ 
            Доступны следующие команды:
            
            /help - Вывод всех доступных команд
            /start - Начало работы с ботом. Создание таблицы
            /tables - Вывод таблицы
            /add_emotion - Добавить эмоцию в список трекаемых эмоций
            /add_rate - Добавить запись, оценку эмоций     `
            /get_emotions - Выводит список всех трекуемых эмоций через запятую
            /link_email - Привязать почту к таблице
            """.trimIndent()
        val START_MESSAGE =
            """
            Приветики, для начала работы отправь, пожалуйста, свою почту на @gmail.com или привязанную почту к Google-аккаунту.
            Пожалуйста, указывайте реальную почту, иначе вы не получите доступа к таблице с данными
            Например vasya@gmail.com
            Если ты хочешь отменить создание таблицы, то просто напиши /cancel
            """.trimIndent()

        const val UNKNOWN_MESSAGE = "Я не понимаю, чего вы добиваетесь этим сообщением."
    }
}