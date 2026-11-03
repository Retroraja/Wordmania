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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // The app (Compose) and the 3D layer share one frame. The 3D layer sits on top for effects,
        // or underneath for full-screen scenes (town, intro, wheel...) shown through see-through screens.
        val root = FrameLayout(this)
        val compose = ComposeView(this).apply { setContent { WordnoidApp() } }
        val web = FxWebView(this)
        root.addView(compose, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(web, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)
        runCatching { Fx.attach(web) }
        lifecycleScope.launch {
            snapshotFlow { Fx.front }.collect { onTop ->
                web.z = if (onTop) 1f else -1f
                compose.z = 0f
            }
        }
    }

    override fun onDestroy() {
        Fx.detach()
        super.onDestroy()
    }
}
