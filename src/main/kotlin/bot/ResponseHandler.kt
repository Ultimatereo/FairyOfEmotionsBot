package bot

import constants.FOEBotMessages
import daily.DailyTaskExecutor
import daily.ReminderTask
import sheets.SheetsManager
import java.io.IOException

object ResponseHandler : ResponseHandlerInterface {
    private const val EMAIL_REGEX = "^[A-Za-z](.*)([@])(.+)(\\.)(.+)"
    private fun isEmailValid(email: String): Boolean {
        return EMAIL_REGEX.toRegex().matches(email)
    }

    private val dialogMode = mutableMapOf<Long, DialogMode>() //DEFAULT
    private val emotions = mutableMapOf<Long, List<String>>()
    private val currentIndex = mutableMapOf<Long, Int>() //0
    private val chatIdAndSheetsId = mutableMapOf<Long, String>()
    private val dailyTaskExecutor = mutableMapOf<Long, DailyTaskExecutor>()

    init {
        try {
            SheetsManager.updateMaps(chatIdAndSheetsId, dailyTaskExecutor, FOEBot)
        } catch (e: Exception) {
            System.err.println("Updating chat ids and sheets ids failed!")
            e.printStackTrace()
        }
        for (element in chatIdAndSheetsId) {
            dialogMode[element.key] = DialogMode.DEFAULT
            currentIndex[element.key] = 0
        }
    }

    override fun helpCommand(chatId: Long) = createMessage(chatId, FOEBotMessages.HELP_MESSAGE)

    override fun startCommand(chatId: Long) {
        if (chatIdAndSheetsId.containsKey(chatId)) {
            createMessage(chatId, FOEBotMessages.createTablesMessage(getSheetsId(chatId)))
        } else {
            try {
                val sheetsId = SheetsManager.createSpreadsheetFairy()
                SheetsManager.addClient(sheetsId, chatId.toString())
                chatIdAndSheetsId[chatId] = sheetsId
                createMessage(chatId, FOEBotMessages.START_MESSAGE)
                dialogMode[chatId] = DialogMode.EMAIL
            } catch (e: Exception) {
                createMessage(chatId, FOEBotMessages.SHEETS_CREATION_ERROR)
                System.err.println("Something wrong happened when adding client to data base")
                e.printStackTrace()
            }
        }
    }

    override fun tablesCommand(chatId: Long) =
        createMessage(chatId, FOEBotMessages.createTablesMessage(getSheetsId(chatId)))

    override fun cancelCommand(chatId: Long) {
        if (dialogMode[chatId] == DialogMode.EMAIL) {
            createMessage(chatId, FOEBotMessages.END_REG)
        } else {
            createMessage(chatId, FOEBotMessages.CANCELLATION_SUCCESS)
        }
        dialogMode[chatId] = DialogMode.DEFAULT
    }

    override fun addEmotionCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.WRITE_EMOTION)
        dialogMode[chatId] = DialogMode.ADD_EMOTION
    }

    override fun addRateCommand(chatId: Long) {
        if (dialogMode[chatId] == DialogMode.ADD_RATE) {
            createMessage(chatId, FOEBotMessages.RATE_IN_PROCCESS)
        } else {
            try {
                emotions[chatId] = SheetsManager.getAllEmotions(getSheetsId(chatId))
            } catch (e: Exception) {
                System.err.println("Something wrong happened when getting all emotions")
                e.printStackTrace()
            }
            dialogMode[chatId] = DialogMode.ADD_RATE
            currentIndex[chatId] = 0
            createMessage(chatId, FOEBotMessages.RATE_EMOTIONS)
            createMessage(chatId, FOEBotMessages.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
        }
    }

    override fun getEmotionsCommand(chatId: Long) {
        try {
            createMessage(chatId, FOEBotMessages.writeAllEmotions(getSheetsId(chatId)))
        } catch (e: Exception) {
            System.err.println("Something went wrong when getting emotions")
            e.printStackTrace()
        }
    }

    override fun linkEmailCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.LINK_EMAIL)
        dialogMode[chatId] = DialogMode.EMAIL
    }

    override fun setTimeCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.SET_TIME)
        dialogMode[chatId] = DialogMode.SET_TIME
    }

    override fun cancelReminderCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.CANCEL_REMINDER)
        if (dailyTaskExecutor.containsKey(chatId)) {
            dailyTaskExecutor[chatId]!!.stop()
            dailyTaskExecutor.remove(chatId)
        }
        try {
            SheetsManager.cancelReminder(chatId.toString(), getSheetsId(chatId))
            createMessage(chatId, FOEBotMessages.CANCEL_REMINDER_SUCCESS)
        } catch (e: Exception) {
            createMessage(chatId, FOEBotMessages.CANCEL_REMINDER_FAIL)
        }
    }

    override fun notCommand(chatId: Long, text: String) {
        when (dialogMode[chatId]) {
            DialogMode.EMAIL -> {
                if (isEmailValid(text)) {
                    try {
                        SheetsManager.givePermissionToSpreadsheet(
                            getSheetsId(chatId), text
                        )
                    } catch (e: Exception) {
                        createMessage(chatId, FOEBotMessages.EMAIL_WRONG)
                        return
                    }
                    createMessage(chatId, FOEBotMessages.accessIsGivenTo(text))
                    createMessage(chatId, FOEBotMessages.createTablesMessage(getSheetsId(chatId)))
                    dialogMode[chatId] = DialogMode.DEFAULT
                } else {
                    createMessage(chatId, FOEBotMessages.EMAIL_WRONG)
                }
            }
            DialogMode.ADD_EMOTION -> {
                val emotionsForPrinting: List<String> =
                    text.split(",")
                try {
                    SheetsManager.addEmotions(emotionsForPrinting, getSheetsId(chatId))
                } catch (e: Exception) {
                    System.err.println(
                        "Something wen wrong when adding emotions " +
                                "for user with chatId $chatId"
                    )
                    e.printStackTrace()
                    createMessage(chatId, FOEBotMessages.EMOTION_ADD_FAIL)
                    return
                }
                dialogMode[chatId] = DialogMode.DEFAULT
                createMessage(chatId, FOEBotMessages.EMOTION_ADD_SUCCESS)
            }
            DialogMode.ADD_RATE -> {
                try {
                    val currentRate = text.toInt()
                    if ((10 >= currentRate) && (currentRate >= 1)) {
                        SheetsManager.addRate(
                            emotions[chatId]!![currentIndex[chatId]!!],
                            currentRate,
                            getSheetsId(chatId)
                        )
                        currentIndex[chatId] = currentIndex[chatId]!! + 1
                        if (currentIndex[chatId] == emotions[chatId]!!.size) {
                            dialogMode[chatId] = DialogMode.DEFAULT
                            createMessage(chatId, FOEBotMessages.RATE_END)
                        }
                    } else {
                        createMessage(chatId, FOEBotMessages.RATE_RETRY_WRONG_RANGE)
                    }
                } catch (e: Exception) {
                    createMessage(chatId, FOEBotMessages.RATE_RETRY_NOT_INT)
                }
                createMessage(chatId, FOEBotMessages.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
            }
            DialogMode.SET_TIME -> {
                try {
                    val hoursAndMinutes = text.split(":")
                    val hours = hoursAndMinutes[0].toInt()
                    val minutes = hoursAndMinutes[1].toInt()
                    val dailyTaskExecutorValue = DailyTaskExecutor(ReminderTask(FOEBot))
                    dailyTaskExecutorValue.startExecutionAt(
                        hours,
                        minutes,
                        0, chatId
                    )
                    if (dailyTaskExecutor.containsKey(chatId)) {
                        dailyTaskExecutor[chatId]!!.stop()
                    }
                    dailyTaskExecutor[chatId] = dailyTaskExecutorValue
                    SheetsManager.setTime(chatId.toString(), getSheetsId(chatId), hours, minutes)
                    createMessage(chatId, FOEBotMessages.SET_TIME_SUCCESS)
                } catch (e: Exception) {
                    createMessage(chatId, FOEBotMessages.SET_TIME_FAIL)
                }
                dialogMode[chatId] = DialogMode.DEFAULT
            }
            else -> createMessage(chatId, FOEBotMessages.UNKNOWN_MESSAGE)
        }
    }

    private fun getSheetsId(chatId: Long) = chatIdAndSheetsId[chatId]!!

    private fun createMessage(chatId: Long, text: String) {
        FOEBot.createMessage(chatId, text)
    }
}