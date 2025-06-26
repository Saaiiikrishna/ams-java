package com.example.subscriberapp.di

import android.content.Context
import com.example.subscriberapp.data.api.ApiService
import com.example.subscriberapp.data.TokenManager
import com.example.subscriberapp.data.DynamicApiService
import com.example.subscriberapp.data.ServerManager
import com.example.subscriberapp.util.ServerDiscovery
import com.example.subscriberapp.util.MDnsDiscovery
import com.example.subscriberapp.util.EnhancedMDnsDiscovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Default fallback URLs
    private const val LOCALHOST_URL = "http://localhost:8080/"
    private const val EMULATOR_URL = "http://10.0.2.2:8080/"

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        tokenManager: TokenManager
    ): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(request)
        }

        // Retry interceptor for failed connections
        val retryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)

            // Retry up to 3 times for connection failures
            var tryCount = 0
            while (!response.isSuccessful && tryCount < 3) {
                Log.w("NetworkModule", "🔄 Request failed (attempt ${tryCount + 1}/3): ${response.code}")
                response.close()
                tryCount++

                // Wait a bit before retrying
                Thread.sleep(1000)
                response = chain.proceed(request)
            }

            if (response.isSuccessful) {
                Log.i("NetworkModule", "✅ Request successful after ${tryCount + 1} attempts")
            } else {
                Log.e("NetworkModule", "❌ Request failed after ${tryCount + 1} attempts: ${response.code}")
            }

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            // Increased timeouts for better reliability
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // Add call timeout for overall request timeout
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDynamicApiService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): DynamicApiService {
        return DynamicApiService(okHttpClient, context)
    }

    @Provides
    @Singleton
    fun provideServerManager(
        @ApplicationContext context: Context,
        dynamicApiService: DynamicApiService
    ): ServerManager {
        return ServerManager(context, dynamicApiService)
    }

    @Provides
    @Singleton
    fun provideApiService(dynamicApiService: DynamicApiService): ApiService {
        return dynamicApiService.getApiService()
    }

    @Provides
    @Singleton
    fun provideMDnsDiscovery(@ApplicationContext context: Context): MDnsDiscovery {
        return MDnsDiscovery(context)
    }

    @Provides
    @Singleton
    fun provideEnhancedMDnsDiscovery(@ApplicationContext context: Context): EnhancedMDnsDiscovery {
        return EnhancedMDnsDiscovery(context)
    }
}
