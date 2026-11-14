package com.retroraja.wordnoid

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.retroraja.wordnoid.ui.Fx
import com.retroraja.wordnoid.ui.FxWebView
import com.retroraja.wordnoid.ui.WordnoidApp
import kotlinx.coroutines.launch
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {
    // Update 1.3: when a new version is on the Play Store, Google's own "Update available" window shows
    // (Update / No thanks). It downloads in the background and installs the next time the game opens.
    private val updates by lazy { AppUpdateManagerFactory.create(this) }
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    private val updateListener = InstallStateUpdatedListener { st ->
        if (st.installStatus() == InstallStatus.DOWNLOADED) {
            android.widget.Toast.makeText(this, "Wordnoid update ready! It installs next time you open the game.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun checkForUpdate() = runCatching {
        updates.registerListener(updateListener)
        updates.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> updates.completeUpdate()
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                    runCatching { updates.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // The app (Compose) and the 3D layer share one frame. The 3D layer sits on top for effects,
        // or underneath for full-screen scenes (town, intro, wheel...) shown through see-through screens.
        val root = FrameLayout(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        val compose = ComposeView(this).apply { setContent { WordnoidApp() } }
        val web = FxWebView(this)
        root.addView(compose, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(web, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)
        runCatching { Fx.attach(web) }
        checkForUpdate()
        lifecycleScope.launch {
            snapshotFlow { Fx.front }.collect { onTop ->
                web.z = if (onTop) 1f else -1f
                compose.z = 0f
            }
        }
    }

    override fun onDestroy() {
        runCatching { updates.unregisterListener(updateListener) }
        Fx.detach()
        super.onDestroy()
    }
}
