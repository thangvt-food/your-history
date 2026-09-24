package com.yourhistory.app

import com.yourhistory.app.domain.parser.VietQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VietQrParserTest {

    @Test
    fun testParseEmvCoVietQrPayload() {
        // Sample standard Napas VietQR string with MBBank BIN (970422), account 0123456789, amount 50000, memo "Tien an trua"
        val payload = "00020101021238540010A00000072701240006970422011001234567890208QRIBFTTA53037045405500005802VN5912NGUYEN VAN A62160812Tien an trua6304ABCD"

        val result = VietQrParser.parse(payload)

        assertNotNull("Result should not be null", result)
        assertEquals("970422", result!!.bankBin)
        assertEquals("MBBank", result.bankName)
        assertEquals("0123456789", result.accountNumber)
        assertEquals(50000L, result.amount)
        assertEquals("Tien an trua", result.memo)
        assertEquals("NGUYEN VAN A", result.recipientName)
    }

    @Test
    fun testParseVietQrSchemeUrl() {
        val url = "vietqr://transfer?bank=970422&account=0123456789&amount=100000&memo=Com%20trua"

        val result = VietQrParser.parse(url)

        assertNotNull(result)
        assertEquals("970422", result!!.bankBin)
        assertEquals("MBBank", result.bankName)
        assertEquals("0123456789", result.accountNumber)
        assertEquals(100000L, result.amount)
        assertEquals("Com trua", result.memo)
    }

    @Test
    fun testParseVietQrWebUrl() {
        val url = "https://img.vietqr.io/image/970436-0071000123456-compact.png?amount=250000&addInfo=Tien%20nha"

        val result = VietQrParser.parse(url)

        assertNotNull(result)
        assertEquals("970436", result!!.bankBin)
        assertEquals("Vietcombank", result.bankName)
        assertEquals("0071000123456", result.accountNumber)
        assertEquals(250000L, result.amount)
        assertEquals("Tien nha", result.memo)
    }

    @Test
    fun testParseInvalidPayload() {
        assertNull(VietQrParser.parse(""))
        assertNull(VietQrParser.parse("   "))
        assertNull(VietQrParser.parse("random string not a qr code"))
    }
}
