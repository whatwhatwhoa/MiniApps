package io.github.whatwhatwhoa.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/**
 * The installer's answers to [Updates]' session. First it asks for the user's OK: its dialog is
 * started from here while the app is on screen (the user just tapped Update). On success Android
 * replaces the app and closes it, so nothing is left to do.
 */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // The typed getter misbehaves on Android 13; use it from 14.
                val confirm = if (Build.VERSION.SDK_INT >= 34) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirm == null) Updates.installFailed("The installer didn't ask to go on.")
                else context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> {}
            PackageInstaller.STATUS_FAILURE_ABORTED -> Updates.installCancelled()
            else -> {
                val why = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Updates.installFailed(
                    when (status) {
                        PackageInstaller.STATUS_FAILURE_CONFLICT -> "Android refused: the update conflicts with this copy of the app."
                        PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough free space for the update."
                        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "This device can't run the update."
                        else -> "The install failed" + (why?.let { ": $it" } ?: ".")
                    }
                )
            }
        }
    }
}
