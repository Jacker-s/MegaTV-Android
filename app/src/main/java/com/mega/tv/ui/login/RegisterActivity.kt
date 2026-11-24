package com.mega.tv.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.RegisterRequest
import com.mega.tv.databinding.ActivityRegisterBinding
import com.mega.tv.ui.home.HomeActivity
import com.mega.tv.ui.plans.PlansActivity
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root
        )

        prefs = PreferencesManager.getInstance(this)

        setupUI()
        setupListeners()
        AnimUtils.fadeInSlideUp(binding.registerMainContainer)
    }

    private fun setupUI() {
        val config = prefs.getConfig()
        config?.logoUrl?.let { logoUrl ->
            if (logoUrl.isNotBlank()) {
                binding.imgRegisterLogo.load(logoUrl) { crossfade(true) }
            }
        }

        if (DeviceUtils.isTv(this)) {
            val fields = listOf(
                binding.etRegisterName to "Nome",
                binding.etRegisterUsername to "Usuário",
                binding.etRegisterEmail to "E-mail",
                binding.etRegisterPassword to "Senha"
            )
            fields.forEach { (editText, label) ->
                editText.showSoftInputOnFocus = false
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        binding.tvVirtualKeyboardRegister?.attachEditText(editText)
                        binding.tvKeyboardRegisterLabel?.text = "Digite pelo controle remoto (Campo: $label)"
                    }
                }
            }
            binding.tvVirtualKeyboardRegister?.attachEditText(binding.etRegisterName)
            binding.tvVirtualKeyboardRegister?.onDoneClickListener = {
                when {
                    binding.etRegisterName.text.isNullOrBlank() -> binding.etRegisterName.requestFocus()
                    binding.etRegisterUsername.text.isNullOrBlank() -> binding.etRegisterUsername.requestFocus()
                    binding.etRegisterEmail.text.isNullOrBlank() -> binding.etRegisterEmail.requestFocus()
                    binding.etRegisterPassword.text.isNullOrBlank() -> binding.etRegisterPassword.requestFocus()
                    else -> performRegister()
                }
            }
            binding.etRegisterName.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.btnRegisterSubmit.setOnClickListener {
            performRegister()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val name = binding.etRegisterName.text.toString().trim()
        val username = binding.etRegisterUsername.text.toString().trim()
        val email = binding.etRegisterEmail.text.toString().trim()
        val password = binding.etRegisterPassword.text.toString().trim()

        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            showError("Por favor, preencha todos os campos.")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.createAppService().register(
                    RegisterRequest(
                        name = name,
                        username = username,
                        email = email,
                        password = password
                    )
                )
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val regRes = response.body()!!
                    val user = regRes.user

                    if (regRes.status == "success" && user != null) {
                        prefs.saveUser(user)

                        val dest = if (user.hasActiveSubscription) HomeActivity::class.java else PlansActivity::class.java
                        val intent = Intent(this@RegisterActivity, dest).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        showError(regRes.message ?: "Erro ao realizar cadastro.")
                    }
                } else {
                    showError("Falha no servidor (código ${response.code()}).")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Erro de conexão: ${e.localizedMessage ?: "Verifique sua internet."}")
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.registerProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegisterSubmit.isEnabled = !loading
        binding.tvRegisterError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvRegisterError.text = message
        binding.tvRegisterError.visibility = View.VISIBLE
    }
}
