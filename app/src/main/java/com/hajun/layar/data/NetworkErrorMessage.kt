package com.hajun.layar.data

import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

internal fun Exception.userMessage(): String = when (this) {
    is InterruptedIOException -> "Koneksi terlalu lama. Coba lagi sebentar lagi."
    is HttpException -> if (code() == 404) {
        "Tayangan tidak ditemukan. Silakan kembali dan pilih tayangan lain."
    } else {
        "Layanan TVmaze sedang bermasalah. Coba lagi sebentar lagi."
    }
    is IOException -> "Koneksi tidak tersedia. Periksa internet kamu, lalu coba lagi."
    else -> "Data belum bisa dimuat. Silakan coba lagi."
}
