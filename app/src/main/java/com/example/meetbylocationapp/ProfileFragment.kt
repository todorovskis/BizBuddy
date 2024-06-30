package com.example.meetbylocationapp.core.domain

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.meetbylocationapp.R
import com.example.meetbylocationapp.User
import com.example.meetbylocationapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private var countries = emptyList<String>()
    private var databaseReference: DatabaseReference? = null
    private var storageReference: StorageReference? = null
    private var userId: String? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        countries = resources.getStringArray(R.array.countries).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCountry.adapter = adapter

        userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId!!)
        storageReference = FirebaseStorage.getInstance().getReference("profile_images").child(userId!!)

        loadUserData()

        binding.buttonUploadImage.setOnClickListener { openFileChooser() }
        binding.buttonSave.setOnClickListener { saveUserData() }
    }

    private fun loadUserData() {
        databaseReference?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                user?.let {
                    binding.editTextName.setText(it.name)
                    binding.editTextEmail.setText(it.email)
                    binding.editTextTitle.setText(it.title)
                    binding.editTextAge.setText(it.age.toString())
                    binding.checkBoxUpForADrink.isChecked = it.upForADrink == true

                    val countryIndex = countries.indexOf(it.country)
                    if (countryIndex != -1) {
                        binding.spinnerCountry.setSelection(countryIndex)
                    }

                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(it.profileImageUrl)
                            .into(binding.profileImage)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            // Set the image in ImageView or upload to Firebase Storage
            uploadImageToFirebase(imageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = storageReference?.child("profile_image.jpg")
        storageRef?.putFile(imageUri)
            ?.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    // Update user profile with the image URL
                    databaseReference?.child("profileImageUrl")?.setValue(uri.toString())
                        ?.addOnSuccessListener {
                            Glide.with(requireContext())
                                .load(uri)
                                .into(binding.profileImage)
                        }
                }
            }
            ?.addOnFailureListener { e ->
                // Handle unsuccessful upload
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val title = binding.editTextTitle.text.toString().trim()
        val age = binding.editTextAge.text.toString().toIntOrNull() ?: 0
        val country = countries[binding.spinnerCountry.selectedItemPosition]
        val upForADrink = binding.checkBoxUpForADrink.isChecked

//        val user = User(name = name, email = email, title = title, age = age, country = country, upForADrink = upForADrink)

//        databaseReference?.setValue(user)
//            ?.addOnSuccessListener {
//                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
//            }
//            ?.addOnFailureListener { e ->
//                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
    }
}
