package com.yourhistory.app

import com.yourhistory.app.domain.parser.VietQrBuilder
import com.yourhistory.app.domain.parser.VietQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VietQrBuilderTest {

    @Test
    fun testCrc16KnownVector() {
        // Vector chuẩn CRC-16/CCITT-FALSE
        assertEquals(0x29B1, VietQrBuilder.crc16Ccitt("123456789"))
    }

    @Test
    fun testNormalizeAsciiVietnamese() {
        assertEquals("COM TRUA", VietQrBuilder.normalizeAscii("Cơm trưa"))
        assertEquals("NGUYEN VAN A", VietQrBuilder.normalizeAscii("Nguyễn Văn A"))
        assertEquals("TIEN NHA", VietQrBuilder.normalizeAscii("  Tiền   nhà "))
    }

    @Test
    fun testBuildDynamicQrRoundTrip() {
        val payload = VietQrBuilder.build(
            bankBin = "970422",
            accountNumber = "12345678",
            amount = 100000L,
            memo = "Cơm trưa",
            recipientName = "Nguyễn Văn A"
        )

        assertNotNull(payload)
        // QR động: tag 01 = "12"
        assertTrue(payload!!.contains("010212"))

        // Parse lại bằng parser hiện có phải ra đúng thông tin
        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("970422", parsed!!.bankBin)
        assertEquals("12345678", parsed.accountNumber)
        assertEquals(100000L, parsed.amount)
        assertEquals("COM TRUA", parsed.memo)
        assertEquals("NGUYEN VAN A", parsed.recipientName)
    }

    @Test
    fun testBuildStaticQrHasNoAmount() {
        val payload = VietQrBuilder.build(
            bankBin = "970436",
            accountNumber = "0071000123456",
            amount = null,
            memo = "",
            recipientName = ""
        )

        assertNotNull(payload)
        // QR tĩnh: tag 01 = "11", không có tag 54
        assertTrue(payload!!.contains("010211"))
        val parsed = VietQrParser.parse(payload)
        assertNotNull(parsed)
        assertEquals("970436", parsed!!.bankBin)
        assertEquals("0071000123456", parsed.accountNumber)
    }

    @Test
    fun testBuildReturnsNullWhenMissingAccount() {
        assertNull(VietQrBuilder.build("", "12345678"))
        assertNull(VietQrBuilder.build("970422", "   "))
    }
}
