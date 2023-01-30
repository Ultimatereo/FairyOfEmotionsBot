package sheets

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
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import daily.DailyTaskExecutor
import daily.ReminderTask
import properties.ProjectProperties
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import com.google.api.services.drive.model.File as DriveFile


object SheetsManager {
    private const val APPLICATION_NAME = "Fairy Of Emotions"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private val SCOPES = SheetsScopes.all()
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"

    private val sheetsSampleId = ProjectProperties.sheetsProperties.getProperty("SHEETS_SAMPLE_ID")
    private val sheetsDataId = ProjectProperties.sheetsProperties.getProperty("SHEETS_DATA_ID")
    private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val credentials: Credential = getCredentials(HTTP_TRANSPORT)
    private val sheetsService: Sheets = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
        .setApplicationName(APPLICATION_NAME)
        .build()
    private val driveService: Drive = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
        .setApplicationName(APPLICATION_NAME).build()

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
    private const val dataRange = "data!A2:D"
    private const val insertDataOption = "INSERT_ROWS"
    private const val valueInputOption = "USER_ENTERED"


    fun givePermissionToSpreadsheet(id: String, email: String) {
        val newPermission = Permission()
        newPermission.type = "user"
        newPermission.role = "writer"
        newPermission.emailAddress = email
        driveService.permissions().create(id, newPermission).execute()
    }

    fun createSpreadsheetFairy(title: String = "Фея Эмоций"): String {
        val newSheetsId = driveService.files().copy(sheetsSampleId, DriveFile()).execute().id
        val requests = mutableListOf<Request>()
        requests.add(
            Request()
                .setUpdateSpreadsheetProperties(
                    UpdateSpreadsheetPropertiesRequest()
                        .setProperties(
                            SpreadsheetProperties()
                                .setTitle(title)
                        )
                        .setFields("title")
                )
        )
        val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
        sheetsService.spreadsheets().batchUpdate(newSheetsId, body).execute()
        return newSheetsId
    }

    fun addEmotion(emotion: String, sheetsId: String) {
        addEmotions(listOf(emotion), sheetsId)
    }

    fun addEmotions(emotions: List<String>, sheetsId: String) {
        insertColumn(emotions, emotionRange, sheetsId)
    }

    fun addRate(emotion: String, rate: Int, sheetsId: String) {
        insertRow(
            mutableListOf(SimpleDateFormat("HH:mm dd.MM.yyyy").format(Date()), emotion, rate.toString()),
            ratesRange, sheetsId
        )
    }

    private fun insert(list: List<String>, range: String, majorDimension: String, sheetsId: String) {
        val requestBody = ValueRange()
        requestBody.majorDimension = majorDimension
        requestBody.range = range
        requestBody.setValues(mutableListOf(list) as List<List<Any>>)
        val request: Sheets.Spreadsheets.Values.Append =
            sheetsService.spreadsheets().values().append(sheetsId, range, requestBody)
        request.valueInputOption = valueInputOption
        request.insertDataOption = insertDataOption
        request.execute()
    }

    private fun insertColumn(list: List<String>, range: String, sheetsId: String) {
        insert(list, range, "COLUMNS", sheetsId)
    }

    private fun insertRow(list: List<String>, range: String, sheetsId: String) {
        insert(list, range, "ROWS", sheetsId)
    }

    fun getAllEmotions(sheetsId: String): List<String> {
        val request = sheetsService.spreadsheets().values().get(sheetsId, emotionRange)
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
        System.err.println(list)
        return list
    }

    fun addClient(sheetsId: String, chatId: String) {
        insertRow(mutableListOf(chatId, sheetsId), dataRange, sheetsDataId)
    }

    fun updateMaps(
        mapIds: MutableMap<Long, String>,
        mapDaily: MutableMap<Long, DailyTaskExecutor>,
        callback: ReminderTask.Callback
    ) {
        val request = sheetsService.spreadsheets().values().get(sheetsDataId, dataRange)
        val response = request.execute()
        val values = response.getValues() ?: return
        for (row in values) {
            val chatId = row[0].toString().toLong()
            val sheetsIdValue = row[1].toString()
            mapIds[chatId] = sheetsIdValue
            if (row.size == 4 && row[2].toString().isNotEmpty() && row[3].toString().isNotEmpty()) {
                val dailyTask = DailyTaskExecutor(ReminderTask(callback))
                dailyTask.startExecutionAt(
                    row[2].toString().toInt(),
                    row[3].toString().toInt(),
                    0,
                    chatId
                )
                if (mapDaily.containsKey(chatId)) {
                    mapDaily[chatId]!!.stop()
                }
                mapDaily[chatId] = dailyTask
            } else if (mapDaily.containsKey(chatId)) {
                mapDaily[chatId]!!.stop()
                mapDaily.remove(chatId)
            }
        }
        System.err.println(mapIds)
        System.err.println(mapDaily)
    }

    fun setTime(chatId: String, sheetsId: String, hours: Int, minutes: Int) {
        insertRow(mutableListOf(chatId, sheetsId, hours.toString(), minutes.toString()), dataRange, sheetsDataId)
    }

    fun cancelReminder(chatId: String, sheetsId: String) {
        addClient(sheetsId, chatId)
    }
}