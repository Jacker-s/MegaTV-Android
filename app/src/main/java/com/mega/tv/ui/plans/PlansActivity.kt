package com.mega.tv.ui.plans

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.CheckPaymentRequest
import com.mega.tv.data.model.PaymentRequest
import com.mega.tv.data.model.PlanItem
import com.mega.tv.databinding.ActivityPlansBinding
import com.mega.tv.ui.custom.TvFocusButton
import com.mega.tv.ui.home.HomeActivity
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class PlansActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlansBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: PlansAdapter

    private var pollingJob: Job? = null
    private var selectedPlan: PlanItem? = null
    private var currentPixCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root
        )

        prefs = PreferencesManager.getInstance(this)

        setupRecyclerView()
        setupListeners()
        fetchPlans()
        AnimUtils.fadeInSlideUp(binding.plansMainContainer)
    }

    private fun setupRecyclerView() {
        adapter = PlansAdapter(emptyList()) { plan ->
            createPaymentForPlan(plan)
        }
        binding.rvPlans.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPlans.adapter = adapter
    }

    private fun setupListeners() {
        (binding.btnCancelPayment as? TvFocusButton)?.setSecondary(true)

        binding.btnCancelPayment.setOnClickListener {
            stopPolling()
            binding.pixPaymentLayout.visibility = View.GONE
            AnimUtils.crossfadeVisible(binding.plansSelectionLayout)
            if (DeviceUtils.isTv(this)) {
                binding.rvPlans.requestFocus()
            }
        }

        binding.btnCopyPixCode.setOnClickListener {
            if (currentPixCode.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Código PIX", currentPixCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Código PIX copiado com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchPlans() {
        binding.plansProgressBar.visibility = View.VISIBLE
        binding.tvPlansError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.createAppService().getPlans()
                binding.plansProgressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val plans = response.body()!!.plans
                    if (plans.isNotEmpty()) {
                        adapter.updateData(plans)
                    } else {
                        val defaultPlans = listOf(
                            PlanItem(1, "Plano Mensal", "Acesso total por 30 dias", 29.90, 30),
                            PlanItem(2, "Plano Trimestral", "Acesso total por 90 dias com desconto", 79.90, 90),
                            PlanItem(3, "Plano Anual", "Acesso total por 365 dias (Melhor Valor)", 249.90, 365)
                        )
                        adapter.updateData(defaultPlans)
                    }
                    if (DeviceUtils.isTv(this@PlansActivity)) {
                        binding.rvPlans.post { binding.rvPlans.getChildAt(0)?.requestFocus() }
                    }
                } else {
                    showPlansError("Erro ao carregar planos (código ${response.code()})")
                }
            } catch (e: Exception) {
                binding.plansProgressBar.visibility = View.GONE
                showPlansError("Erro de conexão ao buscar planos: ${e.localizedMessage}")
            }
        }
    }

    private fun createPaymentForPlan(plan: PlanItem) {
        selectedPlan = plan
        val user = prefs.getUser()
        val userId = user?.id ?: 1L

        binding.plansProgressBar.visibility = View.VISIBLE
        binding.tvPlansError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.createAppService().createPayment(
                    PaymentRequest(userId = userId, planId = plan.id)
                )
                binding.plansProgressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val payment = response.body()!!
                    val pixCode = payment.pixCode ?: payment.qrCode ?: ""
                    val paymentId = payment.getPaymentIdString().ifBlank { "pay_${System.currentTimeMillis()}" }

                    displayPixScreen(plan, pixCode, paymentId)
                } else {
                    showPlansError("Não foi possível gerar o PIX. Tente novamente.")
                }
            } catch (e: Exception) {
                binding.plansProgressBar.visibility = View.GONE
                showPlansError("Erro ao criar pagamento: ${e.localizedMessage}")
            }
        }
    }

    private fun displayPixScreen(plan: PlanItem, pixCode: String, paymentId: String) {
        currentPixCode = pixCode
        binding.plansSelectionLayout.visibility = View.GONE
        AnimUtils.crossfadeVisible(binding.pixPaymentLayout)

        binding.tvPixPlanTitle.text = "${plan.title} - PIX"
        binding.tvPixAmount.text = String.format(Locale.getDefault(), "Valor: R$ %.2f", plan.price)
        binding.tvPixCopyPaste.text = pixCode

        binding.qrCodeView.renderQrCode(pixCode, 512)
        if (DeviceUtils.isTv(this)) {
            binding.btnCancelPayment.requestFocus()
        }

        startPaymentPolling(paymentId, plan.durationDays)
    }

    private fun startPaymentPolling(paymentId: String, durationDays: Int) {
        stopPolling()
        pollingJob = lifecycleScope.launch {
            binding.tvPollingStatus.text = getString(com.mega.tv.R.string.pix_waiting_confirmation)
            binding.pollingProgressBar.visibility = View.VISIBLE

            var isApproved = false
            while (isActive && !isApproved) {
                delay(5000)

                try {
                    val response = ApiClient.createAppService().checkPayment(
                        CheckPaymentRequest(paymentId = paymentId)
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val check = response.body()!!
                        if (check.paymentStatus.equals("approved", ignoreCase = true)) {
                            isApproved = true
                            onPaymentApproved(check.expDate, durationDays)
                            break
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun onPaymentApproved(serverExpDate: Long?, durationDays: Int) {
        binding.pollingProgressBar.visibility = View.GONE
        binding.tvPollingStatus.text = getString(com.mega.tv.R.string.pix_approved_message)
        binding.tvPollingStatus.setTextColor(Color.parseColor("#00C853"))

        val calculatedExpDate = serverExpDate ?: ((System.currentTimeMillis() / 1000L) + (durationDays * 86400L))
        prefs.updateSubscription(hasActiveSubscription = true, expDate = calculatedExpDate)

        lifecycleScope.launch {
            delay(1500)
            startActivity(Intent(this@PlansActivity, HomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun showPlansError(message: String) {
        binding.tvPlansError.text = message
        binding.tvPlansError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }
}
