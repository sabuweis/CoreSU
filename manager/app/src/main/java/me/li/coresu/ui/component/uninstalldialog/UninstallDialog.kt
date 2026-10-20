package me.li.coresu.ui.component.uninstalldialog

import androidx.compose.runtime.Composable
import me.li.coresu.ui.LocalUiMode
import me.li.coresu.ui.UiMode

@Composable
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> UninstallDialogMiuix(show, onDismissRequest)
        UiMode.Material -> UninstallDialogMaterial(show, onDismissRequest)
    }
}
