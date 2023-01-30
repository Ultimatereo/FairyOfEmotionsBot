package bot

import daily.ReminderTask
import org.telegram.telegrambots.bots.TelegramLongPollingBot

abstract class CustomBotInterface : TelegramLongPollingBot(), ReminderTask.Callback