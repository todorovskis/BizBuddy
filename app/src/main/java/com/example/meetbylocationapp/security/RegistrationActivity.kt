package com.example.meetbylocationapp.security

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meetbylocationapp.MainActivity
import com.example.meetbylocationapp.R
import com.example.meetbylocationapp.User
import com.example.meetbylocationapp.databinding.ActivityRegistrationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class RegistrationActivity : AppCompatActivity() {

    private var _binding: ActivityRegistrationBinding? = null
    private val binding get() = _binding!!
    private var countries = emptyList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var authManager: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Log.d("ImagePicker", "Image URI: $it")
                binding.registerProfilePicture.setImageURI(it)
                selectedImageUri = it
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        countries = resources.getStringArray(R.array.countries).toList()
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.registerSpinner.adapter = adapter

        binding.registerProfilePicture.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.button.setOnClickListener {
            val email = binding.registerEmail.text.toString()
            val password = binding.registerPassword.text.toString()

            val name = binding.registerName.text.toString()
            val title = binding.registerCardTitle.text.toString()
            val age = binding.registerAge.text.toString().toInt()
            val isChecked = binding.registerDrink.isChecked
            val country = binding.registerSpinner.selectedItem.toString()
            val linkedin = binding.registerLinkedin.text.toString()

            selectedImageUri?.let { uri ->
                lifecycleScope.launch {
                    try {
                        authManager.createUserWithEmailAndPassword(email, password).await()

                        val imageRef = storage.reference.child("profile_pictures").child(email)
                        val inputStream: InputStream? = contentResolver.openInputStream(uri)
                        inputStream?.let {
                            imageRef.putStream(it).await()

                            val imageUrl = imageRef.downloadUrl.await().toString()

                            val likedUsersList = ArrayList<String>()
                            val matchedUsersList = ArrayList<String>()
                            likedUsersList.add("test")
                            matchedUsersList.add("test")

                            val userData = User(
                                name,
                                email,
                                title,
                                age,
                                country,
                                isChecked,
                                imageUrl,
                                latitude = 0.0,
                                longitude = 0.0,
                                online = false,
                                likedUsers = likedUsersList,
                                matchedUsers = matchedUsersList,
                                linkedinUrl = linkedin
                            )

// Store user data in Firebase Realtime Database
                            database.getReference("users/users")
                                .child(sanitizeEmail(email))
                                .setValue(userData)
                                .addOnSuccessListener {
                                    // Successfully stored user data
                                    Toast.makeText(
                                        this@RegistrationActivity,
                                        "Sign up successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    // Handle failure to store user data
                                    Log.e("RegistrationActivity", "Failed to store user data", e)
                                    Toast.makeText(
                                        this@RegistrationActivity,
                                        "Sign up failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        } ?: run {
                            Toast.makeText(
                                this@RegistrationActivity,
                                "Failed to open image stream",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("RegistrationActivity", "Sign up failed", e)
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Sign up failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } ?: run {
                Toast.makeText(
                    this@RegistrationActivity,
                    "Please select a profile picture",
                    Toast.LENGTH_SHORT
                ).show()
            }



        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun sanitizeEmail(email: String): String {
        return email.replace('.', ',')
    }
}
