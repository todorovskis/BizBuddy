package com.example.meetbylocationapp

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.meetbylocationapp.databinding.FragmentProfilesBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import com.squareup.picasso.Picasso
import kotlin.math.*

class ProfilesFragment : Fragment() {

    private var _binding: FragmentProfilesBinding? = null
    private val binding get() = _binding!!

    private val profiles = mutableListOf<User>()
    private lateinit var database: DatabaseReference
    private lateinit var arrayAdapter: ArrayAdapter<User>

    private var currentUserLat: Double = 0.0
    private var currentUserLon: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().getReference("users")
        getCurrentLocation()

        arrayAdapter = object : ArrayAdapter<User>(
            requireContext(),
            R.layout.profile_card,
            profiles
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view =
                    convertView ?: layoutInflater.inflate(R.layout.profile_card, parent, false)

                val user = getItem(position)
                val nameTextView = view.findViewById<TextView>(R.id.card_name)
                val ageTextView = view.findViewById<TextView>(R.id.card_age)
                val jobTitleTextView = view.findViewById<TextView>(R.id.card_title)
                val countryTextView = view.findViewById<TextView>(R.id.card_country)
                val upForADrinkTextView = view.findViewById<TextView>(R.id.card_upForADrink)
                val distanceTextView = view.findViewById<TextView>(R.id.card_distance)
                val onlineStatusTextView = view.findViewById<TextView>(R.id.card_online_status)
                val profileImageView = view.findViewById<ImageView>(R.id.card_image)

                nameTextView.text = user?.name ?: "Name not available"
                ageTextView.text = user?.age.toString()
                jobTitleTextView.text = user?.title ?: "Title not available"
                countryTextView.text = user?.country ?: "Country not available"
                upForADrinkTextView.text =
                    if (user?.upForADrink == true) "Up for a drink \uD83C\uDF7A" else ""

                val userLat = user?.latitude ?: 0.0
                val userLon = user?.longitude ?: 0.0
                val distance = haversine(currentUserLat, currentUserLon, userLat, userLon)
                distanceTextView.text = "${distance.toInt()} km away"

                onlineStatusTextView.text = if (user?.online == true) "Online" else "Offline"

                val profileImageUrl = user?.profileImageUrl

                if (!profileImageUrl.isNullOrEmpty()) {
                    Picasso.get().load(profileImageUrl).into(profileImageView)
                } else {
                    Log.e("Picasso", "Profile image URL is empty or null")
                }

                return view
            }
        }

        binding.frame.adapter = arrayAdapter
        binding.frame.setFlingListener(object : SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                profiles.removeAt(0)
                arrayAdapter.notifyDataSetChanged()
            }

            override fun onLeftCardExit(dataObject: Any) {

            }

            override fun onRightCardExit(dataObject: Any) {
                if (dataObject is User) {
                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
                    val targetUserEmail = dataObject.email

                    if (currentUserEmail != null && targetUserEmail != null) {
                        val sanitizedCurrentUserEmail = sanitizeEmail(currentUserEmail)
                        val sanitizedTargetUserEmail = sanitizeEmail(targetUserEmail)

                        val usersRef = database

                        usersRef.child(sanitizedCurrentUserEmail)
                            .child("likedUsers")
                            .push()
                            .setValue(sanitizedTargetUserEmail)
                            .addOnSuccessListener {
                                // Check if target user already liked the current user
                                usersRef.child(sanitizedTargetUserEmail)
                                    .child("likedUsers")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val targetUserLikedUsers = mutableListOf<String>()

                                            if (snapshot.value is List<*>) {
                                                targetUserLikedUsers.addAll(snapshot.children.mapNotNull { it.getValue(String::class.java) })
                                            } else if (snapshot.value is HashMap<*, *>) {
                                                for (childSnapshot in snapshot.children) {
                                                    val likedUser = childSnapshot.getValue(String::class.java)
                                                    likedUser?.let { targetUserLikedUsers.add(it) }
                                                }
                                            }

                                            if (targetUserLikedUsers.contains(sanitizedCurrentUserEmail)) {
                                                usersRef.child(sanitizedCurrentUserEmail)
                                                    .child("matchedUsers")
                                                    .push()
                                                    .setValue(sanitizedTargetUserEmail)
                                                    .addOnSuccessListener {
                                                        usersRef.child(sanitizedTargetUserEmail)
                                                            .child("matchedUsers")
                                                            .push()
                                                            .setValue(sanitizedCurrentUserEmail)
                                                            .addOnSuccessListener {
                                                                // Show match dialog
                                                                val dialogFragment =
                                                                    DialogFragment().apply {
                                                                        arguments = Bundle().apply {
                                                                            putString(
                                                                                "MATCHED_USER_NAME",
                                                                                dataObject.name
                                                                            )
                                                                            putString(
                                                                                "LINKEDIN_URL",
                                                                                dataObject.linkedinUrl
                                                                            )
                                                                        }
                                                                    }
                                                                val transaction =
                                                                    childFragmentManager.beginTransaction()
                                                                transaction.replace(
                                                                    R.id.fragment_container,
                                                                    dialogFragment,
                                                                    "MatchDialogFragment"
                                                                )
                                                                transaction.addToBackStack(null)
                                                                transaction.commit()
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e(
                                                                    TAG,
                                                                    "Error updating target user's matchedUsers: $e"
                                                                )
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e(
                                                            TAG,
                                                            "Error updating current user's matchedUsers: $e"
                                                        )
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e(TAG, "Database read canceled: $error")
                                        }
                                    })
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error updating current user's likedUsers: $e")
                            }
                    }
                }
            }



            private fun sanitizeEmail(email: String): String {
                return email.replace(
                    '.',
                    ','
                )
            }

            override fun onAdapterAboutToEmpty(itemsInAdapter: Int) {

            }

            override fun onScroll(scrollProgressPercent: Float) {

            }
        })

        fetchUsers()
    }

    private fun fetchUsers() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                profiles.clear()

                for (userSnapshot in dataSnapshot.children) {
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: ""

                    if (email == currentUserEmail) {
                        continue
                    }

                    val age = userSnapshot.child("age").getValue(Int::class.java) ?: 0
                    val country = userSnapshot.child("country").getValue(String::class.java) ?: ""
                    val latitude =
                        userSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val linkedinUrl =
                        userSnapshot.child("linkedinUrl").getValue(String::class.java) ?: ""
                    val longitude =
                        userSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                    val online = userSnapshot.child("online").getValue(Boolean::class.java) ?: false
                    val profileImageUrl =
                        userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                    val title = userSnapshot.child("title").getValue(String::class.java) ?: ""
                    val upForADrink =
                        userSnapshot.child("upForADrink").getValue(Boolean::class.java) ?: false

                    val likedUsersSnapshot = userSnapshot.child("likedUsers")
                    val likedUsers: ArrayList<String> = if (likedUsersSnapshot.exists()) {
                        val list = arrayListOf<String>()
                        if (likedUsersSnapshot.value is List<*>) {
                            list.addAll(likedUsersSnapshot.children.mapNotNull { it.getValue(String::class.java) })
                        } else if (likedUsersSnapshot.value is Map<*, *>) {
                            list.addAll(likedUsersSnapshot.children.mapNotNull { it.getValue(String::class.java) })
                        }
                        list
                    } else {
                        arrayListOf()
                    }

                    val matchedUsersSnapshot = userSnapshot.child("matchedUsers")
                    val matchedUsers: ArrayList<String> = if (matchedUsersSnapshot.exists()) {
                        val list = arrayListOf<String>()
                        if (matchedUsersSnapshot.value is List<*>) {
                            list.addAll(matchedUsersSnapshot.children.mapNotNull {
                                it.getValue(
                                    String::class.java
                                )
                            })
                        } else if (matchedUsersSnapshot.value is Map<*, *>) {
                            list.addAll(matchedUsersSnapshot.children.mapNotNull {
                                it.getValue(
                                    String::class.java
                                )
                            })
                        }
                        list
                    } else {
                        arrayListOf()
                    }

                    val user = User(
                        age = age,
                        country = country,
                        email = email,
                        latitude = latitude,
                        likedUsers = likedUsers,
                        linkedinUrl = linkedinUrl,
                        longitude = longitude,
                        matchedUsers = matchedUsers,
                        name = name,
                        online = online,
                        profileImageUrl = profileImageUrl,
                        title = title,
                        upForADrink = upForADrink
                    )
                    profiles.add(user)
                }
                arrayAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fetchUsers", "Database read cancelled: $error")
            }
        })
    }


    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
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
                currentUserLat = currentLat
                currentUserLon = currentLon
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
