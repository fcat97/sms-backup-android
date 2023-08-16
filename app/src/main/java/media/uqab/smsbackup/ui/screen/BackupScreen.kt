package media.uqab.smsbackup.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import media.uqab.libsmsbackup.Sms
import media.uqab.libsmsbackup.SmsReader
import media.uqab.libsmsbackup.SmsWriter
import media.uqab.smsbackup.R
import media.uqab.smsbackup.ui.navigatior.LocalNavigator
import media.uqab.smsbackup.ui.navigatior.Screen
import media.uqab.smsbackup.ui.navigatior.ScreenNavigator.Companion.setBackPressDispatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupScreen : Screen {
    override val name: String
        get() = javaClass.simpleName

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        LocalNavigator.current?.let {
            setBackPressDispatcher(it)
        }

        var runningProgress by remember { mutableStateOf(false) }
        val allThreads = remember { mutableStateListOf<String>() }
        val selectedThread = remember { mutableStateListOf<String>() }
        val totalMap = remember { HashMap<String, Int>() }

        LaunchedEffect(key1 = Unit) {
            val reader = SmsReader()
            reader.readAllSmsFromInbox(context).groupBy { it.address }.forEach { (f, l) ->
                allThreads.add(f)
                selectedThread.add(f)
                totalMap[f] = l.size
            }
        }

        val backupUriLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/json"),
            onResult = { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        runningProgress = true
                        if (takeBackup(context, uri, selectedThread)) {
                            Toast.makeText(context, "Backup Done", Toast.LENGTH_SHORT).show()
                            shareFile(context, uri)
                        } else {
                            Toast.makeText(context, "Failed to backup!", Toast.LENGTH_SHORT).show()
                        }
                        runningProgress = false
                    }
                }
            },
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Select Threads") },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedThread.size == allThreads.size) {
                                    selectedThread.clear()
                                } else {
                                    selectedThread.clear()
                                    selectedThread.addAll(allThreads)
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (selectedThread.size == allThreads.size) {
                                        R.drawable.baseline_deselect_24
                                    } else {
                                        R.drawable.baseline_select_all_24
                                    }
                                ),
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH)
                        val date = sdf.format(Date())
                        backupUriLauncher.launch("sms-backup-$date.backup")
                    },
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                }
            }
        ) { pad ->
            Column(modifier = Modifier.padding(pad)) {
                LazyColumn {
                    items(items = allThreads) {
                        ItemThread(
                            threadName = it,
                            totalSms = totalMap[it] ?: 0,
                            isSelected = selectedThread.contains(it),
                            onSelect = { t ->
                                if (selectedThread.contains(t)) selectedThread.remove(t)
                                else selectedThread.add(t)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemThread(
        threadName: String,
        totalSms: Int,
        isSelected: Boolean,
        onSelect: (String) -> Unit
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(threadName) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = threadName, style = MaterialTheme.typography.titleMedium)
                    Text(text = "total sms: $totalSms")
                }

                RadioButton(selected = isSelected, onClick = { onSelect(threadName) })
            }

            Divider()
        }

    }

    private suspend fun takeBackup(
        context: Context,
        uri: Uri,
        selectedThread: List<String>
    ): Boolean {
        val threads = selectedThread.joinToString { "'$it'" }
        val selection = "${Telephony.Sms.ADDRESS} IN ($threads)"
        val smsList = SmsReader().readAllSmsFromInbox(context, selection = selection)
        return SmsWriter().createBackup(context, smsList, uri)
    }

    private fun shareFile(context: Context, uri: Uri) {
        context.startActivity(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "*/*"
            }
        )
    }
}