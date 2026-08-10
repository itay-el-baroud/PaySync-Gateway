package com.paysync.gateway.util

import java.util.Locale

/**
 * PaySync Gateway - Strict Sender ID Validation
 * Prevents SMS Spoofing by allowing ONLY official wallet/bank sender IDs
 */
object SenderValidator {

    // Whitelist - Case-insensitive exact matching
    // Egypt official wallets & banks - all normalized to lowercase
    private val WHITELIST = setOf(
        // Vodafone Cash
        "vodafone",
        "vodafonecash",
        "vodafone cash",
        "vf-cash",
        "vf cash",
        "vf",
        // e& money / Etisalat
        "e& money",
        "e&money",
        "e&",
        "etisalat",
        "etisalat cash",
        "etisalatcash",
        "eand",
        "eand money",
        // Orange Cash
        "orange",
        "orange cash",
        "orangecash",
        "orange money",
        // WE Pay
        "we",
        "we pay",
        "wepay",
        "we-pay",
        "we pay eg",
        // Instapay
        "instapay",
        "insta pay",
        "instapay eg",
        "ipn"
    )

    /**
     * Main validation: returns true ONLY if sender is in whitelist
     * and NOT a phone number
     */
    fun isAuthorized(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        
        val raw = sender.trim()
        if (raw.length < 2) return false

        // Rule 1: Reject any phone number immediately
        if (isPhoneNumber(raw)) {
            return false
        }

        // Rule 2: Case-insensitive exact match against whitelist
        val normalized = normalize(raw)
        return WHITELIST.contains(normalized)
    }

    /**
     * Detects if sender is a personal phone number
     * Egyptian: 010, 011, 012, 015, +20, 0020
     * International: any 7-15 digits
     */
    fun isPhoneNumber(sender: String): Boolean {
        try {
            val s = sender.trim().replace(" ", "").replace("-", "").replace("\u00A0", "")
            
            // Empty
            if (s.isEmpty()) return true

            // Egyptian patterns: 010xxxxxxxx, 011xxxxxxxx, 012xxxxxxxx, 015xxxxxxxx
            if (s.matches(Regex("^01[0125][0-9]{8}$"))) return true

            // +20 1[0125] xxxxxxxx
            if (s.matches(Regex("^\\+201[0125][0-9]{8}$"))) return true

            // 0020 1[0125] xxxxxxxx
            if (s.matches(Regex("^00201[0125][0-9]{8}$"))) return true

            // Starts with +20 and long enough
            if (s.startsWith("+20") && s.length in 12..14) return true
            if (s.startsWith("0020") && s.length in 13..15) return true

            // Generic international/local phone: only + and digits, 7-15 chars
            if (s.matches(Regex("^\\+?[0-9]{7,15}$"))) return true

            // Contains only digits and + with length >=7
            if (s.matches(Regex("^[0-9+]{7,15}$")) && s.any { it.isDigit() }) return true

            return false
        } catch (_: Exception) {
            // Fail secure: if parsing fails, treat as phone (reject)
            return true
        }
    }

    fun normalize(input: String): String {
        return input.lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun getCanonicalName(sender: String): String {
        return sender.trim()
    }
}
