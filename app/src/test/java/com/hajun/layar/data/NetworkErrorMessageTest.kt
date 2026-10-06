package com.hajun.layar.data

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorMessageTest {
    @Test
    fun offlineErrorsDoNotExposeExceptionDetails() {
        assertEquals("Koneksi tidak tersedia. Periksa internet kamu, lalu coba lagi.", UnknownHostException("private host").userMessage())
        assertEquals("Koneksi tidak tersedia. Periksa internet kamu, lalu coba lagi.", IOException("internal failure").userMessage())
    }

    @Test
    fun timeoutHasSpecificRetryCopy() {
        assertEquals("Koneksi terlalu lama. Coba lagi sebentar lagi.", SocketTimeoutException("secret").userMessage())
    }

    @Test
    fun callTimeoutHasSpecificRetryCopy() {
        assertEquals("Koneksi terlalu lama. Coba lagi sebentar lagi.", java.io.InterruptedIOException("timeout").userMessage())
    }

    @Test
    fun httpFailureHasSpecificServiceCopy() {
        assertEquals("Layanan TVmaze sedang bermasalah. Coba lagi sebentar lagi.", HttpException(Response.error<String>(503, "private".toResponseBody())).userMessage())
    }

    @Test
    fun notFoundHasSpecificUnavailableCopy() {
        assertEquals("Tayangan tidak ditemukan. Silakan kembali dan pilih tayangan lain.", HttpException(Response.error<String>(404, "private".toResponseBody())).userMessage())
    }

    @Test
    fun unexpectedFailureDoesNotExposeStackTraceOrMessage() {
        assertEquals("Data belum bisa dimuat. Silakan coba lagi.", IllegalStateException("stacktrace detail").userMessage())
    }
}
