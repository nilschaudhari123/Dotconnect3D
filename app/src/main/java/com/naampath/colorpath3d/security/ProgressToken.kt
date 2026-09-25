package com.naampath.colorpath3d.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** HMAC signatures so edited coins, scores, and completions are rejected. */
object ProgressToken {
    fun sign(secret: ByteArray, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun verify(secret: ByteArray, payload: String, token: String): Boolean {
        val expected = sign(secret, payload)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), token.toByteArray(Charsets.UTF_8))
    }

    fun levelPayload(levelId: Int, mode: String, stars: Int, score: Int, timeMs: Long, moves: Int): String =
        "$levelId|$mode|$stars|$score|$timeMs|$moves|v1"

    fun walletPayload(coins: Int, hints: Int, adsRemoved: Boolean, premium: Boolean): String =
        "$coins|$hints|$adsRemoved|$premium|v1"
}
