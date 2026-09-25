package com.naampath.colorpath3d.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.naampath.colorpath3d.R
import com.naampath.colorpath3d.data.ProgressRepository
import com.naampath.colorpath3d.firebase.AnalyticsManager
import com.naampath.colorpath3d.firebase.RemoteConfigManager

class AdManager(
    private val context: Context,
    private val progress: ProgressRepository,
    private val remote: RemoteConfigManager,
    private val analytics: AnalyticsManager
) {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var appOpen: AppOpenAd? = null
    private var showing = false

    fun initialize() {
        runCatching { MobileAds.initialize(context) }
        preload()
    }

    fun adsRemoved(): Boolean = progress.state.value.wallet.adsRemoved || progress.state.value.wallet.premium

    fun preload() {
        if (adsRemoved()) return
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, context.getString(R.string.ad_interstitial_unit), request, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
        })
        RewardedAd.load(context, context.getString(R.string.ad_rewarded_unit), request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
        })
        AppOpenAd.load(context, context.getString(R.string.ad_app_open_unit), request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { appOpen = null }
        })
    }

    suspend fun maybeInterstitial(activity: Activity, drawing: Boolean) {
        if (adsRemoved() || drawing || showing) return
        val tuning = remote.tuning()
        val meta = progress.adMeta()
        val now = System.currentTimeMillis()
        val dueCount = meta.levelsSinceAd + 1 >= tuning.interstitialEvery
        val dueTime = now - meta.lastInterstitialAt >= tuning.minSecondsBetweenInterstitials * 1000L
        if (!dueCount || !dueTime) {
            progress.writeAdMeta(meta.copy(levelsSinceAd = meta.levelsSinceAd + 1))
            return
        }
        val ad = interstitial
        if (ad == null) {
            preload()
            return
        }
        showing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                interstitial = null
                preload()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                showing = false
            }
        }
        ad.show(activity)
        progress.writeAdMeta(meta.copy(levelsSinceAd = 0, lastInterstitialAt = now))
        analytics.event("interstitial_shown")
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        if (showing) return
        val ad = rewarded
        if (ad == null) {
            preload()
            return
        }
        showing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                rewarded = null
                preload()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                showing = false
            }
        }
        ad.show(activity) {
            analytics.event("rewarded_ad_watched")
            onReward()
        }
    }

    fun maybeAppOpen(activity: Activity, allow: Boolean) {
        if (!allow || adsRemoved() || showing) return
        val tuning = remote.tuning()
        val meta = kotlinx.coroutines.runBlocking { progress.adMeta() }
        val now = System.currentTimeMillis()
        if (now - meta.lastAppOpenAt < tuning.appOpenMinIntervalSec * 1000L) return
        val ad = appOpen ?: return
        showing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                appOpen = null
                preload()
            }
        }
        ad.show(activity)
        kotlinx.coroutines.runBlocking { progress.writeAdMeta(meta.copy(lastAppOpenAt = now)) }
    }
}
