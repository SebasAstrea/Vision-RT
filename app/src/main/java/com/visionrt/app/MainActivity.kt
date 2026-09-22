package com.visionrt.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity navigation host (ARCHITECTURE.md §7, app module). The app
 * shell nav graph lives in :feature; MainActivity only hosts it. Routing
 * (first-run onboarding vs. returning-home) happens inside the feature.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpNavigationHost()
    }

    private fun setUpNavigationHost() {
        if (supportFragmentManager.findFragmentById(R.id.nav_host) != null) {
            return
        }
        val navHostFragment =
            NavHostFragment.create(com.visionrt.feature.R.navigation.nav_home)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commitNow()
        navController = navHostFragment.navController
    }
}
