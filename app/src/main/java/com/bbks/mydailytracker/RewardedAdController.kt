package com.bbks.mydailytracker

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.os.Handler
import android.os.Looper
import android.widget.Button

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
        onFail: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            showLoadingDialog(remainingCount, onSuccess, onFail)

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
        onFail: () -> Unit
    ) {
        if (loadingDialog?.isShowing == true) return

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ad_start_notice, null)
        val textView = view.findViewById<TextView>(R.id.ad_notice_text)
        val startButton = view.findViewById<Button>(R.id.ad_start_button)
        val cancelButton = view.findViewById<Button>(R.id.ad_cancel_button)

        textView.text = "무료 진입 기회 (2회) 를 모두 사용했어요.\n광고를 시청하면 설정 화면으로 이동할 수 있어요."

        loadingDialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(false)
            .create()

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

        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}