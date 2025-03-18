package com.l4kt.kofe.di

import com.l4kt.kofe.data.repositories.CafeRepository
import com.l4kt.kofe.data.repositories.MatchRepository
import com.l4kt.kofe.data.repositories.UserRepository
import com.l4kt.kofe.data.services.FirebaseService
import com.l4kt.kofe.data.services.GoogleMapsService
import com.l4kt.kofe.ui.screens.auth.LoginViewModel
import com.l4kt.kofe.ui.screens.cafes.CafeViewModel
import com.l4kt.kofe.ui.screens.main.MainViewModel
import com.l4kt.kofe.ui.screens.meetup.MeetupViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.l4kt.kofe.BuildConfig


/**
 * Koin module providing app-wide dependencies
 */
val appModule = module {
    // Google Maps API Key - Fetching from BuildConfig
    single { BuildConfig.GOOGLE_MAPS_API_KEY }

    // Services
    single { FirebaseService() }
    single { GoogleMapsService(get()) }

    // Repositories
    single { UserRepository(get()) }
    single { MatchRepository(get(), get()) }
    single { CafeRepository(get(), get()) }

    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { CafeViewModel(get()) }
    viewModel { MeetupViewModel(get(), get()) }
}