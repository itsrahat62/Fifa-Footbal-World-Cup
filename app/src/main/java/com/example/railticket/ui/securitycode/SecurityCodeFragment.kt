package com.example.railticket.ui.securitycode

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.railticket.R
import com.example.railticket.data.UserManager
import com.example.railticket.databinding.FragmentSecurityCodeBinding
import com.example.railticket.util.SecurityCodeValidator
import java.util.Locale

class SecurityCodeFragment : Fragment() {

    private var _binding: FragmentSecurityCodeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SecurityCodeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, SecurityCodeViewModelFactory())[SecurityCodeViewModel::class.java]

        binding.submitSecurityCodeButton.setOnClickListener {
            handleSubmit()
        }
    }

    private fun handleSubmit() {
        val enteredCode = binding.securityCodeEdittext.text.toString().trim().uppercase(Locale.ROOT)
        if (enteredCode.isEmpty()) {
            binding.securityCodeLayout.error = "Please enter the security code."
            return
        }

        binding.securityCodeLayout.error = null

        val mobileNumber = UserManager.mobileNumber
        if (mobileNumber == null) {
            Toast.makeText(context, "Error: Could not find mobile number. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val correctCode = SecurityCodeValidator.generateCode(mobileNumber)

        Log.d("SecurityCodeFragment", "Entered Code: $enteredCode")
        Log.d("SecurityCodeFragment", "Correct Code for $mobileNumber is: $correctCode")

        if (enteredCode == correctCode) {
            Toast.makeText(context, "Security code verified!", Toast.LENGTH_SHORT).show()
            if (isAdded) {
                // Using the new, renamed action ID to fix the build error
                findNavController().navigate(R.id.action_security_to_search)
            }
        } else {
            Toast.makeText(context, "Invalid security code. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
