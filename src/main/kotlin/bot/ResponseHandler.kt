package bot

import constants.FOEBotMessages
import daily.DailyTaskExecutor
import daily.ReminderTask
import sheets.SheetsManager

object ResponseHandler : ResponseHandlerInterface {
    private val EMAIL_REGEX =
        ("^(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)" +
                "*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]" +
                "|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]" +
                "*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(2(5[0-5]|[0-4][0-9])" +
                "|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])" +
                "|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\" +
                "[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])").toRegex()

    private fun isEmailValid(email: String): Boolean {
        return EMAIL_REGEX.matches(email)
    }

    class ClientData(
        var sheetsId: String?,
        var dialogMode: DialogMode,
        var currentIndex: Int,
        var emotions: List<String>?,
        var dailyTaskExecutor: DailyTaskExecutor?
    )

    private val mapClient = mutableMapOf<Long, ClientData>()

    init {
        try {
            SheetsManager.updateMaps(mapClient, FOEBot)
        } catch (e: Exception) {
            System.err.println("Updating chat ids and sheets ids failed!")
            e.printStackTrace()
        }
    }

    override fun helpCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        createMessage(chatId, FOEBotMessages.HELP_MESSAGE)
    }

    override fun startCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        if (mapClient.containsKey(chatId)) {
            createMessage(chatId, FOEBotMessages.createTablesMessage(getSheetsId(chatId)))
        } else {
            try {
                val sheetsId = SheetsManager.createSpreadsheetFairy()
                SheetsManager.addClient(sheetsId, chatId.toString())
                createMessage(chatId, FOEBotMessages.START_MESSAGE)
                mapClient[chatId] = ClientData(sheetsId, DialogMode.EMAIL, 0, mutableListOf(), null)
            } catch (e: Exception) {
                createMessage(chatId, FOEBotMessages.SHEETS_CREATION_ERROR)
                System.err.println("Something wrong happened when adding client to data base")
                e.printStackTrace()
            }
        }
    }

    override fun tablesCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        createMessage(chatId, FOEBotMessages.createTablesMessage(getSheetsId(chatId)))
    }

    override fun cancelCommand(chatId: Long) {
        if (mapClient[chatId]!!.dialogMode == DialogMode.EMAIL) {
            createMessage(chatId, FOEBotMessages.END_REG)
        } else {
            createMessage(chatId, FOEBotMessages.CANCELLATION_SUCCESS)
        }
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
    }

    override fun addEmotionCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.WRITE_EMOTION)
        mapClient[chatId]!!.dialogMode = DialogMode.ADD_EMOTION
    }

    override fun addRateCommand(chatId: Long) {
        if (mapClient[chatId]!!.dialogMode == DialogMode.ADD_RATE) {
            createMessage(chatId, FOEBotMessages.RATE_IN_PROCCESS)
        } else {
            try {
                mapClient[chatId]!!.emotions = SheetsManager.getAllEmotions(getSheetsId(chatId))
            } catch (e: Exception) {
                System.err.println("Something wrong happened when getting all emotions")
                e.printStackTrace()
            }
            mapClient[chatId]!!.dialogMode = DialogMode.ADD_RATE
            mapClient[chatId]!!.currentIndex = 0
            createMessage(chatId, FOEBotMessages.RATE_EMOTIONS)
            createMessage(chatId, FOEBotMessages.rateEmotion(getEmotion(chatId)))
        }
    }

    private fun getEmotion(chatId: Long) =
        mapClient[chatId]!!.emotions!![mapClient[chatId]!!.currentIndex]

    override fun getEmotionsCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        try {
            createMessage(chatId, FOEBotMessages.writeAllEmotions(getSheetsId(chatId)))
        } catch (e: Exception) {
            createMessage(chatId, FOEBotMessages.GET_EMOTIONS_FAIL)
            System.err.println("Something went wrong when getting emotions")
            e.printStackTrace()
        }
    }

    override fun getTimeCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        try {
            createMessage(chatId, FOEBotMessages.writeTime(mapClient[chatId]!!.dailyTaskExecutor))
        } catch (e: Exception) {
            createMessage(chatId, FOEBotMessages.GET_TIME_FAIL)
            System.err.println("Something went wrong when getting time")
            e.printStackTrace()
        }
    }

    override fun linkEmailCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.LINK_EMAIL)
        mapClient[chatId]!!.dialogMode = DialogMode.EMAIL
    }

    override fun setTimeCommand(chatId: Long) {
        createMessage(chatId, FOEBotMessages.SET_TIME)
        mapClient[chatId]!!.dialogMode = DialogMode.SET_TIME
    }

    override fun cancelReminderCommand(chatId: Long) {
        mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
        createMessage(chatId, FOEBotMessages.CANCEL_REMINDER)
        if (isDailyExecutorNotNull(chatId)) {
            mapClient[chatId]!!.dailyTaskExecutor!!.stop()
            mapClient[chatId]!!.dailyTaskExecutor = null
        }
        try {
            SheetsManager.cancelReminder(chatId.toString(), getSheetsId(chatId))
            createMessage(chatId, FOEBotMessages.CANCEL_REMINDER_SUCCESS)
        } catch (e: Exception) {
            createMessage(chatId, FOEBotMessages.CANCEL_REMINDER_FAIL)
        }
    }

    fun isDailyExecutorNotNull(chatId: Long) =
        mapClient.containsKey(chatId) && mapClient[chatId]!!.dailyTaskExecutor != null

    override fun notCommand(chatId: Long, text: String) {
        when (mapClient[chatId]!!.dialogMode) {
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
                    mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
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
                mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
                createMessage(chatId, FOEBotMessages.EMOTION_ADD_SUCCESS)
            }
            DialogMode.ADD_RATE -> {
                try {
                    val currentRate = text.toInt()
                    if ((10 >= currentRate) && (currentRate >= 1)) {
                        SheetsManager.addRate(
                            getEmotion(chatId),
                            currentRate,
                            getSheetsId(chatId)
                        )
                        mapClient[chatId]!!.currentIndex++
                        if (mapClient[chatId]!!.currentIndex == mapClient[chatId]!!.emotions!!.size) {
                            mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
                            createMessage(chatId, FOEBotMessages.RATE_END)
                        }
                    } else {
                        createMessage(chatId, FOEBotMessages.RATE_RETRY_WRONG_RANGE)
                    }
                } catch (e: Exception) {
                    createMessage(chatId, FOEBotMessages.RATE_RETRY_NOT_INT)
                }
                createMessage(chatId, FOEBotMessages.rateEmotion(getEmotion(chatId)))
            }
            DialogMode.SET_TIME -> {
                try {
                    val hoursAndMinutes = text.split(":")
                    val hours = hoursAndMinutes[0].toInt()
                    val minutes = hoursAndMinutes[1].toInt()
                    val dailyTaskExecutorValue = DailyTaskExecutor(ReminderTask(FOEBot), hours, minutes)
                    dailyTaskExecutorValue.startExecution(chatId)
                    if (isDailyExecutorNotNull(chatId)) {
                        mapClient[chatId]!!.dailyTaskExecutor!!.stop()
                    }
                    mapClient[chatId]!!.dailyTaskExecutor = dailyTaskExecutorValue
                    SheetsManager.setTime(chatId.toString(), getSheetsId(chatId), hours, minutes)
                    createMessage(chatId, FOEBotMessages.SET_TIME_SUCCESS)
                } catch (e: Exception) {
                    createMessage(chatId, FOEBotMessages.SET_TIME_FAIL)
                }
                mapClient[chatId]!!.dialogMode = DialogMode.DEFAULT
            }
            else -> createMessage(chatId, FOEBotMessages.UNKNOWN_MESSAGE)
        }
    }

    private fun getSheetsId(chatId: Long) = mapClient[chatId]!!.sheetsId!!

    private fun createMessage(chatId: Long, text: String) {
        FOEBot.createMessage(chatId, text)
    }
}