package com.appgrabberlink.engine

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile

object RootHelper {

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
    }

    val isRootAvailable: Boolean by lazy {
        try {
            Shell.cmd("id").exec().isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun execCommand(command: String): Shell.Result {
        return Shell.cmd(command).exec()
    }

    fun execCommandSilent(command: String): Boolean {
        return Shell.cmd(command).exec().isSuccess
    }

    fun installCertificate(certBytes: ByteArray): Boolean {
        if (!isRootAvailable) return false
        val certDir = SuFile.open("/data/local/tmp")
        val certFile = SuFile.open("/data/local/tmp/appgrabber-ca.crt")
        certFile.writeBytes(certBytes)

        val commands = listOf(
            "mount -o rw,remount /system",
            "cp /data/local/tmp/appgrabber-ca.crt /system/etc/security/cacerts/",
            "chmod 644 /system/etc/security/cacerts/appgrabber-ca.crt",
            "mount -o ro,remount /system",
            "rm /data/local/tmp/appgrabber-ca.crt"
        )
        return commands.all { Shell.cmd(it).exec().isSuccess }
    }

    fun startIptablesCapture(): Boolean {
        if (!isRootAvailable) return false
        val commands = listOf(
            "iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to-port 8080",
            "iptables -t nat -A OUTPUT -p tcp --dport 443 -j REDIRECT --to-port 8443"
        )
        return commands.all { Shell.cmd(it).exec().isSuccess }
    }

    fun stopIptablesCapture(): Boolean {
        if (!isRootAvailable) return false
        val commands = listOf(
            "iptables -t nat -D OUTPUT -p tcp --dport 80 -j REDIRECT --to-port 8080",
            "iptables -t nat -D OUTPUT -p tcp --dport 443 -j REDIRECT --to-port 8443"
        )
        return commands.all { Shell.cmd(it).exec().isSuccess }
    }

    fun getAppDataPath(packageName: String): String? {
        if (!isRootAvailable) return null
        val result = Shell.cmd("su -c cat /data/data/$packageName/shared_prefs/*.xml").exec()
        return if (result.isSuccess) result.out.firstOrNull() else null
    }
}
