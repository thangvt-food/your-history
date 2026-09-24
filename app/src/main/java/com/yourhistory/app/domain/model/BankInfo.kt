package com.yourhistory.app.domain.model

data class BankInfo(
    val bin: String,
    val shortName: String,
    val fullName: String,
    val packageName: String? = null
)

object VietnameseBanks {
    val ALL = listOf(
        BankInfo("970422", "MBBank", "Ngân hàng TMCP Quân Đội", "com.mbmobile"),
        BankInfo("970436", "Vietcombank", "Ngân hàng Ngoại thương Việt Nam", "com.VCB"),
        BankInfo("970407", "Techcombank", "Ngân hàng Kỹ Thương Việt Nam", "vn.com.techcombank.bb.app"),
        BankInfo("970423", "TPBank", "Ngân hàng Tiên Phong", "com.tpb.mb.gprsandroid"),
        BankInfo("970418", "BIDV", "Ngân hàng Đầu tư và Phát triển Việt Nam", "com.vnpay.bidv"),
        BankInfo("970405", "Agribank", "Ngân hàng Nông nghiệp & PT Nông thôn", "com.vnpay.Agribank3g"),
        BankInfo("970416", "ACB", "Ngân hàng Á Châu", "mobile.acb.com.vn"),
        BankInfo("970432", "VPBank", "Ngân hàng Việt Nam Thịnh Vượng", "com.vnpay.vpbankonline"),
        BankInfo("970448", "OCB", "Ngân hàng Phương Đông", "com.vnpay.ocb"),
        BankInfo("970426", "MSB", "Ngân hàng Hàng Hải", "com.msb.digital"),
        BankInfo("970441", "VIB", "Ngân hàng Quốc tế", "vn.vib.vibbarmobile"),
        BankInfo("970443", "SHB", "Ngân hàng Sài Gòn - Hà Nội", "com.shb.mobilebanking"),
        BankInfo("970403", "Sacombank", "Ngân hàng Sài Gòn Thương Tín", "com.sacombank.ewallet"),
        BankInfo("970415", "VietinBank", "Ngân hàng Công Thương Việt Nam", "com.vietinbank.ipay"),
        BankInfo("970428", "NamABank", "Ngân hàng Nam Á", "namabank.mobilebanking"),
        BankInfo("970437", "HDBank", "Ngân hàng Phát triển TP.HCM", "com.vnpay.hdbank"),
        BankInfo("970454", "VietCapitalBank", "Ngân hàng Bản Việt", "vn.vietcapitalbank.digibank")
    )

    fun findByBin(bin: String): BankInfo? {
        return ALL.firstOrNull { it.bin == bin }
    }

    fun findByNameOrBin(query: String): BankInfo? {
        val trimmed = query.trim().lowercase()
        return ALL.firstOrNull {
            it.bin == trimmed ||
            it.shortName.lowercase() == trimmed ||
            it.fullName.lowercase().contains(trimmed)
        }
    }
}
