package media.uqab.smsbackup.ui.navigatior

import androidx.compose.runtime.Composable

/**
 * All screen should implement this interface
 * in order to use the navigator
 */
interface Screen {
    val name: String

    @Composable
    fun Content()
}