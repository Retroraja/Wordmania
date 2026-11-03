package com.retroraja.wordnoid.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.retroraja.wordnoid.audio.SoundFx
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/** The 3D layer: a see-through WebView that draws three.js scenes and effects, and never takes touches. */
@SuppressLint("ViewConstructor")
class FxWebView(context: Context) : WebView(context) {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = false
}

/**
 * Talks to the 3D layer. Positions are screen pixels (as Compose reports them) and are sent as dp.
 * Calls made before the page is ready wait in a queue. If the phone has no WebGL, [ok] stays false and
 * the game keeps its flat 2D effects.
 */
object Fx {
    private var web: WebView? = null
    private val main = Handler(Looper.getMainLooper())
    private var loaded = false
    private val queue = ArrayList<String>()
    private val waiting = HashMap<String, MutableList<CompletableDeferred<String>>>()
    var density = 1f
    /** Screen size in pixels. */
    val screenW: Float get() = (web?.width ?: 1000).toFloat()
    val screenH: Float get() = (web?.height ?: 600).toFloat()
    var sounds: SoundFx? = null

    /** True when the 3D layer works on this phone. */
    var ok by mutableStateOf(false); private set
    /** 3D booster icons, made once by the page. */
    val icons = mutableStateMapOf<String, ImageBitmap>()
    /** The 3D layer is drawn over the app (effects) or under it (scenes behind see-through screens). */
    var front by mutableStateOf(true); private set
    /** Name of the scene behind the screen, or null. */
    var scene by mutableStateOf<String?>(null); private set

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(v: WebView) {
        web = v
        density = v.resources.displayMetrics.density
        v.setBackgroundColor(AColor.TRANSPARENT)
        v.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        v.isFocusable = false
        v.isClickable = false
        v.isVerticalScrollBarEnabled = false
        v.isHorizontalScrollBarEnabled = false
        v.overScrollMode = WebView.OVER_SCROLL_NEVER
        v.settings.javaScriptEnabled = true
        v.settings.allowFileAccess = true
        v.settings.mediaPlaybackRequiresUserGesture = true
        v.webViewClient = WebViewClient()
        v.addJavascriptInterface(Bridge, "WN")
        v.loadUrl("file:///android_asset/fx/fx.html")
    }

    fun detach() { web?.destroy(); web = null; loaded = false; ok = false; scene = null; front = true }

    private object Bridge {
        @JavascriptInterface fun sfx(name: String) { sounds?.play(name) }
        @JavascriptInterface fun ready(gl: Boolean) {
            main.post {
                loaded = true
                ok = gl
                val q = ArrayList(queue); queue.clear()
                if (gl) {
                    q.forEach { web?.evaluateJavascript(it, null) }
                    call("icons", JSONObject().put("size", 128))
                }
            }
        }
        @JavascriptInterface fun event(name: String, data: String) {
            main.post { waiting.remove(name)?.forEach { it.complete(data) } }
        }
        @JavascriptInterface fun icon(key: String, dataUrl: String) {
            val b64 = dataUrl.substringAfter(",")
            runCatching {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                main.post { icons[key] = bmp.asImageBitmap() }
            }
        }
    }

    /** Calls FX.[fn](data) in the page. */
    fun call(fn: String, data: JSONObject = JSONObject()) {
        val js = "window.FX&&FX.$fn(${JSONObject.quote(data.toString())})"
        main.post {
            if (!loaded) { queue.add(js); return@post }
            if (ok) web?.evaluateJavascript(js, null)
        }
    }

    /** Waits for an event from the page (for example "goalBlastDone"), at most [ms]. */
    suspend fun await(name: String, ms: Long): String? {
        if (!ok) return null
        val d = CompletableDeferred<String>()
        waiting.getOrPut(name) { mutableListOf() }.add(d)
        return withTimeoutOrNull(ms) { d.await() }
    }

    /** Shows a full-screen scene behind the app (the app screen must be see-through), or none. */
    fun backdrop(name: String?, data: JSONObject = JSONObject()) {
        scene = name
        front = name == null
        call("backdrop", JSONObject().put("name", name ?: JSONObject.NULL).put("data", data))
    }

    fun act(name: String, data: JSONObject = JSONObject()) = call("act", JSONObject().put("name", name).put("data", data))

    // ---- helpers to build the JSON
    fun dp(px: Float): Double = (px / density).toDouble()
    fun at(o: Offset, obj: JSONObject = JSONObject()): JSONObject = obj.put("x", dp(o.x)).put("y", dp(o.y))
    fun xy(o: Offset): JSONArray = JSONArray().put(dp(o.x)).put(dp(o.y))
    fun obj(vararg kv: Pair<String, Any?>): JSONObject = JSONObject().apply { kv.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } }
    fun hex(c: androidx.compose.ui.graphics.Color): String =
        String.format("#%02x%02x%02x", (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
}
