package com.mega.tv.data.model

import com.google.gson.annotations.SerializedName

data class ConfigResponse(
    @SerializedName("status") val status: String? = "success",
    @SerializedName("site_title") val siteTitle: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("theme_color") val themeColor: String? = null,
    @SerializedName("player_color") val playerColor: String? = null,
    @SerializedName("default_player") val defaultPlayer: String? = "megaplay",
    @SerializedName("auth_enabled") val authEnabled: Boolean = true,
    @SerializedName("xtream_url") val xtreamUrl: String? = null,
    @SerializedName("app_package_name") val appPackageName: String? = null,
    @SerializedName("app_version") val appVersion: String? = null,
    @SerializedName("app_download_link") val appDownloadLink: String? = null,
    @SerializedName("tmdb_api_key") val tmdbApiKey: String? = null,
    @SerializedName("tmdb_language") val tmdbLanguage: String? = "pt-BR"
)

data class UserModel(
    @SerializedName("id") val id: Long? = 0L,
    @SerializedName("name") val name: String? = "",
    @SerializedName("username") val username: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName("exp_date") val expDate: Long = 0L, // Unix timestamp in seconds
    @SerializedName("status") val status: String? = "Active",
    @SerializedName("has_active_subscription") val hasActiveSubscription: Boolean = false
) {
    fun isSubscriptionValid(): Boolean {
        if (!hasActiveSubscription) return false
        val currentSeconds = System.currentTimeMillis() / 1000L
        return expDate > currentSeconds
    }
}

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: UserModel? = null
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: UserModel? = null
)

data class PlanItem(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("duration_days") val durationDays: Int
)

data class PlansResponse(
    @SerializedName("status") val status: String? = "success",
    @SerializedName("plans") val plans: List<PlanItem> = emptyList()
)

data class PaymentRequest(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("plan_id") val planId: Int
)

data class PaymentResponse(
    @SerializedName("payment_id") val paymentId: Any? = null,
    @SerializedName("qr_code") val qrCode: String? = null,
    @SerializedName("qr_code_base64") val qrCodeBase64: String? = null,
    @SerializedName("pix_code") val pixCode: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("status") val status: String? = null
) {
    fun getPaymentIdString(): String = paymentId?.toString() ?: ""
}

data class CheckPaymentRequest(
    @SerializedName("payment_id") val paymentId: Any
)

data class CheckPaymentResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("has_active_subscription") val hasActiveSubscription: Boolean? = null,
    @SerializedName("exp_date") val expDate: Long? = null
)

data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class GenericResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)
