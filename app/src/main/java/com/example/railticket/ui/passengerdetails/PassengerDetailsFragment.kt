package com.example.railticket.ui.passengerdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.railticket.R
import com.example.railticket.data.network.RetrofitInstance
import com.example.railticket.data.repository.BookingRepository
import com.example.railticket.databinding.FragmentPassengerDetailsBinding
import com.example.railticket.databinding.ItemPassengerFormBinding
import com.example.railticket.util.Event

class PassengerDetailsFragment : Fragment() {

    private var _binding: FragmentPassengerDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PassengerDetailsViewModel
    private val args: PassengerDetailsFragmentArgs by navArgs()

    private val passengerForms = mutableListOf<ItemPassengerFormBinding>()

    private val deviceKey = "114e6a31e406bf79f2efa4ea722293f7a477112801cf30f7422790a7733d487185cf02f8031aa577e0ca4f0c16563c30586808a50e00beb1d3331b2c949e5b254ea8e0a767e5b2a46bc6a9036757c3ba"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val realBookingService = RetrofitInstance.bookingService
        val bookingRepository = BookingRepository(realBookingService)
        
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "mock_device_id_fallback"

        val factory = PassengerDetailsViewModelFactory(bookingRepository, deviceId)
        viewModel = ViewModelProvider(this, factory)[PassengerDetailsViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.contactNameEdittext.setText(args.userName)
        binding.contactEmailEdittext.setText(args.userEmail)
        binding.contactMobileEdittext.setText(args.userMobile)

        binding.passengerFormsContainer.removeAllViews()
        passengerForms.clear()

        val numberOfActualSeats = args.ticketIds.size
        if (numberOfActualSeats <= 0) {
            binding.passengersSectionTitle.isVisible = false
            Toast.makeText(context, "Error: No tickets selected.", Toast.LENGTH_LONG).show()
            binding.submitPassengerDetailsButton.isEnabled = false
            return
        }

        binding.passengersSectionTitle.text = if (numberOfActualSeats == 1) getString(R.string.passenger_details_title) else getString(R.string.passengers_details_title_plural, numberOfActualSeats)

        val passengerTypesArray = resources.getStringArray(R.array.passenger_types)
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, passengerTypesArray)

        for (i in 0 until numberOfActualSeats) { 
            val passengerFormBinding = ItemPassengerFormBinding.inflate(LayoutInflater.from(context), binding.passengerFormsContainer, false)
            passengerFormBinding.passengerHeaderTextView.text = getString(R.string.passenger_header_template, i + 1)

            if (i == 0) {
                passengerFormBinding.passengerNameEditText.setText(args.userName) 
                passengerFormBinding.passengerNameEditText.isEnabled = false
            }
            
            passengerFormBinding.genderMaleRadiobutton.isChecked = true 

            passengerFormBinding.passengerTypeAutocomplete.setAdapter(typeAdapter)
            if (passengerTypesArray.isNotEmpty()) {
                passengerFormBinding.passengerTypeAutocomplete.setText(passengerTypesArray[0], false) 
            }

            passengerForms.add(passengerFormBinding)
            binding.passengerFormsContainer.addView(passengerFormBinding.root)
        }

        binding.submitPassengerDetailsButton.setOnClickListener {
            handleSubmit()
        }
    }

    private fun handleSubmit() {
        val passengerNames = mutableListOf<String>()
        val passengerTypes = mutableListOf<String>()
        val genders = mutableListOf<String>()
        var allFormsValid = true

        for ((index, formBinding) in passengerForms.withIndex()) {
            val name = if (index == 0) args.userName else formBinding.passengerNameEditText.text.toString().trim()
            if (name.isEmpty()) {
                formBinding.passengerNameLayout.error = getString(R.string.required_field)
                allFormsValid = false
            } else {
                formBinding.passengerNameLayout.error = null
            }
            passengerNames.add(name)

            val type = formBinding.passengerTypeAutocomplete.text.toString()
            if (type.isEmpty()) {
                formBinding.passengerTypeLayout.error = getString(R.string.required_field)
                allFormsValid = false
            } else {
                formBinding.passengerTypeLayout.error = null
            }
            passengerTypes.add(type)

            val selectedGenderId = formBinding.genderRadiogroup.checkedRadioButtonId
            if (selectedGenderId == -1) {
                Toast.makeText(context, getString(R.string.error_select_gender_for_passenger, index + 1), Toast.LENGTH_SHORT).show()
                allFormsValid = false
            } else {
                val selectedRadioButton: RadioButton = formBinding.genderRadiogroup.findViewById(selectedGenderId)
                genders.add(selectedRadioButton.text.toString().lowercase()) 
            }
        }

        if (!allFormsValid) {
            Toast.makeText(context, R.string.error_fill_all_passenger_details, Toast.LENGTH_LONG).show()
            return
        }
        
        if (args.submittedOtp.isBlank()) { 
             Toast.makeText(context, "Error: Valid OTP not found. Please verify OTP again.", Toast.LENGTH_LONG).show()
             return
        }

        val isBkash = binding.paymentMethodRadiogroup.checkedRadioButtonId == R.id.radio_button_bkash
        val selectedMobileTransaction = if (isBkash) "1" else "3"
        val isBkashOnline = isBkash // This will be true for bKash, false for Nagad
        
        val ticketIdsAsLongs = args.ticketIds.map { it.toLong() }

        viewModel.confirmBooking(
            tripId = args.tripId.toLong(),
            tripRouteId = args.tripRouteId.toLong(),
            ticketIds = ticketIdsAsLongs,
            boardingPointId = args.boardingPointId.toLong(),
            passengerTypes = passengerTypes,
            contactEmail = args.userEmail,
            contactMobile = args.userMobile,
            passengerNames = passengerNames,
            genders = genders,
            selectedMobileTransaction = selectedMobileTransaction,
            otp = args.submittedOtp,
            isBkashOnline = isBkashOnline, // Pass the correct boolean value
            authToken = args.sessionAuthToken,
            deviceKey = deviceKey
        )
    }

    private fun observeViewModel() {
        viewModel.bookingConfirmationState.observe(viewLifecycleOwner, Observer { state ->
            binding.passengerDetailsProgressBar.isVisible = state.isLoading
            binding.submitPassengerDetailsButton.isEnabled = !state.isLoading

            state.error?.getContentIfNotHandled()?.let { errorMsg ->
                Toast.makeText(context, "Booking Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
            state.successUrl?.getContentIfNotHandled()?.let { redirectUrl ->
                Toast.makeText(context, "${args.ticketIds.size} ticket reserve successful", Toast.LENGTH_LONG).show()
                handleBookingSuccess(redirectUrl)
            }
        })
    }
    
    private fun handleBookingSuccess(redirectUrl: String) {
        if (redirectUrl.isBlank()) {
            Toast.makeText(context, "Received an empty redirect URL from server.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "No application can handle this request. Please install a web browser.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening redirect URL: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.passengerFormsContainer.removeAllViews()
        passengerForms.clear()
        _binding = null
    }
}
