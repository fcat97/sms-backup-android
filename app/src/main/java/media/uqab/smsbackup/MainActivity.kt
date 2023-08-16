package media.uqab.smsbackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import media.uqab.smsbackup.ui.screen.HomeScreen
import media.uqab.smsbackup.ui.navigatior.ScreenNavigator
import media.uqab.smsbackup.ui.theme.SmsBackupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsBackupTheme {
                remember {
                    ScreenNavigator()
                }.StartFrom {
                    HomeScreen()
                }
            }
        }
    }


}


