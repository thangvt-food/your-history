package com.yourhistory.app.di

import com.yourhistory.app.data.local.AppDatabase
import com.yourhistory.app.data.repository.ExpenseRepository
import com.yourhistory.app.data.repository.ExpenseRepositoryImpl
import com.yourhistory.app.ui.contacts.QrContactsViewModel
import com.yourhistory.app.ui.history.HistoryViewModel
import com.yourhistory.app.ui.home.HomeViewModel
import com.yourhistory.app.ui.scanner.ScannerViewModel
import com.yourhistory.app.ui.transaction.TransactionFormViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { AppDatabase.buildDatabase(androidContext()) }

    // Preferences
    single { com.yourhistory.app.data.local.preferences.UserPreferencesRepository(androidContext()) }

    // Repository
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }

    // ViewModels
    viewModel { HomeViewModel(get()) }
    viewModel { ScannerViewModel() }
    viewModel { TransactionFormViewModel(get(), get()) }
    viewModel { QrContactsViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
}
