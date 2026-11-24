package com.mega.tv.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.LoginRequest
import com.mega.tv.databinding.ActivityLoginBinding
import com.mega.tv.ui.home.HomeActivity
import com.mega.tv.ui.plans.PlansActivity
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root
        )

        prefs = PreferencesManager.getInstance(this)

        setupUI()
        setupListeners()
        AnimUtils.fadeInSlideUp(binding.loginMainContainer)
    }

    private fun setupUI() {
        val config = prefs.getConfig()
        config?.logoUrl?.let { logoUrl ->
            if (logoUrl.isNotBlank()) {
                binding.imgLoginLogo.load(logoUrl) { crossfade(true) }
            }
        }

        if (DeviceUtils.isTv(this)) {
            binding.etUsername.showSoftInputOnFocus = false
            binding.etPassword.showSoftInputOnFocus = false
            binding.tvVirtualKeyboard?.attachEditText(binding.etUsername)

            binding.etUsername.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.tvVirtualKeyboard?.attachEditText(binding.etUsername)
                    binding.tvKeyboardTargetLabel?.text = "Digite usando o controle remoto (Campo: Usuário)"
                }
            }
            binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.tvVirtualKeyboard?.attachEditText(binding.etPassword)
                    binding.tvKeyboardTargetLabel?.text = "Digite usando o controle remoto (Campo: Senha)"
                }
            }
            binding.tvVirtualKeyboard?.onDoneClickListener = {
                if (binding.etPassword.text.isNullOrBlank()) {
                    binding.etPassword.requestFocus()
                } else {
                    performLogin()
                }
            }

            binding.etUsername.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isBlank() || password.isBlank()) {
            showError("Por favor, preencha o usuário e a senha.")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.createAppService().login(LoginRequest(username, password))
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val loginRes = response.body()!!
                    val user = loginRes.user

                    if (loginRes.status == "success" && user != null) {
                        prefs.saveUser(user)

                        if (user.hasActiveSubscription) {
                            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        } else {
                            startActivity(Intent(this@LoginActivity, PlansActivity::class.java))
                        }
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        showError(loginRes.message ?: "Usuário ou senha incorretos.")
                    }
                } else {
                    showError("Falha na autenticação (código ${response.code()}).")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Erro de conexão: ${e.localizedMessage ?: "Verifique sua internet."}")
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.loginProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.tvLoginError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvLoginError.text = message
        binding.tvLoginError.visibility = View.VISIBLE
    }
}
