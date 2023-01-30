package bot

interface ResponseHandlerInterface {
    fun help(chatId: Long)
    fun start(chatId: Long)
    fun tables(chatId: Long)
    fun cancel(chatId: Long)
    fun addEmotion(chatId: Long)
    fun addRate(chatId: Long)
    fun getEmotions(chatId: Long)
    fun linkEmail(chatId: Long)
    fun setTime(chatId: Long)
    fun cancelReminder(chatId: Long)
    fun notCommand(chatId: Long, text: String)
}