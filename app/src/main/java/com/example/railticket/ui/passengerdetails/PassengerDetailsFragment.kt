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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.railticket.R // Assuming this is your R file
import com.example.railticket.data.network.RetrofitInstance // Import the real RetrofitInstance
import com.example.railticket.data.repository.BookingRepository
import com.example.railticket.databinding.FragmentPassengerDetailsBinding
import com.example.railticket.databinding.ItemPassengerFormBinding

class PassengerDetailsFragment : Fragment() {

    private var _binding: FragmentPassengerDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PassengerDetailsViewModel
    private val args: PassengerDetailsFragmentArgs by navArgs()

    private val passengerForms = mutableListOf<ItemPassengerFormBinding>()

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
        
        val appVersion = "1.0.0" // TODO: Replace with actual app version from BuildConfig or constants
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "mock_device_id_fallback"

        val factory = PassengerDetailsViewModelFactory(bookingRepository, appVersion, deviceId)
        viewModel = ViewModelProvider(this, factory)[PassengerDetailsViewModel::class.java]

        // Log arguments received by PassengerDetailsFragment
        Log.d("PassengerDetailsFrag", "Arguments received:")
        Log.d("PassengerDetailsFrag", "  From City: ${args.fromCity}")
        Log.d("PassengerDetailsFrag", "  To City: ${args.toCity}")
        Log.d("PassengerDetailsFrag", "  Date of Journey: ${args.dateOfJourney}")
        Log.d("PassengerDetailsFrag", "  Seat Class: ${args.seatClass}")

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.contactNameEdittext.setText(args.userName)
        binding.contactEmailEdittext.setText(args.userEmail)
        binding.contactMobileEdittext.setText(args.userMobile)

        // Set default payment method (Nagad is checked by default in XML)
        // binding.paymentMethodRadiogroup.check(R.id.radio_button_nagad) // Already done in XML

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
            val name: String
            if (index == 0) {
                name = args.userName
            } else {
                name = formBinding.passengerNameEditText.text.toString().trim()
                if (name.isEmpty()) {
                    formBinding.passengerNameLayout.error = getString(R.string.required_field)
                    allFormsValid = false
                } else {
                    formBinding.passengerNameLayout.error = null
                }
            }
            passengerNames.add(name)

            val type = formBinding.passengerTypeAutocomplete.text.toString()
            if (type.isEmpty()) {
                formBinding.passengerTypeLayout.error = getString(R.string.required_field)
                allFormsValid = false
            } else {
                formBinding.passengerTypeLayout.error = null
                passengerTypes.add(type)
            }

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

        // Payment method specific parameters
        val isBkashOnline: Boolean? 
        val selectedMobileTransaction: Int?

        when (binding.paymentMethodRadiogroup.checkedRadioButtonId) {
            R.id.radio_button_bkash -> {
                isBkashOnline = true
                selectedMobileTransaction = 1
                Log.d("PassengerDetailsFrag", "Bkash selected. isBkashOnline: true, selectedMobileTransaction: 1")
            }
            R.id.radio_button_nagad -> {
                isBkashOnline = false // Updated requirement for Nagad
                selectedMobileTransaction = 3 // Updated requirement for Nagad
                Log.d("PassengerDetailsFrag", "Nagad selected. isBkashOnline: false, selectedMobileTransaction: 3")
            }
            else -> {
                Toast.makeText(context, "Please select a payment method.", Toast.LENGTH_SHORT).show()
                return // Exit if no payment method selected
            }
        }

        Log.d("PassengerDetailsFrag", "handleSubmit: Calling viewModel.confirmBooking with:")
        Log.d("PassengerDetailsFrag", "  fromCity: '${args.fromCity}'")
        Log.d("PassengerDetailsFrag", "  toCity: '${args.toCity}'")
        // ... (add other existing logs if needed)
        Log.d("PassengerDetailsFrag", "  isBkashOnline: $isBkashOnline")
        Log.d("PassengerDetailsFrag", "  selectedMobileTransaction: $selectedMobileTransaction")

        viewModel.confirmBooking(
            tripIdStr = args.tripId,          
            tripRouteIdStr = args.tripRouteId,  
            ticketIdsStr = args.ticketIds.toList(), 
            boardingPointIdStr = args.boardingPointId, 
            contactPersonEmail = args.userEmail, 
            contactPersonMobile = args.userMobile, 
            passengerNames = passengerNames,      
            passengerTypes = passengerTypes,
            genders = genders,
            otpParam = args.submittedOtp,      
            authToken = args.sessionAuthToken,
            fromCity = args.fromCity,
            toCity = args.toCity,
            dateOfJourney = args.dateOfJourney,
            seatClass = args.seatClass,
            isBkashOnline = isBkashOnline, 
            selectedMobileTransaction = selectedMobileTransaction
        )
    }

    private fun observeViewModel() {
        viewModel.bookingConfirmationState.observe(viewLifecycleOwner) { state ->
            binding.passengerDetailsProgressBar.isVisible = state.isLoading
            binding.submitPassengerDetailsButton.isEnabled = !state.isLoading

            state.error?.getContentIfNotHandled()?.let { errorMsg ->
                Toast.makeText(context, "Booking Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
            state.successUrl?.getContentIfNotHandled()?.let { redirectUrl ->
                val numberOfTickets = args.ticketIds.size // Get the number of tickets
                if (numberOfTickets > 0) { // Ensure there are tickets
                    Toast.makeText(context, "$numberOfTickets ticket reserve successful", Toast.LENGTH_LONG).show()
                } else {
                    // Fallback or generic success message if ticket count is somehow zero
                    Toast.makeText(context, "Booking successful!", Toast.LENGTH_LONG).show() 
                }
                handleBookingSuccess(redirectUrl) // Proceed with redirection
            }
        }
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
