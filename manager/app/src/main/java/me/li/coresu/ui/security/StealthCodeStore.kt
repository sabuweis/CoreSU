package me.li.coresu.ui.security

import android.content.Context
import com.topjohnwu.superuser.ShellUtils
import me.li.coresu.data.repository.SettingsRepositoryImpl
import me.li.coresu.ui.util.getRootShell

object StealthCodeStore {
    private const val TAG = "coresu-stealth"
    private const val DIR = "/data/adb/csu"
    private const val PATH = "$DIR/stealth_code"
    private const val LEGACY_PATH = "/data/adb/ksu/stealth_code"
    const val DEFAULT_CODE = "70707"
    private const val KSU_CON = "u:object_r:ksu_file:s0"

    private fun sanitize(raw: String?): String? =
        raw?.trim()?.filter { it.isDigit() }?.take(12)?.takeIf { it.isNotBlank() }

    fun read(): String? = runCatching {
        sanitize(ShellUtils.fastCmd(getRootShell(), "cat $PATH 2>/dev/null"))
            ?: sanitize(ShellUtils.fastCmd(getRootShell(), "cat $LEGACY_PATH 2>/dev/null"))
                ?.also { write(it) }
    }.getOrNull()

    fun write(code: String): Boolean = runCatching {
        val c = sanitize(code) ?: return@runCatching false
        val tmp = "$PATH.new"
        val script = """
            mkdir -p $DIR
            printf '%s' '$c' > $tmp
            chmod 600 $tmp 2>/dev/null
            chown 0:0 $tmp 2>/dev/null
            chcon $KSU_CON $tmp 2>/dev/null || restorecon $tmp 2>/dev/null || true
            got=${'$'}(cat $tmp 2>/dev/null)
            mode=${'$'}(stat -c %a $tmp 2>/dev/null)
            owner=${'$'}(stat -c %u $tmp 2>/dev/null)
            if [ "${'$'}got" = '$c' ] && [ "${'$'}mode" = '600' ] && [ "${'$'}owner" = '0' ]; then
              mv -f $tmp $PATH
              chmod 600 $PATH 2>/dev/null
              chown 0:0 $PATH 2>/dev/null
              got2=${'$'}(cat $PATH 2>/dev/null)
              if [ "${'$'}got2" = '$c' ]; then echo "CORESU_OK mode=${'$'}mode owner=${'$'}owner"; else echo CORESU_FAIL_AFTER_MV; fi
            else
              rm -f $tmp
              echo "CORESU_FAIL mode=${'$'}mode owner=${'$'}owner"
            fi
        """.trimIndent()
        val out = ShellUtils.fastCmd(getRootShell(), script)
        android.util.Log.i(TAG, "密令落盘核验:${if (out.contains("CORESU_OK")) "OK" else "FAIL"}")
        out.contains("CORESU_OK")
    }.getOrDefault(false)

    fun effectiveCode(context: Context? = null): String {
        read()?.let { return it }
        val pref = runCatching { SettingsRepositoryImpl().stealthCode }.getOrNull()
        return sanitize(pref) ?: DEFAULT_CODE
    }

    fun acceptedCodes(context: Context? = null): Set<String> {
        val codes = LinkedHashSet<String>()
        read()?.let { codes.add(it) }
        runCatching { SettingsRepositoryImpl().stealthCode }.getOrNull()
            ?.let { sanitize(it) }?.let { codes.add(it) }
        return if (codes.isEmpty()) setOf(DEFAULT_CODE) else codes
    }

    fun sync(): Boolean {
        val repo = runCatching { SettingsRepositoryImpl() }.getOrNull() ?: return false
        val pref = sanitize(repo.stealthCode)
        val disk = read()
        return when {
            disk == null && pref != null -> { write(pref); false }
            disk != null && disk != pref -> { repo.stealthCode = disk; true }
            else -> false
        }
    }
}
