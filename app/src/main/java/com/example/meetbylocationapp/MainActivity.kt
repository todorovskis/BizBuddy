package com.example.meetbylocationapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.meetbylocationapp.databinding.ActivityMainBinding
import com.example.meetbylocationapp.security.AuthManager
import com.example.meetbylocationapp.security.FirebaseAuthManager
import com.example.meetbylocationapp.security.LoginActivity
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authManager: AuthManager = FirebaseAuthManager()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance().getReference("users/users")

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_logout -> {
                    lifecycleScope.launch {
                        logoutAndNavigateToLogin()
                    }
                    true
                }

                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        getCurrentLocationAndUpdateStatus()
    }

    private suspend fun logoutAndNavigateToLogin() {
        authManager.logout()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateUserLocationAndStatus(
        email: String,
        lat: Double,
        lon: Double,
        online: Boolean
    ) {
        val updates = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "online" to online
        )
        database.child(sanitizeEmail(email))
            .updateChildren(updates)
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace(
            '.',
            ','
        )
    }

    private fun getCurrentLocationAndUpdateStatus() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLat = it.latitude
                val currentLon = it.longitude
                val email =
                    FirebaseAuth.getInstance().currentUser?.email ?: return@addOnSuccessListener
                updateUserLocationAndStatus(email, currentLat, currentLon, true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getCurrentLocationAndUpdateStatus()
    }

    override fun onStop() {
        super.onStop()
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        updateUserLocationAndStatus(email, 0.0, 0.0, false)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
