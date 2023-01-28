import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update


object FairyOfEmotionsBot : TelegramLongPollingBot() {
    private var token = ""
    override fun getBotToken(): String {
        if (token.isEmpty()) {
            token = ProjectProperties.mainProperties.getProperty("BOT_TOKEN")
            if (token.isEmpty()) {
                System.err.println("Bot token not found in main.properties resources!")
            }
        }
        return token
    }

    override fun getBotUsername(): String {
        return "FairyOfEmotionsBot"
    }

    override fun onUpdateReceived(update: Update?) {
        if (update == null || update.message == null || update.message.chatId == null) {
            return
        }
        val chatId = update.message.chatId
        if (update.hasMessage()) {
            if (update.message.text == null) {
                update.message.text = ""
            }
            println(update.message.text)
            try {
                if (update.message.text.endsWith("help")) {
                    val sendMessage = SendMessage()
                    sendMessage.chatId = chatId.toString()
                    sendMessage.text =
                        """
                            Доступны следующие команды:
                            
                            /help - Вывод всех доступных команд
                            /start - Начало работы с ботом. Создание таблицы
                            /tables - Вывод таблицы
                            /addEmotion - Добавить эмоцию в список трекаемых эмоций
                            /addRate - Добавить запись, оценку эмоций                         
                        """.trimIndent()
                    execute(sendMessage)
                } else {
                    val sendMessage = SendMessage()
                    sendMessage.chatId = chatId.toString()
                    sendMessage.text = "Я не понимаю, чего вы добиваетесь этим сообщением."
                    execute(sendMessage)
                }
            } catch (e: Exception) {
                System.err.println(e.stackTrace)
            }
        }
    }
}