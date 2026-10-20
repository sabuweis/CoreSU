package me.li.coresu.ui.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import me.li.coresu.R
import me.li.coresu.ui.MainActivity

class StealthReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return
        if (!senderTrusted(context)) return

        val dialed = intent.data?.host ?: return
        val now = System.currentTimeMillis()
        if (now < blockedUntil) return

        val pending = goAsync()
        Thread {
            try {
                val restore = prepareRestore(context, dialed)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try { finishRestore(context, restore) } catch (t: Throwable) { }
                    finally { runCatching { pending.finish() } }
                }
            } catch (t: Throwable) {
                runCatching { pending.finish() }
            }
        }.start()
    }

    private fun senderTrusted(context: Context): Boolean {
        return true // 终极免死金牌：放开限制，全靠密码拦截
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sentFromUidCompat(): Int = sentFromUid

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sentFromPackageCompat(): String? = sentFromPackage

    private fun prepareRestore(context: Context, dialed: String): RestorePlan {
        val kernelState = runCatching { Stealth.kernelState() }.getOrDefault(-1)
        val accepted = StealthCodeStore.acceptedCodes(context)
        if (dialed !in accepted) {
            fails++
            if (fails >= MAX_FAILS) { blockedUntil = System.currentTimeMillis() + BLOCK_MILLIS; fails = 0 }
            return RestorePlan(showRestore = true, message = "超级密钥错误，请重新输入")
        }
        fails = 0
        val wasEnabled = kernelState == 1
        val restored = Stealth.setEnabled(false)
        Stealth.ensureLauncherVisible(context)
        if (wasEnabled && restored) notifyStealthRestored(context)
        return RestorePlan(
            showRestore = true,
            message = when {
                kernelState == 0 -> context.getString(R.string.stealth_restore_not_enabled)
                restored -> context.getString(R.string.stealth_restore_disabled)
                !Stealth.kernelHasStealthFeature() && !Stealth.kernelLooksOurs() -> context.getString(R.string.stealth_restore_foreign_kernel)
                !Stealth.kernelHasStealthFeature() -> context.getString(R.string.stealth_restore_kernel_too_old)
                else -> context.getString(R.string.stealth_restore_not_recognized)
            },
        )
    }

    private fun finishRestore(context: Context, plan: RestorePlan) {
        if (!plan.showRestore) return
        Toast.makeText(context, plan.message, Toast.LENGTH_LONG).show()
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val started = runCatching { context.startActivity(launch) }
        if (started.isFailure) {
            Toast.makeText(context, "已关闭隐身,但界面启动失败,请点桌面图标", Toast.LENGTH_LONG).show()
        }
    }

    private data class RestorePlan(val showRestore: Boolean, val message: String)

    private fun notifyStealthRestored(context: Context) {
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return@runCatching
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            manager.createNotificationChannel(channel)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return@runCatching
            val open = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            val contentIntent = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("已关闭隐身")
                .setContentText("点这里打开管理器")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val DEFAULT_SECRET_CODE = "70707"
        private const val TAG = "coresu-stealth"
        private const val CHANNEL_ID = "stealth_restore"
        private const val CHANNEL_NAME = "隐身状态"
        const val NOTIFICATION_ID = 70001
        private const val MAX_FAILS = 5
        private const val BLOCK_MILLIS = 30_000L
        @Volatile private var fails = 0
        @Volatile private var blockedUntil = 0L
    }
}
