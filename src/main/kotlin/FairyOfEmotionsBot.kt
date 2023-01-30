import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update


object FairyOfEmotionsBot : TelegramLongPollingBot() {
    class EmailValidator {
        companion object {
            @JvmStatic
            val EMAIL_REGEX = "^[A-Za-z](.*)([@])(.+)(\\.)(.+)"
            fun isEmailValid(email: String): Boolean {
                return EMAIL_REGEX.toRegex().matches(email)
            }
        }
    }

    private var token = ""
    private var dialogId = 0
    private var emotions = listOf<String>()
    private var currentIndex = 0
    private var currentRate = -1
    private val chatIdAndSheetsId = mutableMapOf<Long, String>()
    private var isMapUpdated = false
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
        if (!isMapUpdated) {
            SheetsManager.getChatIdAndSheetsId(chatIdAndSheetsId)
            isMapUpdated = true
        }
        if (update == null || update.message == null || update.message.chatId == null) {
            return
        }
        val chatId = update.message.chatId
        if (update.hasMessage()) {
            if (update.message.text == null) {
                update.message.text = ""
            }
            println(update.message.text)
            if (!chatIdAndSheetsId.containsKey(chatId) && !update.message.text.endsWith("start")) {
                createMessage(chatId, Strings.START)
            } else {
                try {
                    if (update.message.text == "/help") {
                        createMessage(chatId, Strings.HELP_MESSAGE)
                    } else if (update.message.text =="/start") {
                        if (chatIdAndSheetsId.containsKey(chatId)) {
                            createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                        } else {
                            val sheetsId = SheetsManager.createSpreadsheetFairy()
                            SheetsManager.addClient(sheetsId, chatId.toString())
                            chatIdAndSheetsId[chatId] = sheetsId
                            createMessage(chatId, Strings.START_MESSAGE)
                            dialogId = 1
                        }
                    } else if (update.message.text == "/tables") {
                        createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                    } else if (update.message.text == "/cancel") {
                        if (dialogId == 1) {
                            createMessage(chatId, Strings.END_REG)
                        } else {
                            createMessage(chatId, Strings.CANCELLATION_SUCCESS)
                        }
                        dialogId = 0
                    } else if (update.message.text == "/add_emotion") {
                        createMessage(chatId, Strings.WRITE_EMOTION)
                        dialogId = 2
                    } else if (update.message.text =="/add_rate") {
                        if (dialogId >= 3) {
                            createMessage(chatId, Strings.RATE_IN_PROCCESS)
                        } else {
                            emotions = SheetsManager.getAllEmotions(getSheetsId(chatId))
                            dialogId = 3
                            currentIndex = 0
                            createMessage(chatId, Strings.RATE_EMOTIONS)
                            createMessage(chatId, Strings.rateEmotion(emotions[currentIndex]))
                        }
                    } else if (update.message.text =="/get_emotions") {
                        createMessage(chatId, Strings.writeAllEmotions(getSheetsId(chatId)))
                    } else if (update.message.text == "/link_email") {
                        createMessage(chatId, Strings.LINK_EMAIL)
                        dialogId = 1
                    } else if (dialogId != 0) {
                        when (dialogId) {
                            1 -> {
                                val email = update.message.text
                                if (EmailValidator.isEmailValid(email)) {
                                    try {
                                        SheetsManager.givePermissionToSpreadsheet(
                                            getSheetsId(chatId), email
                                        )
                                        createMessage(chatId, Strings.accessIsGivenTo(email))
                                        createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                                        dialogId = 0
                                    } catch (e: Exception) {
                                        createMessage(chatId, Strings.EMAIL_WRONG)
                                    }
                                } else {
                                    createMessage(chatId, Strings.EMAIL_WRONG)
                                }
                            }
                            2 -> {
                                val emotionsForPrinting: List<String> =
                                    update.message.text.filterNot { it.isWhitespace() }.split(",")
                                SheetsManager.addEmotions(emotionsForPrinting, getSheetsId(chatId))
                                dialogId = 0
                                createMessage(chatId, Strings.EMOTION_ADD_SUCCESS)
                            }
                            3 -> {
                                var isCorrect = false
                                try {
                                    currentRate = update.message.text.toInt()
                                    if ((10 >= currentRate) && (currentRate >= 1)) {
                                        isCorrect = true
                                    }
                                } catch (e: Exception) {
                                    isCorrect = false
                                }
                                if (isCorrect) {
                                    SheetsManager.addRate(emotions[currentIndex++], currentRate, getSheetsId(chatId))
                                    if (currentIndex == emotions.size) {
                                        dialogId = 0
                                        createMessage(chatId, Strings.RATE_END)
                                    }
                                } else {
                                    createMessage(chatId, Strings.RATE_RETRY)
                                }
                                createMessage(chatId, Strings.rateEmotion(emotions[currentIndex]))
                            }
                        }
                    } else {
                        createMessage(chatId, Strings.UNKNOWN_MESSAGE)
                    }
                } catch (e: Exception) {
                    System.err.println(e.stackTrace)
                }
            }
        }
    }

    private fun getSheetsId(chatId: Long) = chatIdAndSheetsId[chatId]!!

    private fun createMessage(chatId: Long, text: String) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = text
        execute(sendMessage)
    }
}