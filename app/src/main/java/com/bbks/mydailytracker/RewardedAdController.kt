package com.bbks.mydailytracker

import android.app.Activity
import android.app.AlertDialog
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.widget.Button
import android.graphics.Typeface

class RewardedAdController(
    private val activity: Activity,
    private val adUnitId: String
) {
    private var rewardedAd: RewardedAd? = null
    private var loadingDialog: AlertDialog? = null

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                rewardedAd = null
            }
        })
    }

    fun showAd(
        remainingCount: Int,
        onSuccess: () -> Unit,
        onFail: () -> Unit,
        onUpgradeClick: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            showLoadingDialog(remainingCount, onSuccess, onFail, onUpgradeClick)

            // 광고 콜백 설정
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    dismissLoadingDialog()
                    onFail()
                }
            }
        } else {
            onFail()
        }
    }

    private fun showLoadingDialog(
        remainingCount: Int,
        onSuccess: () -> Unit,
        onFail: () -> Unit,
        onUpgradeClick: () -> Unit
    ) {
        if (loadingDialog?.isShowing == true) return

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ad_start_notice, null)
        val textView = view.findViewById<TextView>(R.id.ad_notice_text)
        val startButton = view.findViewById<Button>(R.id.ad_start_button)
        val cancelButton = view.findViewById<Button>(R.id.ad_cancel_button)
        val premiumButton = view.findViewById<Button>(R.id.ad_premium_button)

        val bold = activity.getString(R.string.ad_notice_bold)
        val rest = activity.getString(R.string.ad_notice_rest)
        val spannable = SpannableStringBuilder("$bold\n$rest")
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            bold.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable

        loadingDialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        startButton.setOnClickListener {
            loadingDialog?.dismiss()
            rewardedAd?.let { ad ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        onFail()
                    }
                }
                ad.show(activity) { onSuccess() }
            } ?: onFail()
        }

        cancelButton.setOnClickListener {
            loadingDialog?.dismiss()
            onFail()
        }

        view.findViewById<Button>(R.id.ad_premium_button).setOnClickListener {
            loadingDialog?.dismiss()
            onUpgradeClick()
        }

        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}