package com.example.railticket.ui.booking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.railticket.databinding.FragmentOtpVerificationBinding
import com.example.railticket.ui.login.LoginViewModel
import com.example.railticket.ui.login.LoginViewModelFactory
import com.example.railticket.ui.login.VerifyOtpResult

class OtpVerificationFragment : Fragment() {

    private var _binding: FragmentOtpVerificationBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels { LoginViewModelFactory() }
    private val args: OtpVerificationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("OtpVerificationFragment", "Fragment created. Received arguments:")
        Log.d("OtpVerificationFragment", "Trip ID: ${args.tripId}")
        Log.d("OtpVerificationFragment", "Trip Route ID: ${args.tripRouteId}")
        Log.d("OtpVerificationFragment", "Boarding Point ID: ${args.boardingPointId}")
        // Log new arguments received by OtpVerificationFragment
        Log.d("OtpVerificationFragment", "From City: ${args.fromCity}")
        Log.d("OtpVerificationFragment", "To City: ${args.toCity}")
        Log.d("OtpVerificationFragment", "Date of Journey: ${args.dateOfJourney}")
        Log.d("OtpVerificationFragment", "Seat Class: ${args.seatClass}")

        binding.buttonVerifyOtp.setOnClickListener {
            val otpInput = binding.editTextOtp.text.toString().trim()
            if (otpInput.isNotEmpty()) {
                loginViewModel.verifyOtp(
                    otp = otpInput,
                    tripIdStr = args.tripId,
                    tripRouteIdStr = args.tripRouteId,
                    ticketIdsStr = args.ticketIds.toList(),
                    boardingPointId = args.boardingPointId
                    // fromCity, toCity etc. are not directly used by verifyOtp logic itself,
                    // but are needed for navigation to the next step.
                )
            } else {
                binding.otpInputLayout.error = "Please enter OTP"
            }
        }

        loginViewModel.verifyOtpResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result: VerifyOtpResult ->
                if (result.error != null) {
                    Toast.makeText(requireContext(), "OTP Verification Failed: ${result.error}", Toast.LENGTH_LONG).show()
                    binding.otpInputLayout.error = result.error
                }
            }
        }

        loginViewModel.navigateToPassengerDetails.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { navData ->
                Toast.makeText(requireContext(), "OTP Verified! Navigating to passenger details...", Toast.LENGTH_SHORT).show()
                
                val action = OtpVerificationFragmentDirections.actionOtpVerificationFragmentToPassengerDetailsFragment(
                    userName = navData.userName,
                    userEmail = navData.userEmail,
                    userMobile = navData.userMobile,
                    tripId = navData.tripId,
                    tripRouteId = navData.tripRouteId,
                    ticketIds = navData.ticketIds,
                    sessionAuthToken = navData.sessionAuthToken,
                    submittedOtp = navData.submittedOtp,
                    numberOfSeats = args.numberOfSeats,
                    boardingPointId = navData.boardingPointId,
                    // Pass the new arguments received by OtpVerificationFragment
                    fromCity = args.fromCity,
                    toCity = args.toCity,
                    dateOfJourney = args.dateOfJourney,
                    seatClass = args.seatClass
                )
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
