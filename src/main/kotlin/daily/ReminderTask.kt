package daily

class ReminderTask(private val callback: Callback) : DailyTask {
    interface Callback {
        fun onTimeForDailyTask()
    }

    override fun execute() {
        callback.onTimeForDailyTask()
    }
}