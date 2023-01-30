package bot

import constants.Strings
import properties.ProjectProperties
import sheets.SheetsManager
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
    private var dialogMode = mutableMapOf<Long, Int>() //0
    private var emotions = mutableMapOf<Long, List<String>>()
    private var currentIndex = mutableMapOf<Long, Int>() //0
    private val chatIdAndSheetsId = mutableMapOf<Long, String>()
    private var isMapUpdated = false

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
        if (!isMapUpdated) {
            try {
                SheetsManager.getChatIdAndSheetsId(chatIdAndSheetsId)
                isMapUpdated = true
            } catch (e: Exception) {
                System.err.println("Updating chat ids and sheets ids failed!")
                e.printStackTrace()
                return
            }
            for (element in chatIdAndSheetsId) {
                dialogMode[element.key] = 0
                currentIndex[element.key] = 0
            }
        }
        if (update == null || update.message == null || update.message.chatId == null) {
            return
        }
        val chatId = update.message.chatId

        if (!update.hasMessage()) return

        if (update.message.text == null) {
            update.message.text = ""
        }

        System.err.println(update.message.text)

        if (!chatIdAndSheetsId.containsKey(chatId) && update.message.text != "/start") {
            createMessage(chatId, Strings.START)
            return
        }
        when (update.message.text) {
            "/help" -> createMessage(chatId, Strings.HELP_MESSAGE)
            "/start" -> {
                if (chatIdAndSheetsId.containsKey(chatId)) {
                    createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                } else {
                    val sheetsId = SheetsManager.createSpreadsheetFairy()
                    try {
                        SheetsManager.addClient(sheetsId, chatId.toString())
                    } catch (e: Exception) {
                        System.err.println("Something wrong happened when adding client to data base")
                        e.printStackTrace()
                    }
                    chatIdAndSheetsId[chatId] = sheetsId
                    createMessage(chatId, Strings.START_MESSAGE)
                    dialogMode[chatId] = 1
                }
            }
            "/tables" -> createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
            "/cancel" -> {
                if (dialogMode[chatId] == 1) {
                    createMessage(chatId, Strings.END_REG)
                } else {
                    createMessage(chatId, Strings.CANCELLATION_SUCCESS)
                }
                dialogMode[chatId] = 0
            }
            "/add_emotion" -> {
                createMessage(chatId, Strings.WRITE_EMOTION)
                dialogMode[chatId] = 2
            }
            "/add_rate" -> {
                if (dialogMode[chatId] == 3) {
                    createMessage(chatId, Strings.RATE_IN_PROCCESS)
                } else {
                    try {
                        emotions[chatId] = SheetsManager.getAllEmotions(getSheetsId(chatId))
                    } catch (e: Exception) {
                        System.err.println("Something wrong happened when getting all emotions")
                        e.printStackTrace()
                    }
                    dialogMode[chatId] = 3
                    currentIndex[chatId] = 0
                    createMessage(chatId, Strings.RATE_EMOTIONS)
                    createMessage(chatId, Strings.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
                }
            }
            "/get_emotions" -> {
                try {
                    createMessage(chatId, Strings.writeAllEmotions(getSheetsId(chatId)))
                } catch (e: Exception) {
                    System.err.println("Something went wrong when getting emotions")
                    e.printStackTrace()
                }
            }
            "/link_email" -> {
                createMessage(chatId, Strings.LINK_EMAIL)
                dialogMode[chatId] = 1
            }
            else -> {
                when (dialogMode[chatId]) {
                    1 -> {
                        val email = update.message.text
                        if (EmailValidator.isEmailValid(email)) {
                            try {
                                SheetsManager.givePermissionToSpreadsheet(
                                    getSheetsId(chatId), email
                                )
                            } catch (e: Exception) {
                                createMessage(chatId, Strings.EMAIL_WRONG)
                            }
                            createMessage(chatId, Strings.accessIsGivenTo(email))
                            createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                            dialogMode[chatId] = 0
                        } else {
                            createMessage(chatId, Strings.EMAIL_WRONG)
                        }
                    }
                    2 -> {
                        val emotionsForPrinting: List<String> =
                            update.message.text.filterNot { it.isWhitespace() }.split(",")
                        try {
                            SheetsManager.addEmotions(emotionsForPrinting, getSheetsId(chatId))
                        } catch (e: Exception) {
                            System.err.println(
                                "Something wen wrong when adding emotions " +
                                        "for user with chatId $chatId"
                            )
                            e.printStackTrace()
                        }
                        dialogMode[chatId] = 0
                        createMessage(chatId, Strings.EMOTION_ADD_SUCCESS)
                    }
                    3 -> {
                        var isCorrect = false
                        var currentRate = 0
                        try {
                            currentRate = update.message.text.toInt()
                            if ((10 >= currentRate) && (currentRate >= 1)) {
                                isCorrect = true
                            }
                        } catch (e: Exception) {
                            isCorrect = false
                        }
                        if (isCorrect) {
                            SheetsManager.addRate(
                                emotions[chatId]!![currentIndex[chatId]!!],
                                currentRate,
                                getSheetsId(chatId)
                            )
                            currentIndex[chatId] = currentIndex[chatId]!! + 1
                            if (currentIndex[chatId] == emotions.size) {
                                dialogMode[chatId] = 0
                                createMessage(chatId, Strings.RATE_END)
                            }
                        } else {
                            createMessage(chatId, Strings.RATE_RETRY)
                        }
                        createMessage(chatId, Strings.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
                    }
                    else -> createMessage(chatId, Strings.UNKNOWN_MESSAGE)
                }
            }
        }
    }

    private fun getSheetsId(chatId: Long) = chatIdAndSheetsId[chatId]!!

    private fun createMessage(chatId: Long, text: String) {
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
}