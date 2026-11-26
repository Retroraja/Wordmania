package com.retroraja.wordnoid.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.retroraja.wordnoid.data.Analytics
import com.retroraja.wordnoid.data.Progress

/**
 * All ads. Real AdMob units in the Play bundle (release), Google's TEST units in the test APK (debug).
 * Rules for the first live release (decided 2026-11-22):
 *  - automatic (interstitial) ads ONLY between levels after a win, and after losing or quitting a level;
 *    never in levels 1-3, and at least 3 minutes apart;
 *  - rewarded videos only when the player taps: free coins in the shop, +5 moves when out of moves;
 *  - no ads during play, tutorials or mini games; no banners; no app-open ads;
 *  - under-13 players get child-safe, non-personalised ads. UMP consent form first.
 * Without internet the game works normally; Watch buttons just say "Needs internet".
 */
object Ads {
    // Real AdMob units in the Play bundle; Google's test units in the test APK (safe to tap).
    private val REAL = com.retroraja.wordnoid.BuildConfig.REAL_ADS
    private val REWARDED = if (REAL) "ca-app-pub-${com.retroraja.wordnoid.BuildConfig.ADMOB_PUB}/7049119807" else "ca-app-pub-3940256099942544/5224354917"
    private val INTERSTITIAL = if (REAL) "ca-app-pub-${com.retroraja.wordnoid.BuildConfig.ADMOB_PUB}/5345669244" else "ca-app-pub-3940256099942544/1033173712"
    /** Automatic ads: never before level 4, and at least this long apart. */
    const val FIRST_AUTO_LEVEL = 4
    const val AUTO_GAP_MS = 3 * 60_000L

    /** The 13 rewarded spots, with how many times a day each may be used. */
    enum class Spot(val perDay: Int) {
        CONTINUE(5), EXTRA_SPIN(1), SKIP_CHALLENGE(1), DOUBLE_WIN_COINS(5), EXTRA_BRICK(3), FREE_HAMMER(3), STUCK_HINT(5),
        DOUBLE_DAILY(1), SECOND_CRATE(3), QUIZ_RETRY(3), GIFT_BALLOON(2), FREE_COINS(5), BOSS_REMATCH(3), KEEP_STREAK(1),
        /** Update 1.1 mini games: one extra play a day, a few extra pencil hints, hearts back once. */
        MINI_PLAY(1), MINI_HINT(6), MINI_HEARTS(2),
    }

    private var started = false
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    var rewardedReady by mutableStateOf(false); private set
    private lateinit var progress: Progress

    fun start(activity: Activity, progress: Progress) {
        this.progress = progress
        if (started) return
        started = true
        // Consent form (UMP) first, then the ads SDK.
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(progress.isChild).build()
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initSdk(activity) }
        }, { initSdk(activity) })
    }

    private fun initSdk(activity: Activity) {
        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(if (progress.isChild) RequestConfiguration.MAX_AD_CONTENT_RATING_G else RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
            .setTagForChildDirectedTreatment(if (progress.isChild) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(activity) {
            loadRewarded(activity)
            loadInterstitial(activity)
        }
    }

    /** The game is landscape only. Some video ads turn the phone to portrait, so landscape is set again as soon as any ad closes. */
    private fun landscape(activity: Activity) {
        runCatching { activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
    }

    fun online(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Only these spots are used in the live release (the player taps for them). */
    private val LIVE_SPOTS = setOf(Spot.FREE_COINS, Spot.CONTINUE, Spot.BOSS_REMATCH)

    /** Can this Watch button be shown? (after the tutorials, daily caps). */
    fun spotAvailable(spot: Spot): Boolean =
        ::progress.isInitialized && spot in LIVE_SPOTS && progress.unlocked >= FIRST_AUTO_LEVEL &&
            progress.adsToday < Progress.DAILY_AD_CAP && progress.spotUses(spot.name) < spot.perDay

    private fun request() = AdRequest.Builder().build()

    private fun loadRewarded(context: Context) {
        RewardedAd.load(context, REWARDED, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedReady = true }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; rewardedReady = false }
        })
    }

    /**
     * Shows a rewarded video. [onReward] runs only if the player watched to the end.
     * [onFail] gets a short reason ("Needs internet", "No video right now").
     */
    fun showRewarded(activity: Activity, spot: Spot, onReward: () -> Unit, onFail: (String) -> Unit) {
        if (!online(activity)) { onFail("Needs internet"); return }
        val ad = rewarded
        if (ad == null) { loadRewarded(activity); onFail("No video right now, try again soon"); return }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                landscape(activity)
                rewarded = null; rewardedReady = false
                loadRewarded(activity)
                if (earned) onReward() else onFail("Watch to the end to get the reward")
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                landscape(activity)
                rewarded = null; rewardedReady = false
                loadRewarded(activity)
                onFail("No video right now, try again soon")
            }
        }
        lastFullAd = System.currentTimeMillis()
        Analytics.ad("rewarded", spot.name)
        ad.show(activity) {
            earned = true
            progress.adWatched()
            progress.spotUsed(spot.name)
        }
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(context, INTERSTITIAL, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
        })
    }

    /** When the last full-screen ad (automatic or video) was shown. */
    private var lastFullAd = 0L

    /**
     * An automatic ad between levels: after a win, or after losing / quitting a level.
     * Never in levels 1-3, never with No Ads, at least 3 minutes after the last full-screen ad.
     * [then] always runs (straight away when no ad is shown).
     */
    fun maybeInterstitial(activity: Activity, level: Int, why: String, then: () -> Unit) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (!::progress.isInitialized || progress.noAds || level < FIRST_AUTO_LEVEL || ad == null ||
            now - lastFullAd < AUTO_GAP_MS || progress.adsToday >= Progress.DAILY_AD_CAP
        ) { then(); return }
        lastFullAd = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                landscape(activity); interstitial = null; loadInterstitial(activity); then() }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                landscape(activity); interstitial = null; loadInterstitial(activity); then() }
        }
        progress.adWatched()
        Analytics.ad("interstitial", why)
        ad.show(activity)
    }

    fun onPause() { if (::progress.isInitialized) progress.lastClose = System.currentTimeMillis() }

    /** No app-open ads in the live release. */
    @Suppress("UNUSED_PARAMETER")
    fun onResume(activity: Activity) {}
}
