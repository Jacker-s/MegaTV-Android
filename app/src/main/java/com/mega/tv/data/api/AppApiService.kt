package com.mega.tv.data.api

import com.mega.tv.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AppApiService {

    @POST("?action=config")
    suspend fun getConfig(
        @Body emptyBody: Map<String, String> = emptyMap()
    ): Response<ConfigResponse>

    @POST("?action=login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("?action=register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("?action=plans")
    suspend fun getPlans(
        @Body emptyBody: Map<String, String> = emptyMap()
    ): Response<PlansResponse>

    @POST("?action=create_payment")
    suspend fun createPayment(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    @POST("?action=check_payment")
    suspend fun checkPayment(
        @Body request: CheckPaymentRequest
    ): Response<CheckPaymentResponse>

    @POST("?action=update_profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<GenericResponse>
}
