package media.uqab.libsmsbackup

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.libsmsbackup.Keys.DATA
import media.uqab.libsmsbackup.Sms.Companion.toSms
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class SmsReader {
    companion object {
        private const val TAG = "SmsReader"
    }

    fun readAllSmsFromInbox(
        context: Context,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): List<Sms> {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.CREATOR,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.READ,
            Telephony.Sms.SEEN,
            Telephony.Sms.TYPE,
        )

        val smsList = mutableListOf<Sms>()

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val sms = Sms()
                for (i in 0..projection.lastIndex) {
                    val data = when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getFloat(i).toString()
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                        Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i).toString(Charsets.UTF_8)
                        else -> null
                    } ?: continue

                    print("{projection[i].padEnd(15)}: $data")

                    when (projection[i]) {
                        Telephony.Sms.ADDRESS -> sms.address = data
                        Telephony.Sms.BODY -> sms.body = data
                        Telephony.Sms.DATE -> sms.date = data.toLong()
                        Telephony.Sms.DATE_SENT -> sms.dateSent = data.toLong()
                        Telephony.Sms.READ -> sms.read = data.toInt()
                        Telephony.Sms.SEEN -> sms.seen = data.toInt()
                        Telephony.Sms.TYPE -> {
                            sms.type = when (data.toInt()) {
                                Telephony.Sms.MESSAGE_TYPE_INBOX -> Sms.Type.INBOX
                                Telephony.Sms.MESSAGE_TYPE_SENT -> Sms.Type.SENT
                                Telephony.Sms.MESSAGE_TYPE_DRAFT -> Sms.Type.DRAFT
                                Telephony.Sms.MESSAGE_TYPE_OUTBOX -> Sms.Type.OUTBOX
                                Telephony.Sms.MESSAGE_TYPE_FAILED -> Sms.Type.FAILED
                                Telephony.Sms.MESSAGE_TYPE_QUEUED -> Sms.Type.QUEUED
                                else -> Sms.Type.NONE
                            }
                        }
                    }
                }
                smsList.add(sms)

                print("---".repeat(20))
            }
        }

        return smsList
    }

    suspend fun readFromBackup(context: Context, uri: Uri): List<Sms> {
        val smsList = mutableListOf<Sms>()

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                val inputAsString = ins.bufferedReader().use { it.readText() }
                val backup = JSONObject(inputAsString)
                val smsArr = backup.optJSONArray(DATA) ?: return@use

                for (i in 0 until smsArr.length()) {
                    safe {
                        smsArr.getJSONObject(i)
                            .toSms()
                            ?.let {
                                smsList.add(it)
                            }
                    }
                }
            }
        }

        return smsList
    }

    private fun print(msg: String) {
        // Log.d(TAG, "testSms: $msg")
    }
}