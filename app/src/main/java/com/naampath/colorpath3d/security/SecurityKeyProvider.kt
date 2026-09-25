package com.naampath.colorpath3d.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecurityKeyProvider(context: Context) {
    private val appContext = context.applicationContext

    fun secret(): ByteArray {
        return try {
            keystoreKey().encoded ?: fileFallback()
        } catch (_: Throwable) {
            fileFallback()
        }
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = store.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun fileFallback(): ByteArray {
        val file = appContext.getFileStreamPath("progress.key")
        if (file.exists() && file.length() >= 32) return file.readBytes()
        val bytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        file.writeBytes(bytes)
        return bytes
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "colorpath3d.progress"
    }
}
