package com.example.meetbylocationapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.meetbylocationapp.databinding.FragmentDialogBinding

class DialogFragment : Fragment() {

    private var _binding: FragmentDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDialogBinding.inflate(inflater, container, false)

        var matchText: TextView = binding.dialogMatchText
        var matchMessage: TextView = binding.dialogMatchMessage
        var linkedinButton: Button = binding.dialogLinkedinButton

        val args = arguments
        val matchedUserName = args?.getString("MATCHED_USER_NAME") ?: ""
        val linkedInUrl = args?.getString("LINKEDIN_URL") ?: ""

        matchMessage.text = "This user would like to meet you too."

        linkedinButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl))
            startActivity(intent)
        }

        return binding.root
    }


}