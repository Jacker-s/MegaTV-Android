package com.mega.tv.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.ConfigResponse
import com.mega.tv.databinding.ActivitySplashBinding
import com.mega.tv.ui.home.HomeActivity
import com.mega.tv.ui.login.LoginActivity
import com.mega.tv.ui.plans.PlansActivity
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(this, binding.root)

        prefs = PreferencesManager.getInstance(this)

        startLogoFadeIn()
        fetchConfigAndNavigate()
    }

    private fun startLogoFadeIn() {
        binding.splashContainer.alpha = 0f
        binding.splashContainer.scaleX = 0.9f
        binding.splashContainer.scaleY = 0.9f

        val fadeAnim = ObjectAnimator.ofFloat(binding.splashContainer, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(binding.splashContainer, View.SCALE_X, 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.splashContainer, View.SCALE_Y, 0.9f, 1f)

        AnimatorSet().apply {
            playTogether(fadeAnim, scaleX, scaleY)
            duration = 650
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun fetchConfigAndNavigate() {
        lifecycleScope.launch {
            delay(350)

            var config: ConfigResponse? = null
            try {
                val response = ApiClient.createAppService().getConfig()
                if (response.isSuccessful && response.body() != null) {
                    config = response.body()
                    prefs.saveConfig(config!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                config = prefs.getConfig()
            }

            config?.logoUrl?.let { logoUrl ->
                if (logoUrl.isNotBlank()) {
                    binding.imgSplashLogo.load(logoUrl) {
                        crossfade(true)
                    }
                }
            }

            delay(750)
            routeNextScreen(config)
        }
    }

    private fun routeNextScreen(config: ConfigResponse?) {
        val authEnabled = config?.authEnabled ?: true
        val savedUser = prefs.getUser()

        val nextIntent = when {
            !authEnabled -> Intent(this, HomeActivity::class.java)
            savedUser != null && savedUser.isSubscriptionValid() -> Intent(this, HomeActivity::class.java)
            savedUser != null && !savedUser.isSubscriptionValid() -> Intent(this, PlansActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        startActivity(nextIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
