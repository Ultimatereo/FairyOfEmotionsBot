import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update


object FairyOfEmotionsBot : TelegramLongPollingBot() {
    private var token = ""
    private var dialogId = 0

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
                    createMessage(chatId, Constants.HELP_MESSAGE)
                } else if (update.message.text.endsWith("start")) {
                    createMessage(chatId, Constants.START_MESSAGE)
                    dialogId = 1
                } else if (update.message.text.endsWith("tables")) {
                    createMessage(chatId, Constants.createTablesMessage)
                } else if (update.message.text.endsWith("cancel")) {
                    dialogId = 0
                    createMessage(chatId, "Команда успешно отменена!")
                } else if (update.message.text.endsWith("addEmotion")) {
                    createMessage(chatId, Constants.WRITE_EMOTION)
                    dialogId = 2
                } else if (update.message.text.endsWith("addRate")) {
                    TODO()
                } else if (dialogId != 0) {
                    when (dialogId) {
                        1 -> {
                            val email = update.message.text
                            //TODO("I should be able to create sheet for every user.")
                            createMessage(chatId, """
                                Спасибочки за твой ответ, но эта функция пока что не работает)
                                Воспользуйся /help, чтобы посмотреть, что можно сделать ещё.
                            """.trimIndent())
                            dialogId = 0
                        }
                        2 -> {
                            val emotions = update.message.text.filterNot { it.isWhitespace() }.split(",")
                            for (emotion in emotions) {
                                SheetsManager.addEmotion(emotion)
                            }
                            dialogId = 0
                            createMessage(chatId, "Всё успешно добавлено!")
                        }
                    }
                    dialogId = 0
                } else {
                    createMessage(chatId, Constants.UNKNOWN_MESSAGE)
                }
            } catch (e: Exception) {
                System.err.println(e.stackTrace)
            }
        }
    }

    private fun createMessage(chatId: Long, text: String) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = text
        execute(sendMessage)
    }
}