package media.uqab.libsmsbackup

import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.libsmsbackup.Keys.DATA
import media.uqab.libsmsbackup.Keys.META
import media.uqab.libsmsbackup.Keys.META_CURRENT_VERSION
import media.uqab.libsmsbackup.Keys.META_DATE
import media.uqab.libsmsbackup.Keys.META_TOTAL
import media.uqab.libsmsbackup.Keys.META_VERSION
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.function.BiConsumer

class SmsWriter {
    companion object {
        private const val TAG = "SmsWriter"
    }

    /**
     * Get [Intent] to show change default Sms app dialog
     */
    fun getSmsAppChangingIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    private fun isDefaultSmsApp(context: Context): Boolean {
        return context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
    }

    /**
     * Write sms into inbox.
     *
     * ---
     * __N.B.__ writing Sms requires the package to be default
     * SMS application. To request to be default app for sms use
     * intent from [getSmsAppChangingIntent].
     *
     * @param context Android's [Context]
     * @param smsList List of [Sms] to restore.
     * @param onProgress lambda to notify progress as (progress, total).
     *
     * @return `false` if any sms failed to write
     */
    fun writeSmsToInbox(
        context: Context,
        smsList: List<Sms>,
        onProgress: (Int, Int) -> Unit
    ): Boolean {
        if (!isDefaultSmsApp(context)) return false
        if (smsList.isEmpty()) return true

        val cr = context.contentResolver
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.TYPE
        )

        var current = 0
        return smsList.all { sms ->
            safe {
                onProgress(++current, smsList.size)

                // check if the value already exists or not
                val selection = "address='${sms.address}' AND date='${sms.date}' AND date_sent='${sms.dateSent}' and type=${sms.type.value}"
                val cursor = cr.query(Telephony.Sms.CONTENT_URI, projection, selection, null, null)
                val count = cursor?.count ?: 0
                cursor?.close()
                Log.d(TAG, "writeSmsToInbox: inserting: $count $sms")
                if(count >= 1) return@all true

                val values = ContentValues()
                values.put(Telephony.Sms.ADDRESS, sms.address)
                values.put(Telephony.Sms.BODY, sms.body)
                values.put(Telephony.Sms.DATE, sms.date)
                values.put(Telephony.Sms.DATE_SENT, sms.dateSent)
                values.put(Telephony.Sms.READ, sms.read)
                values.put(Telephony.Sms.STATUS, sms.seen)
                values.put(Telephony.Sms.TYPE, sms.type.value)
                // context.contentResolver.insert(Uri.parse("content://sms/inbox"), values)
                cr.insert(Uri.parse("content://sms/"), values)
            } != null
        }
    }

    /**
     * Write sms back in sd card
     *
     * @param sms List of [Sms] to create backup
     * @param uri [Uri] of the output file.
     * @return `true` if written successful, `false` otherwise.
     */
    suspend fun createBackup(context: Context, sms: List<Sms>, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            safe {
                val meta = JSONObject().apply {
                    put(META_DATE, Date())
                    put(META_VERSION, META_CURRENT_VERSION)
                    put(META_TOTAL, sms.size)
                }

                val smsArr = JSONArray()
                sms.forEach { smsArr.put(it.toJson()) }

                val backup = JSONObject().apply {
                    put(META, meta)
                    put(DATA, smsArr)
                }.toString(2)

                // println(backup)

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(backup.toByteArray(Charsets.UTF_8))
                    true
                } ?: false
            } ?: false
        }
    }
}