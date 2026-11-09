package com.retroraja.wordnoid.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.retroraja.wordnoid.data.Progress

/**
 * All ads. Uses Google's TEST ad units until the AdMob account exists (then only the IDs below change).
 * Rules agreed in the plan:
 *  - rewarded videos only when the player taps "Watch" (13 spots), none before level 5, about 18 a day at most;
 *  - a picture-only interstitial every 3 levels from level 15 (not with No Ads);
 *  - an App Open ad when coming back after 30+ minutes (max 3 a day, never in the first 2 days);
 *  - banners only on win/pause screens and the Level Map, never during play;
 *  - under-13 players get child-safe, non-personalised ads.
 * Without internet the game works normally; Watch buttons just say "Needs internet".
 */
object Ads {
    // Real AdMob units in the Play bundle; Google's test units in the test APK (safe to tap).
    private val REAL = com.retroraja.wordnoid.BuildConfig.REAL_ADS
    private val REWARDED = if (REAL) "ca-app-pub-3867331045861180/9863612729" else "ca-app-pub-3940256099942544/5224354917"
    private val INTERSTITIAL = if (REAL) "ca-app-pub-3867331045861180/6957026594" else "ca-app-pub-3940256099942544/1033173712"
    private val BANNER = if (REAL) "ca-app-pub-3867331045861180/1477644215" else "ca-app-pub-3940256099942544/9214589741"
    private val APP_OPEN = if (REAL) "ca-app-pub-3867331045861180/8294158995" else "ca-app-pub-3940256099942544/9257395921"
    /** App Open ads: on (after 30+ min away, max 3 a day, never in the first 2 days). */
    private const val APP_OPEN_ON = true

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
    private var appOpen: AppOpenAd? = null
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
            if (APP_OPEN_ON) loadAppOpen(activity)
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

    /** Can this Watch button be shown? (level 5+, daily caps). */
    fun spotAvailable(spot: Spot): Boolean =
        ::progress.isInitialized && progress.unlocked >= 5 && progress.adsToday < Progress.DAILY_AD_CAP && progress.spotUses(spot.name) < spot.perDay

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

    /** After a win: a picture ad every 3 levels from level 15, unless No Ads was bought. */
    fun maybeInterstitial(activity: Activity, level: Int, then: () -> Unit) {
        val ad = interstitial
        if (progress.noAds || level < 15 || progress.levelsSinceAd < 3 || ad == null || progress.adsToday >= Progress.DAILY_AD_CAP) { then(); return }
        progress.resetLevelsSinceAd()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                landscape(activity); interstitial = null; loadInterstitial(activity); then() }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                landscape(activity); interstitial = null; loadInterstitial(activity); then() }
        }
        progress.adWatched()
        ad.show(activity)
    }

    private fun loadAppOpen(context: Context) {
        AppOpenAd.load(context, APP_OPEN, request(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { appOpen = null }
        })
    }

    /** Called when the app comes back to the front. */
    fun onResume(activity: Activity) {
        if (!::progress.isInitialized) return
        val away = System.currentTimeMillis() - progress.lastClose
        val ad = appOpen
        if (progress.lastClose > 0 && away > 30 * 60_000L && !progress.noAds && progress.daysPlayed >= 2 &&
            progress.appOpenAdsToday < 3 && ad != null && progress.adsToday < Progress.DAILY_AD_CAP
        ) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                landscape(activity); appOpen = null; loadAppOpen(activity) }
            }
            progress.appOpenAdShown()
            progress.adWatched()
            ad.show(activity)
        }
    }

    fun onPause() { if (::progress.isInitialized) progress.lastClose = System.currentTimeMillis() }

    /** A banner (win/pause box or the small Level Map strip). Nothing when No Ads was bought or offline. */
    @Composable
    fun Banner(modifier: Modifier = Modifier, big: Boolean = false) {
        if (!::progress.isInitialized || progress.noAds || !started) return
        AndroidView(
            modifier = modifier.fillMaxWidth().height(if (big) 250.dp else 50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(if (big) AdSize.MEDIUM_RECTANGLE else AdSize.BANNER)
                    adUnitId = BANNER
                    adListener = object : AdListener() {}
                    loadAd(request())
                }
            },
        )
    }
}
