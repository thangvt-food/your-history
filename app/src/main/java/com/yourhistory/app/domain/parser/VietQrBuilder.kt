package com.yourhistory.app.domain.parser

/**
 * Dựng chuỗi VietQR EMVCo từ thông tin đã bóc tách để HIỆN mã QR cho app
 * ngân hàng quét — luồng universal, không phụ thuộc deep link của từng bank.
 *
 * Thuần Kotlin (không dùng android.*) để unit test được trên JVM.
 */
object VietQrBuilder {

    private const val NAPAS_GUID = "A000000727"
    private const val SERVICE_CODE = "QRIBFTTA"

    /**
     * Dựng payload EMVCo hoàn chỉnh (kèm CRC) hoặc null nếu thiếu STK/BIN.
     * Tên người nhận và nội dung được chuẩn hóa ASCII in hoa để mọi app
     * ngân hàng đọc được (tránh lỗi độ dài TLV với ký tự có dấu).
     */
    fun build(
        bankBin: String,
        accountNumber: String,
        amount: Long? = null,
        memo: String = "",
        recipientName: String = ""
    ): String? {
        val bin = bankBin.trim()
        val account = accountNumber.trim()
        if (bin.isEmpty() || account.isEmpty()) return null

        val beneficiary = tlv("00", bin) + tlv("01", account)
        val merchantAccount = tlv("00", NAPAS_GUID) + tlv("01", beneficiary) + tlv("02", SERVICE_CODE)

        val sb = StringBuilder()
        sb.append(tlv("00", "01"))
        sb.append(tlv("01", if (amount != null && amount > 0) "12" else "11"))
        sb.append(tlv("38", merchantAccount))
        sb.append(tlv("52", "0000"))
        sb.append(tlv("53", "704"))
        if (amount != null && amount > 0) {
            sb.append(tlv("54", amount.toString()))
        }
        sb.append(tlv("58", "VN"))
        val name = normalizeAscii(recipientName).take(25)
        if (name.isNotBlank()) {
            sb.append(tlv("59", name))
        }
        sb.append(tlv("60", "VN"))
        val cleanMemo = normalizeAscii(memo).take(50)
        if (cleanMemo.isNotBlank()) {
            sb.append(tlv("62", tlv("08", cleanMemo)))
        }
        sb.append("6304")

        val crc = crc16Ccitt(sb.toString())
        sb.append(String.format("%04X", crc))
        return sb.toString()
    }

    internal fun tlv(tag: String, value: String): String {
        require(value.length <= 99) { "TLV value quá dài cho tag $tag" }
        return tag + value.length.toString().padStart(2, '0') + value
    }

    /**
     * CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF) — chuẩn checksum EMVCo.
     * Vector kiểm chứng: crc16Ccitt("123456789") == 0x29B1.
     */
    fun crc16Ccitt(data: String): Int {
        var crc = 0xFFFF
        for (ch in data) {
            crc = crc xor (ch.code shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF
                else (crc shl 1) and 0xFFFF
            }
        }
        return crc
    }

    /**
     * Chuẩn hóa về ASCII in hoa: bỏ dấu tiếng Việt, chỉ giữ [A-Z0-9 và
     * dấu câu cơ bản], gộp khoảng trắng.
     */
    fun normalizeAscii(input: String): String {
        if (input.isBlank()) return ""
        val upper = input.trim().uppercase()
        val sb = StringBuilder(upper.length)
        for (ch in upper) {
            sb.append(
                when (ch) {
                    in 'A'..'Z', in '0'..'9', ' ', '.', ',', ';', ':', '\'', '"',
                    '(', ')', '?', '-', '+' -> ch
                    'À', 'Á', 'Ạ', 'Ả', 'Ã', 'Â', 'Ầ', 'Ấ', 'Ậ', 'Ẩ', 'Ẫ',
                    'Ă', 'Ằ', 'Ắ', 'Ặ', 'Ẳ', 'Ẵ' -> 'A'
                    'È', 'É', 'Ẹ', 'Ẻ', 'Ẽ', 'Ê', 'Ề', 'Ế', 'Ệ', 'Ể', 'Ễ' -> 'E'
                    'Ì', 'Í', 'Ị', 'Ỉ', 'Ĩ' -> 'I'
                    'Ò', 'Ó', 'Ọ', 'Ỏ', 'Õ', 'Ô', 'Ồ', 'Ố', 'Ộ', 'Ổ', 'Ỗ',
                    'Ơ', 'Ờ', 'Ớ', 'Ợ', 'Ở', 'Ỡ' -> 'O'
                    'Ù', 'Ú', 'Ụ', 'Ủ', 'Ũ', 'Ư', 'Ừ', 'Ứ', 'Ự', 'Ử', 'Ữ' -> 'U'
                    'Ỳ', 'Ý', 'Ỵ', 'Ỷ', 'Ỹ' -> 'Y'
                    'Đ' -> 'D'
                    else -> ' '
                }
            )
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }
}
