interface Constants {
    companion object {
        val createTablesMessage: String =
            """
                Вот твоя таблица со всеми эмоциями и записями:
                https://docs.google.com/spreadsheets/d/${ProjectProperties.sheetsProperties.getProperty("SHEETS_ID")}/edit#gid=0
            """.trimIndent()
        val WRITE_EMOTION = "Введите через запятую все эмоции, за которыми вы хотите следить"
        val HELP_MESSAGE =
            """ 
            Доступны следующие команды:
            
            /help - Вывод всех доступных команд
            /start - Начало работы с ботом. Создание таблицы
            /tables - Вывод таблицы
            /addEmotion - Добавить эмоцию в список трекаемых эмоций
            /addRate - Добавить запись, оценку эмоций     `
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