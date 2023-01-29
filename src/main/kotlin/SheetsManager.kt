import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*


object SheetsManager {
    private const val APPLICATION_NAME = "Fairy Of Emotions"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"

    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS)
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"

    private val sheetsId = ProjectProperties.sheetsProperties.getProperty("SHEETS_ID")
    private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val service: Sheets = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build()

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @Throws(IOException::class)
    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val `in` = SheetsManager::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        // Build flow and trigger user authorization request.
        val flow: GoogleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver: LocalServerReceiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    private const val emotionRange = "Эмоции!A2:A"
    private const val ratesRange = "Записи!A2:A"
    private const val insertDataOption = "INSERT_ROWS"
    private const val valueInputOption = "USER_ENTERED"

    fun addEmotion(emotion: String) {
        addEmotions(listOf(emotion))
    }

    fun addEmotions(emotions: List<String>) {
        insertColumn(emotions, emotionRange)
    }

    fun addRate(emotion: String, rate: Int) {
        insertRow(
            mutableListOf(SimpleDateFormat("HH:mm dd.MM.yyyy").format(Date()), emotion, rate.toString()),
            ratesRange
        )
    }

    private fun insert(list: List<String>, range: String, majorDimension: String) {
        val requestBody = ValueRange()
        requestBody.majorDimension = majorDimension
        requestBody.range = range
        requestBody.setValues(mutableListOf(list) as List<MutableList<Any>>?)
        val request : Sheets.Spreadsheets.Values.Append =
            service.spreadsheets().values().append(sheetsId, range, requestBody)
        request.valueInputOption = valueInputOption
        request.insertDataOption = insertDataOption
        val response : AppendValuesResponse = request.execute()
        println(response)
    }
    private fun insertColumn(list: List<String>, range: String) {
        insert(list, range, "COLUMNS")
    }
    private fun insertRow(list: List<String>, range: String) {
        insert(list, range, "ROWS")
    }
    fun getAllEmotions(): List<String> {
        val request = service.spreadsheets().values().get(sheetsId, emotionRange)
        val response = request.execute()
        val values = response.getValues() ?: return listOf()
        val list = mutableListOf<String>()
        for (row in values) {
            for (element in row) {
                if (element.toString().isNotEmpty()) {
                    list.add(element.toString())
                }
            }
        }
        println(list)
        return list
    }
}