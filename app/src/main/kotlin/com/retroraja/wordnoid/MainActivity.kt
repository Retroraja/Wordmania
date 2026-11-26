package com.retroraja.wordnoid

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.retroraja.wordnoid.ui.Fx
import com.retroraja.wordnoid.ui.FxWebView
import com.retroraja.wordnoid.ui.WordnoidApp
import kotlinx.coroutines.launch
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

    /** Full screen: the status and navigation bars stay hidden (a swipe shows them for a moment). */
    private fun hideBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Ads and system windows can bring the bars back: hide them again.
        if (hasFocus) hideBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 (SDK 35) edge-to-edge: the game draws behind the bars and the camera cutout,
        // and keeps its buttons clear of them with the insets padding below.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.retroraja.wordnoid.data.Analytics.start(this)
        hideBars()
        // The app (Compose) and the 3D layer share one frame. The 3D layer sits on top for effects,
        // or underneath for full-screen scenes (town, intro, wheel...) shown through see-through screens.
        val root = FrameLayout(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        val compose = ComposeView(this).apply { setContent { WordnoidApp() } }
        // Some phones have no working WebView (missing, disabled or updating): then the game runs without the 3D layer.
        val web = runCatching { FxWebView(this) }.getOrNull()
        root.addView(compose, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        if (web != null) root.addView(web, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        // Keep the game out of the camera cutout and any visible system bar (hidden bars give 0, so it stays full screen).
        // The black frame fills the area behind them.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(i.left, i.top, i.right, i.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)
        if (web != null) runCatching { Fx.attach(web) }
        checkForUpdate()
        lifecycleScope.launch {
            snapshotFlow { Fx.front }.collect { onTop ->
                web?.z = if (onTop) 1f else -1f
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
