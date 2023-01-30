package bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import properties.ProjectProperties


object FOEBot : CustomBotInterface() {
    private var token = ""
    override fun getBotToken(): String {
        if (token.isEmpty()) {
            token = ProjectProperties.mainProperties.getProperty("BOT_TOKEN")
            if (token.isEmpty()) {
                System.err.println("Bot token wasn't found in main.properties!")
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

        if (!update.hasMessage()) return

        if (update.message.text == null) {
            update.message.text = ""
        }

        System.err.println(update.message.text)

        when (update.message.text) {
            "/help" -> ResponseHandler.help(chatId)
            "/start" -> ResponseHandler.start(chatId)
            "/tables" -> ResponseHandler.tables(chatId)
            "/cancel" -> ResponseHandler.cancel(chatId)
            "/add_emotion" -> ResponseHandler.addEmotion(chatId)
            "/add_rate" -> ResponseHandler.addRate(chatId)
            "/get_emotions" -> ResponseHandler.getEmotions(chatId)
            "/link_email" -> ResponseHandler.linkEmail(chatId)
            "/set_time" -> ResponseHandler.setTime(chatId)
            "/cancel_reminder" -> ResponseHandler.cancelReminder(chatId)
            else -> ResponseHandler.notCommand(chatId, update.message.text)
        }
    }

    fun createMessage(chatId: Long, text: String) {
        try {
            val sendMessage = SendMessage()
            sendMessage.chatId = chatId.toString()
            sendMessage.text = text
            execute(sendMessage)
        } catch (e: Exception) {
            System.err.println("Something wrong happened when trying to create a message")
            e.printStackTrace()
        }
    }

    override fun onTimeForDailyTask(chatId: Long) {
        createMessage(
            chatId,
            """
            Это твоё ежедневное напоминание! 
            Скорее пиши /add_rate и записывай, как ты себя чувствуешь <3 
            """.trimIndent()
        )
    }
}