package com.yourhistory.app.domain.model

data class BankInfo(
    val bin: String,
    val shortName: String,
    val fullName: String,
    val packageName: String? = null,
    val vietQrAppId: String? = null,
    val scheme: String? = null
)

object VietnameseBanks {
    val ALL = listOf(
        BankInfo("970422", "MBBank", "Ngân hàng TMCP Quân Đội", "com.mbmobile", "mb", "mbbank"),
        BankInfo("970436", "Vietcombank", "Ngân hàng Ngoại thương Việt Nam", "com.VCB", "vcb", "vietcombank"),
        BankInfo("970407", "Techcombank", "Ngân hàng Kỹ Thương Việt Nam", "vn.com.techcombank.bb.app", "tcb", "techcombank"),
        BankInfo("970423", "TPBank", "Ngân hàng Tiên Phong", "com.tpb.mb.gprsandroid", "tpb", "tpbank"),
        BankInfo("970418", "BIDV", "Ngân hàng Đầu tư và Phát triển Việt Nam", "com.vnpay.bidv", "bidv", "bidv"),
        BankInfo("970405", "Agribank", "Ngân hàng Nông nghiệp & PT Nông thôn", "com.vnpay.Agribank3g", "vba", "agribank"),
        BankInfo("970416", "ACB", "Ngân hàng Á Châu", "mobile.acb.com.vn", "acb", "acbone"),
        BankInfo("970432", "VPBank", "Ngân hàng Việt Nam Thịnh Vượng", "com.vnpay.vpbankonline", "vpb", "vpbankneo"),
        BankInfo("970448", "OCB", "Ngân hàng Phương Đông", "com.vnpay.ocb", "ocb", "ocbomni"),
        BankInfo("970426", "MSB", "Ngân hàng Hàng Hải", "com.msb.digital", "msb", "msbmobile"),
        BankInfo("970441", "VIB", "Ngân hàng Quốc tế", "vn.vib.vibbarmobile", "vib", "myvib"),
        BankInfo("970443", "SHB", "Ngân hàng Sài Gòn - Hà Nội", "com.shb.mobilebanking", "shb", "shbsaha"),
        BankInfo("970403", "Sacombank", "Ngân hàng Sài Gòn Thương Tín", "com.sacombank.ewallet", "scb", null),
        BankInfo("970415", "VietinBank", "Ngân hàng Công Thương Việt Nam", "com.vietinbank.ipay", "icb", "vietinbankipay"),
        BankInfo("970428", "NamABank", "Ngân hàng Nam Á", "namabank.mobilebanking", "nab", "namabank"),
        BankInfo("970437", "HDBank", "Ngân hàng Phát triển TP.HCM", "com.vnpay.hdbank", "hdb", "hdbank"),
        BankInfo("970454", "VietCapitalBank", "Ngân hàng Bản Việt", "vn.vietcapitalbank.digibank", "timo", "timo")
    )

    fun findByBin(bin: String): BankInfo? {
        return ALL.firstOrNull { it.bin == bin }
    }

    fun findByPackage(packageName: String): BankInfo? {
        return ALL.firstOrNull { it.packageName == packageName }
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
