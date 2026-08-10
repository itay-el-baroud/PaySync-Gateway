package com.paysync.gateway.util
object SmsParser {
    data class ParsedResult(val amount: String?, val phone: String?, val raw: String)
    fun parse(message: String): ParsedResult {
        return try {
            val phoneRegex = Regex("01[0125][0-9]{8}")
            val amountRegex = Regex("\\d+[.,]?\\d*")
            val phoneMatch = phoneRegex.find(message)?.value
            var amountMatch: String? = null
            val allNumbers = amountRegex.findAll(message).map { it.value }.toList()
            for (n in allNumbers) {
                val v = n.replace(",", "").toDoubleOrNull() ?: continue
                if (v >= 5) { amountMatch = n; break }
            }
            ParsedResult(amountMatch, phoneMatch, message)
        } catch (e: Exception) {
            ParsedResult(null, null, message)
        }
    }
}
