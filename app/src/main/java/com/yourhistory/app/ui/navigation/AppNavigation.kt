package com.yourhistory.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yourhistory.app.ui.contacts.QrContactsScreen
import com.yourhistory.app.ui.contacts.QrContactsViewModel
import com.yourhistory.app.ui.history.HistoryScreen
import com.yourhistory.app.ui.history.HistoryViewModel
import com.yourhistory.app.ui.home.HomeScreen
import com.yourhistory.app.ui.home.HomeViewModel
import com.yourhistory.app.ui.scanner.QrScannerScreen
import com.yourhistory.app.ui.scanner.ScannerViewModel
import com.yourhistory.app.ui.showqr.ShowQrScreen
import com.yourhistory.app.ui.transaction.TransactionFormScreen
import com.yourhistory.app.ui.transaction.TransactionFormViewModel
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Màn hình Trang chủ
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = koinViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToScanner = {
                    navController.navigate(Screen.Scanner.route)
                },
                onNavigateToManualTransaction = {
                    navController.navigate(Screen.TransactionForm.createRoute())
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onContactClick = { contact ->
                    navController.navigate(
                        Screen.TransactionForm.createRoute(
                            bankBin = contact.bankBin,
                            account = contact.accountNumber,
                            amount = contact.defaultAmount,
                            memo = contact.defaultNote,
                            recipient = contact.recipientName,
                            contactId = contact.id
                        )
                    )
                }
            )
        }

        // Màn hình Quét VietQR
        composable(Screen.Scanner.route) {
            val scannerViewModel: ScannerViewModel = koinViewModel()
            QrScannerScreen(
                viewModel = scannerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onQrScanned = { qrData ->
                    navController.navigate(
                        Screen.TransactionForm.createRoute(
                            bankBin = qrData.bankBin,
                            account = qrData.accountNumber,
                            amount = qrData.amount,
                            memo = qrData.memo,
                            recipient = qrData.recipientName ?: ""
                        )
                    ) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Nhập chi tiêu / Form chuyển khoản
        composable(
            route = Screen.TransactionForm.route,
            arguments = listOf(
                navArgument("bankBin") { type = NavType.StringType; defaultValue = "" },
                navArgument("account") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.StringType; defaultValue = "" },
                navArgument("memo") { type = NavType.StringType; defaultValue = "" },
                navArgument("recipient") { type = NavType.StringType; defaultValue = "" },
                navArgument("contactId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val bankBin = backStackEntry.arguments?.getString("bankBin") ?: ""
            val account = backStackEntry.arguments?.getString("account") ?: ""
            val amountStr = backStackEntry.arguments?.getString("amount") ?: ""
            val amount = amountStr.toLongOrNull()
            val rawMemo = backStackEntry.arguments?.getString("memo") ?: ""
            val memo = try { URLDecoder.decode(rawMemo, "UTF-8") } catch (e: Exception) { rawMemo }
            val rawRecipient = backStackEntry.arguments?.getString("recipient") ?: ""
            val recipient = try { URLDecoder.decode(rawRecipient, "UTF-8") } catch (e: Exception) { rawRecipient }
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""

            val transactionViewModel: TransactionFormViewModel = koinViewModel()
            TransactionFormScreen(
                viewModel = transactionViewModel,
                bankBin = bankBin,
                accountNumber = account,
                amount = amount,
                memo = memo,
                recipient = recipient,
                contactId = contactId,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onShowQr = { payload, qrBankBin, qrBankName, qrAccount, qrAmount, qrMemo, senderPkg, senderName, imgSaved ->
                    navController.navigate(
                        Screen.ShowQr.createRoute(
                            payload = payload,
                            bankBin = qrBankBin,
                            bankName = qrBankName,
                            account = qrAccount,
                            amount = qrAmount,
                            memo = qrMemo,
                            senderPkg = senderPkg,
                            senderName = senderName,
                            imageSaved = imgSaved
                        )
                    )
                }
            )
        }

        // Màn hình chuyển nhanh: ảnh QR đã lưu + mở app bank quét từ ảnh
        composable(
            route = Screen.ShowQr.route,
            arguments = listOf(
                navArgument("payload") { type = NavType.StringType; defaultValue = "" },
                navArgument("bankBin") { type = NavType.StringType; defaultValue = "" },
                navArgument("bankName") { type = NavType.StringType; defaultValue = "" },
                navArgument("account") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.StringType; defaultValue = "" },
                navArgument("memo") { type = NavType.StringType; defaultValue = "" },
                navArgument("senderPkg") { type = NavType.StringType; defaultValue = "" },
                navArgument("senderName") { type = NavType.StringType; defaultValue = "" },
                navArgument("imgSaved") { type = NavType.StringType; defaultValue = "0" }
            )
        ) { backStackEntry ->
            fun dec(s: String?) = try { URLDecoder.decode(s ?: "", "UTF-8") } catch (e: Exception) { s ?: "" }
            val payload = dec(backStackEntry.arguments?.getString("payload"))
            val qrBankBin = backStackEntry.arguments?.getString("bankBin") ?: ""
            val qrBankName = dec(backStackEntry.arguments?.getString("bankName"))
            val qrAccount = backStackEntry.arguments?.getString("account") ?: ""
            val qrAmount = backStackEntry.arguments?.getString("amount")?.toLongOrNull()
            val qrMemo = dec(backStackEntry.arguments?.getString("memo"))
            val senderPkgRaw = dec(backStackEntry.arguments?.getString("senderPkg"))
            val senderPkg = senderPkgRaw.ifBlank { null }
            val senderName = dec(backStackEntry.arguments?.getString("senderName"))
            val imgSaved = backStackEntry.arguments?.getString("imgSaved") == "1"

            ShowQrScreen(
                emvPayload = payload,
                bankBin = qrBankBin,
                bankName = qrBankName,
                accountNumber = qrAccount,
                amount = qrAmount,
                memo = qrMemo,
                senderPackage = senderPkg,
                senderBankName = senderName,
                imageSaved = imgSaved,
                onNavigateBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Danh bạ QR
        composable(Screen.Contacts.route) {
            val contactsViewModel: QrContactsViewModel = koinViewModel()
            QrContactsScreen(
                viewModel = contactsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onContactSelected = { contact ->
                    navController.navigate(
                        Screen.TransactionForm.createRoute(
                            bankBin = contact.bankBin,
                            account = contact.accountNumber,
                            amount = contact.defaultAmount,
                            memo = contact.defaultNote,
                            recipient = contact.recipientName,
                            contactId = contact.id
                        )
                    )
                }
            )
        }

        // Màn hình Lịch sử giao dịch
        composable(Screen.History.route) {
            val historyViewModel: HistoryViewModel = koinViewModel()
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
