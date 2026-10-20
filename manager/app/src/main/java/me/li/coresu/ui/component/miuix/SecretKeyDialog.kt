package me.li.coresu.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SecretKeyDialog(
    show: Boolean,
    currentCode: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    OverlayDialog(
        show = show,
        title = "设置超级密钥",
        summary = "请输入 4~12 位纯数字",
        onDismissRequest = onDismissRequest,
        content = {
            var text by remember(show) { mutableStateOf(currentCode) }
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                value = text,
                maxLines = 1,
                onValueChange = { newValue ->
                    if (newValue.length <= 12 && newValue.all { it.isDigit() }) {
                        text = newValue
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        if (text.length >= 4) {
                            onConfirm(text)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    )
}
