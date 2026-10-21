package me.li.coresu.ui.screen.settings.tools

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selinuxStatus by remember { mutableStateOf("Unknown") }
    
    LaunchedEffect(Unit) {
        kotlin.concurrent.thread {
            val result = Shell.cmd("getenforce").exec()
            val status = result.out.firstOrNull()?.trim() ?: "Unknown"
            android.os.Handler(android.os.Looper.getMainLooper()).post { selinuxStatus = status }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("高级工具") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("SELinux 模式") },
                    supportingContent = { Text("当前: $selinuxStatus") },
                    leadingContent = { Icon(Icons.Rounded.Security, null) },
                    trailingContent = {
                        TextButton(onClick = {
                            kotlin.concurrent.thread {
                                val target = if (selinuxStatus == "Enforcing") "0" else "1"
                                Shell.cmd("setenforce $target").exec()
                                val newStatus = Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown"
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    selinuxStatus = newStatus
                                    Toast.makeText(context, "已切换为 $newStatus", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) { Text(if (selinuxStatus == "Enforcing") "设为宽容" else "设为严格") }
                    }
                )
            }
        }
    }
}
