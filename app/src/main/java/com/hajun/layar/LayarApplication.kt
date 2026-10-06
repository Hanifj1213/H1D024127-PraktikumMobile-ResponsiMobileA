package com.hajun.layar

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.hajun.layar.data.AppContainer
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class LayarApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer() }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        OkHttpClient.Builder()
                            .followSslRedirects(false)
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .callTimeout(30, TimeUnit.SECONDS)
                            .build()
                    },
                ),
            )
        }
        .build()
}
