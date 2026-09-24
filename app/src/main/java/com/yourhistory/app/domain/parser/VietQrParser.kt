package com.yourhistory.app.domain.parser

import com.yourhistory.app.domain.model.VietQrData
import com.yourhistory.app.domain.model.VietnameseBanks
import java.net.URI
import java.net.URLDecoder

object VietQrParser {

    /**
     * Parses a QR code payload, supporting both EMVCo TLV format and VietQR URL format.
     */
    fun parse(payload: String): VietQrData? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null

        // 1. Try URL parsing if it starts with http://, https://, or vietqr://
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("vietqr://", ignoreCase = true)
        ) {
            return parseUrlFormat(trimmed)
        }

        // 2. Try EMVCo TLV parsing
        return parseEmvCo(trimmed)
    }

    private fun parseEmvCo(payload: String): VietQrData? {
        try {
            val rootTags = parseTlv(payload)

            // Tag 38 is Merchant Account Info (Napas VietQR)
            val tag38Raw = rootTags["38"] ?: return null
            val tag38Subtags = parseTlv(tag38Raw)

            // Tag 38 -> Subtag 01 contains Beneficiary Bank & Account
            val tag38Sub01 = tag38Subtags["01"] ?: return null
            val (bankBin, accountNumber) = parseBankAndAccount(tag38Sub01)

            if (bankBin.isEmpty() || accountNumber.isEmpty()) {
                return null
            }

            // Tag 54 is Transaction Amount
            val amountStr = rootTags["54"]
            val amount = amountStr?.toDoubleOrNull()?.toLong()

            // Tag 59 is Beneficiary Name
            val recipientName = rootTags["59"]

            // Tag 62 is Additional Data (Memo / Purpose)
            var memo = ""
            val tag62Raw = rootTags["62"]
            if (tag62Raw != null) {
                val tag62Subtags = parseTlv(tag62Raw)
                memo = tag62Subtags["08"] ?: tag62Subtags["01"] ?: ""
            }

            val bankInfo = VietnameseBanks.findByBin(bankBin)
            val bankName = bankInfo?.shortName ?: "Ngân hàng ($bankBin)"

            return VietQrData(
                bankBin = bankBin,
                bankName = bankName,
                accountNumber = accountNumber,
                amount = amount,
                memo = memo,
                recipientName = recipientName,
                rawPayload = payload
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Parses nested TLV for Bank BIN and Account Number.
     * Subtag 01 in Tag 38 usually contains:
     * Tag 00: Bank BIN (6 chars)
     * Tag 01: Account number
     */
    private fun parseBankAndAccount(rawSub01: String): Pair<String, String> {
        val nested = parseTlv(rawSub01)
        val bin = nested["00"]
        val account = nested["01"]

        if (bin != null && account != null) {
            return Pair(bin, account)
        }

        // Fallback: If not nested TLV, first 6 digits might be BIN, remaining is account
        if (rawSub01.length >= 10 && rawSub01.substring(0, 6).all { it.isDigit() }) {
            return Pair(rawSub01.substring(0, 6), rawSub01.substring(6))
        }

        return Pair("", "")
    }

    /**
     * TLV parser: Reads 2 chars Tag, 2 chars Length, and Length chars Value.
     */
    fun parseTlv(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0
        while (index + 4 <= raw.length) {
            val tag = raw.substring(index, index + 2)
            val lengthStr = raw.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: break
            index += 4

            if (index + length > raw.length) break
            val value = raw.substring(index, index + length)
            result[tag] = value
            index += length
        }
        return result
    }

    private fun parseUrlFormat(urlStr: String): VietQrData? {
        try {
            val uri = URI(urlStr)
            val scheme = uri.scheme?.lowercase()
            val queryParams = parseQueryParams(uri.rawQuery ?: "")

            if (scheme == "vietqr") {
                val bankBin = queryParams["bank"] ?: ""
                val account = queryParams["account"] ?: ""
                val amount = queryParams["amount"]?.toLongOrNull()
                val memo = queryParams["memo"] ?: ""

                val bankInfo = VietnameseBanks.findByBin(bankBin) ?: VietnameseBanks.findByNameOrBin(bankBin)
                val resolvedBin = bankInfo?.bin ?: bankBin
                val bankName = bankInfo?.shortName ?: "Ngân hàng ($bankBin)"

                return VietQrData(
                    bankBin = resolvedBin,
                    bankName = bankName,
                    accountNumber = account,
                    amount = amount,
                    memo = memo,
                    rawPayload = urlStr
                )
            }

            val host = uri.host ?: ""
            if (host.contains("vietqr.io")) {
                val path = uri.path ?: ""
                val segments = path.split("/").filter { it.isNotEmpty() }
                if (segments.size >= 2) {
                    val target = segments[1] // "970422-12345678-compact.png"
                    val parts = target.split("-")
                    if (parts.size >= 2) {
                        val bankBin = parts[0]
                        val account = parts[1].substringBefore(".")
                        val amount = queryParams["amount"]?.toLongOrNull()
                        val memo = queryParams["addInfo"] ?: queryParams["memo"] ?: ""

                        val bankInfo = VietnameseBanks.findByBin(bankBin) ?: VietnameseBanks.findByNameOrBin(bankBin)
                        val resolvedBin = bankInfo?.bin ?: bankBin
                        val bankName = bankInfo?.shortName ?: "Ngân hàng ($bankBin)"

                        return VietQrData(
                            bankBin = resolvedBin,
                            bankName = bankName,
                            accountNumber = account,
                            amount = amount,
                            memo = memo,
                            rawPayload = urlStr
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // URL parse error
        }
        return null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (query.isBlank()) return params
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = if (pair.length > idx + 1) {
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                } else ""
                params[key] = value
            }
        }
        return params
    }
}
