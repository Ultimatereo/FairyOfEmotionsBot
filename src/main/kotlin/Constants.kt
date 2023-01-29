interface Constants {
    companion object {
        fun rateEmotion(emotion: String): String {
            return "Оцени, как сильно ты сегодня проявлял(а) эмоцию '$emotion' от 1 до 10"
        }

        val RATE_EMOTIONS: String =
            """
            Начнём же оценку каждой из эмоций, дорогой друг.
            """.trimIndent()
        val WRITE_ALL_EMOTIONS: String get() =
            """
            Вот список всех эмоций, которые трекуются:
            ${SheetsManager.getAllEmotions().joinToString(", ")}
            """.trimIndent()
        val createTablesMessage: String =
            """
                Вот твоя таблица со всеми эмоциями и записями:
                https://docs.google.com/spreadsheets/d/${ProjectProperties.sheetsProperties.getProperty("SHEETS_ID")}/edit#gid=0
            """.trimIndent()
        val WRITE_EMOTION = "Введите через запятую все эмоции, за которыми вы хотите следить. \nДля отмены операции введите /cancel."
        val HELP_MESSAGE =
            """ 
            Доступны следующие команды:
            
            /help - Вывод всех доступных команд
            /start - Начало работы с ботом. Создание таблицы
            /tables - Вывод таблицы
            /addEmotion - Добавить эмоцию в список трекаемых эмоций
            /addRate - Добавить запись, оценку эмоций     `
            /getEmotions - Выводит список всех трекуемых эмоций через запятую
            """.trimIndent()
        val START_MESSAGE =
            """
            Приветики, для начала работы отправь, пожалуйста, свою почту на @gmail.com или привязанную почту к Google-аккаунту.
            Пожалуйста, указывайте реальную почту, иначе вы не получите доступа к таблице с данными
            Например vasya@gmail.com
            Если ты хочешь отменить создание таблицы, то просто напиши /cancel
            """.trimIndent()

        val UNKNOWN_MESSAGE = "Я не понимаю, чего вы добиваетесь этим сообщением."
    }
}