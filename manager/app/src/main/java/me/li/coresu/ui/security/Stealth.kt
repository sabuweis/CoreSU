package me.li.coresu.ui.security

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import me.li.coresu.Natives

object Stealth {
    private const val LAUNCHER_ALIAS = "me.li.coresu.ui.LauncherAlias"
    private const val UAPI_WITH_STEALTH = 5

    const val STEALTH_FLAG_PATH = "/data/adb/csu/stealth"

    fun kernelState(): Int = runCatching { Natives.stealthState() }.getOrDefault(-1)

    enum class State { ON, OFF, UNKNOWN }

    fun state(): State = when (kernelState()) {
        1 -> State.ON
        0 -> State.OFF
        else -> State.UNKNOWN
    }

    fun looksStealthy(): Boolean = state() != State.OFF

    fun isEnabled(): Boolean = kernelState() == 1

    fun kernelHasStealthFeature(): Boolean =
        runCatching { Natives.kernelUAPIVersion >= UAPI_WITH_STEALTH }.getOrDefault(false)

    fun kernelLooksOurs(): Boolean = runCatching { Natives.isManager }.getOrDefault(false)

    fun setEnabled(enabled: Boolean): Boolean =
        runCatching { Natives.stealthSet(enabled) }.getOrDefault(false)

    fun setEnabledReporting(enabled: Boolean): String? {
        if (setEnabled(enabled)) return null
        val action = if (enabled) "开启" else "关闭"
        return when {
            kernelHasStealthFeature() -> "${action}隐身失败:本 App 现在没被内核认主。请重启设备,或卸载另一个管理器后再试"
            kernelLooksOurs() -> "内核太旧,不支持隐身模式"
            else -> "内核不是 CoreSU(当前 root 由别的方案提供):请重启设备,或卸载另一个 root 管理器后再试"
        }
    }

    fun ensureLauncherVisible(context: Context) {
        Thread {
            runCatching {
                val pm = context.packageManager
                val component = ComponentName(context.packageName, LAUNCHER_ALIAS)
                if (pm.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                }
            }
        }.start()
    }
}

fun restartUiFresh(context: Context) {
    val activity = context.findActivity()
    (activity as? androidx.lifecycle.ViewModelStoreOwner)?.let {
        runCatching { it.viewModelStore.clear() }
    }
    activity?.recreate()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
