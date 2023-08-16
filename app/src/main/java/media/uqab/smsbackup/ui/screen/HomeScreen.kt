package media.uqab.smsbackup.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import media.uqab.libsmsbackup.SmsReader
import media.uqab.libsmsbackup.SmsWriter
import media.uqab.smsbackup.ui.navigatior.LocalNavigator
import media.uqab.smsbackup.ui.navigatior.Screen

class HomeScreen : Screen {
    companion object {
        private const val TAG = "HomeScreen"
    }

    override val name: String
        get() = javaClass.simpleName

    @Composable
    override fun Content() {
        HomeScreenContent()
    }

    @Composable
    private fun HomeScreenContent() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.current
        var totalSms by remember { mutableStateOf(0) }
        val totalSmsAnim by animateIntAsState(targetValue = totalSms, label = "")
        var runningProgress by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        var totalItems by remember { mutableStateOf(0) }
        var currentItem by remember { mutableStateOf(0) }

        val restoreUriLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        runningProgress = true

                        val success = restoreBackup(
                            context, uri,
                            onProgress = { i, s ->
                                totalSms = i
                                totalItems = s
                                currentItem = i
                            }
                        )

                        if (success) {
                            Toast.makeText(context, "Restore Done", Toast.LENGTH_SHORT).show()
                            totalSms = getTotalSmsCount(context)
                        } else {
                            Toast.makeText(context, "Failed to restore!", Toast.LENGTH_SHORT).show()
                        }

                        runningProgress = false
                    }
                }
            }
        )

        val defaultSmsToRestore = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                if (it.resultCode == Activity.RESULT_OK) {
                    restoreUriLauncher.launch(arrayOf("*/*"))
                }
            }
        )

        GetSmsReadPermission {
            coroutineScope.launch {
                totalSms = getTotalSmsCount(context)
            }
        }

        DeleteSmsDialog(show = showDeleteDialog) {
            showDeleteDialog = false
            coroutineScope.launch {
                totalSms = getTotalSmsCount(context)
            }
        }

        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    12.dp,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Text(text = "Total SMS", style = MaterialTheme.typography.titleSmall)
                Text(text = totalSmsAnim.toString(), style = MaterialTheme.typography.displaySmall)

                ElevatedButton(
                    onClick = {
                        navigator?.navigate(BackupScreen())
                    },
                ) {
                    Text(text = "Take Backup")
                }

                ElevatedButton(
                    onClick = {
                        defaultSmsToRestore.launch(SmsWriter().getSmsAppChangingIntent(context))
                    },
                ) {
                    Text(text = "Restore Backup")
                }

                Spacer(modifier = Modifier.height(20.dp))

                ElevatedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.Red,
                        contentColor = MaterialTheme.colorScheme.contentColorFor(Color.Red)
                    )
                ) {
                    Text(text = "Delete All SMS")
                }

                AnimatedVisibility(visible = runningProgress) {
                    ProgressBar(
                        progress = currentItem.toFloat() / totalItems.coerceAtLeast(1),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun GetSmsReadPermission(onGrant: () -> Unit) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) onGrant()
            }
        )

        LaunchedEffect(key1 = Unit) {
            launcher.launch(Manifest.permission.READ_SMS)
        }
    }

    @Composable
    private fun DeleteSmsDialog(show: Boolean, onDismiss: () -> Unit) {
        val context = LocalContext.current
        val coroutine = rememberCoroutineScope()
        var deleting by remember { mutableStateOf(false) }

        val defaultSmsToDeleteLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                if (it.resultCode == Activity.RESULT_OK) {
                    coroutine.launch {
                        deleting = true
                        deleteAllSms(context)

                        Toast.makeText(context, "All SMS deleted", Toast.LENGTH_SHORT).show()
                        deleting = false
                        onDismiss()
                    }
                }
            }
        )

        if (!show) return
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "WARNING", color = Color.Red)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "THIS FUNCTION IS ONLY FOR TESTING PURPOSE",
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "THERE IS NO UNDO. ONCE GONE IS GONE FOREVER",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )

                        AnimatedVisibility(visible = deleting) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        }
                    }

                    Button(
                        onClick = {
                            defaultSmsToDeleteLauncher.launch(
                                SmsWriter().getSmsAppChangingIntent(context)
                            )
                        },
                    ) {
                        Text(text = "Confirm and Delete")
                    }
                }
            }
        }
    }

    @Composable
    private fun ProgressBar(progress: Float, modifier: Modifier) {
        var showIndeterminate by remember { mutableStateOf(false) }

        LaunchedEffect(key1 = Unit) {
            while (isActive) {
                showIndeterminate = !showIndeterminate
                delay(if (showIndeterminate) 2000 else 5000)
            }
        }

        AnimatedVisibility(visible = showIndeterminate, enter = fadeIn(), exit = fadeOut()) {
            CircularProgressIndicator(modifier = modifier, color = MaterialTheme.colorScheme.primaryContainer)
        }

        CircularProgressIndicator(modifier = modifier, progress = progress)
    }

    private suspend fun getTotalSmsCount(context: Context): Int {
        return withContext(Dispatchers.IO) {
            val smsReader = SmsReader()
            smsReader.readAllSmsFromInbox(context).size
        }
    }

    private suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        onProgress: (Int, Int) -> Unit
    ): Boolean {
        withContext(Dispatchers.IO) {
            val reader = SmsReader()
            val smsList = reader.readFromBackup(context, uri)
            Log.d(TAG, "restoreBackup: ${smsList.size}")

            val writer = SmsWriter()
            writer.writeSmsToInbox(context, smsList, onProgress)
        }
        return true
    }

    private suspend fun deleteAllSms(context: Context) {
        withContext(Dispatchers.IO) {
            context.contentResolver.delete(Telephony.Sms.CONTENT_URI, null, null)
        }
    }
}