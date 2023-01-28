import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


fun main() {
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(FairyOfEmotionsBot)
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}
