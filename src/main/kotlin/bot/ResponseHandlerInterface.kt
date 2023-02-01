package bot

interface ResponseHandlerInterface {
    fun helpCommand(chatId: Long)
    fun startCommand(chatId: Long)
    fun tablesCommand(chatId: Long)
    fun cancelCommand(chatId: Long)
    fun addEmotionCommand(chatId: Long)
    fun addRateCommand(chatId: Long)
    fun getEmotionsCommand(chatId: Long)
    fun getTimeCommand(chatId: Long)
    fun linkEmailCommand(chatId: Long)
    fun setTimeCommand(chatId: Long)
    fun cancelReminderCommand(chatId: Long)
    fun notCommand(chatId: Long, text: String)
    fun resetCommand(chatId: Long)
    fun supportCommand(chatId: Long)
}