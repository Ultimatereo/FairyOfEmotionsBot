package bot

import constants.Strings
import daily.DailyTaskExecutor
import daily.ReminderTask
import sheets.SheetsManager
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

    override fun help(chatId: Long) = createMessage(chatId, Strings.HELP_MESSAGE)

    override fun start(chatId: Long) {
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
            dialogMode[chatId] = DialogMode.EMAIL
        }
    }

    override fun tables(chatId: Long) = createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))

    override fun cancel(chatId: Long) {
        if (dialogMode[chatId] == DialogMode.EMAIL) {
            createMessage(chatId, Strings.END_REG)
        } else {
            createMessage(chatId, Strings.CANCELLATION_SUCCESS)
        }
        dialogMode[chatId] = DialogMode.DEFAULT
    }

    override fun addEmotion(chatId: Long) {
        createMessage(chatId, Strings.WRITE_EMOTION)
        dialogMode[chatId] = DialogMode.ADD_EMOTION
    }

    override fun addRate(chatId: Long) {
        if (dialogMode[chatId] == DialogMode.ADD_RATE) {
            createMessage(chatId, Strings.RATE_IN_PROCCESS)
        } else {
            try {
                emotions[chatId] = SheetsManager.getAllEmotions(getSheetsId(chatId))
            } catch (e: Exception) {
                System.err.println("Something wrong happened when getting all emotions")
                e.printStackTrace()
            }
            dialogMode[chatId] = DialogMode.ADD_RATE
            currentIndex[chatId] = 0
            createMessage(chatId, Strings.RATE_EMOTIONS)
            createMessage(chatId, Strings.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
        }
    }

    override fun getEmotions(chatId: Long) {
        try {
            createMessage(chatId, Strings.writeAllEmotions(getSheetsId(chatId)))
        } catch (e: Exception) {
            System.err.println("Something went wrong when getting emotions")
            e.printStackTrace()
        }
    }

    override fun linkEmail(chatId: Long) {
        createMessage(chatId, Strings.LINK_EMAIL)
        dialogMode[chatId] = DialogMode.EMAIL
    }

    override fun setTime(chatId: Long) {
        createMessage(chatId, Strings.SET_TIME)
        dialogMode[chatId] = DialogMode.SET_TIME
    }

    override fun cancelReminder(chatId: Long) {
        createMessage(chatId, Strings.CANCEL_REMINDER)
        if (dailyTaskExecutor.containsKey(chatId)) {
            dailyTaskExecutor[chatId]!!.stop()
            dailyTaskExecutor.remove(chatId)
        }
        try {
            SheetsManager.cancelReminder(chatId.toString(), getSheetsId(chatId))
            createMessage(chatId, Strings.CANCEL_REMINDER_SUCCESS)
        } catch (e: Exception) {
            createMessage(chatId, Strings.CANCEL_REMINDER_FAIL)
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
                        createMessage(chatId, Strings.EMAIL_WRONG)
                    }
                    createMessage(chatId, Strings.accessIsGivenTo(text))
                    createMessage(chatId, Strings.createTablesMessage(getSheetsId(chatId)))
                    dialogMode[chatId] = DialogMode.DEFAULT
                } else {
                    createMessage(chatId, Strings.EMAIL_WRONG)
                }
            }
            DialogMode.ADD_EMOTION -> {
                val emotionsForPrinting: List<String> =
                    text.filterNot { it.isWhitespace() }.split(",")
                try {
                    SheetsManager.addEmotions(emotionsForPrinting, getSheetsId(chatId))
                } catch (e: Exception) {
                    System.err.println(
                        "Something wen wrong when adding emotions " +
                                "for user with chatId $chatId"
                    )
                    e.printStackTrace()
                }
                dialogMode[chatId] = DialogMode.DEFAULT
                createMessage(chatId, Strings.EMOTION_ADD_SUCCESS)
            }
            DialogMode.ADD_RATE -> {
                var isCorrect = false
                var currentRate = 0
                try {
                    currentRate = text.toInt()
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
                    if (currentIndex[chatId] == emotions[chatId]!!.size) {
                        dialogMode[chatId] = DialogMode.DEFAULT
                        createMessage(chatId, Strings.RATE_END)
                    }
                } else {
                    createMessage(chatId, Strings.RATE_RETRY)
                }
                createMessage(chatId, Strings.rateEmotion(emotions[chatId]!![currentIndex[chatId]!!]))
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
                    createMessage(chatId, Strings.SET_TIME_SUCCESS)
                } catch (e: Exception) {
                    createMessage(chatId, Strings.SET_TIME_FAIL)
                }
                dialogMode[chatId] = DialogMode.DEFAULT
            }
            else -> createMessage(chatId, Strings.UNKNOWN_MESSAGE)
        }
    }

    private fun getSheetsId(chatId: Long) = chatIdAndSheetsId[chatId]!!

    private fun createMessage(chatId: Long, text: String) {
        FOEBot.createMessage(chatId, text)
    }
}