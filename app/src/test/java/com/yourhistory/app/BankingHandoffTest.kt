package com.yourhistory.app
 
import com.yourhistory.app.domain.handoff.BankingHandoffManager
import com.yourhistory.app.domain.model.VietnameseBanks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
 
class BankingHandoffTest {
 
     @Test
     fun testBuildVietQrPaymentDeeplinkForMBBank() {
         val deeplink = BankingHandoffManager.buildVietQrPaymentDeeplink(
             appId = "mb",
             bankBin = "970422",
             accountNumber = "0123456789",
             amount = 50000L,
             memo = "Tien an trua",
             recipientName = "NGUYEN VAN A"
         )
 
         assertTrue(deeplink.startsWith("https://dl.vietqr.io/pay?"))
         assertTrue(deeplink.contains("app=mb"))
         assertTrue(deeplink.contains("ba=0123456789@970422"))
         assertTrue(deeplink.contains("am=50000"))
         assertTrue(deeplink.contains("tn=Tien+an+trua") || deeplink.contains("tn=Tien%20an%20trua"))
         assertTrue(deeplink.contains("bn=NGUYEN+VAN+A") || deeplink.contains("bn=NGUYEN%20VAN%20A"))
     }
 
     @Test
     fun testVietnameseBanksMapping() {
         val mb = VietnameseBanks.findByBin("970422")
         assertNotNull(mb)
         assertEquals("MBBank", mb!!.shortName)
         assertEquals("com.mbmobile", mb.packageName)
         assertEquals("mb", mb.vietQrAppId)
         assertEquals("mbbank", mb.scheme)
 
         val mbByPkg = VietnameseBanks.findByPackage("com.mbmobile")
         assertNotNull(mbByPkg)
         assertEquals("970422", mbByPkg!!.bin)
 
         val vcb = VietnameseBanks.findByBin("970436")
         assertNotNull(vcb)
         assertEquals("vcb", vcb!!.vietQrAppId)
     }
 
     @Test
     fun testBuildVietQrPaymentDeeplinkWithoutAmountAndRecipient() {
         val deeplink = BankingHandoffManager.buildVietQrPaymentDeeplink(
             appId = "icb",
             bankBin = "970415",
             accountNumber = "9876543210",
             amount = null,
             memo = "Ung ho"
         )
 
         assertTrue(deeplink.contains("app=icb"))
         assertTrue(deeplink.contains("ba=9876543210@970415"))
         assertTrue(!deeplink.contains("am="))
         assertTrue(!deeplink.contains("bn="))
         assertTrue(deeplink.contains("tn=Ung+ho") || deeplink.contains("tn=Ung%20ho"))
     }
}
