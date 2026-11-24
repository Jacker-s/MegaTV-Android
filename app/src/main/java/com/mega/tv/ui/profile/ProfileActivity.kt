package com.mega.tv.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.UpdateProfileRequest
import com.mega.tv.databinding.ActivityProfileBinding
import com.mega.tv.ui.login.LoginActivity
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root
        )

        prefs = PreferencesManager.getInstance(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val user = prefs.getUser()

        binding.btnProfileBack.setOnClickListener { finish() }
        binding.btnLogout.setSecondary(true)

        if (user != null) {
            binding.tvProfileName.text = if (!user.name.isNullOrBlank()) user.name else "Cliente MEga"
            binding.tvProfileUsername.text = "@${user.username}"
            binding.etProfileEmail.setText(user.email ?: "")

            val isValid = user.isSubscriptionValid()
            if (isValid) {
                binding.tvSubscriptionBadge.text = "Assinatura Ativa"
                binding.tvSubscriptionBadge.setTextColor(Color.parseColor("#00C853"))
            } else {
                binding.tvSubscriptionBadge.text = "Assinatura Expirada"
                binding.tvSubscriptionBadge.setTextColor(Color.parseColor("#D50000"))
            }

            if (user.expDate > 0) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val expDateFormatted = sdf.format(Date(user.expDate * 1000L))
                binding.tvProfileExpiry.text = "Vencimento: $expDateFormatted"
            } else {
                binding.tvProfileExpiry.text = "Sem plano ativo"
            }
        }

        if (DeviceUtils.isTv(this)) {
            binding.btnLogout.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            performUpdateProfile()
        }

        binding.btnLogout.setOnClickListener {
            prefs.clearSession()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun performUpdateProfile() {
        val user = prefs.getUser() ?: return
        val newEmail = binding.etProfileEmail.text.toString().trim()
        val newPassword = binding.etProfilePassword.text.toString().trim()

        if (newEmail.isBlank()) {
            showStatus("O e-mail não pode ficar em branco.", isError = true)
            return
        }

        binding.profileProgressBar.visibility = View.VISIBLE
        binding.tvProfileStatus.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.createAppService().updateProfile(
                    UpdateProfileRequest(
                        userId = user.id ?: 1L,
                        email = newEmail,
                        password = newPassword
                    )
                )
                binding.profileProgressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.status == "success") {
                        prefs.saveUser(user.copy(email = newEmail))
                        showStatus(res.message ?: "Perfil atualizado com sucesso!", isError = false)
                    } else {
                        showStatus(res.message ?: "Falha ao atualizar perfil.", isError = true)
                    }
                } else {
                    showStatus("Erro ao atualizar (código ${response.code()})", isError = true)
                }
            } catch (e: Exception) {
                binding.profileProgressBar.visibility = View.GONE
                showStatus("Erro de conexão: ${e.localizedMessage}", isError = true)
            }
        }
    }

    private fun showStatus(message: String, isError: Boolean) {
        binding.tvProfileStatus.text = message
        binding.tvProfileStatus.setTextColor(
            if (isError) Color.parseColor("#D50000") else Color.parseColor("#00C853")
        )
        binding.tvProfileStatus.visibility = View.VISIBLE
    }
}
