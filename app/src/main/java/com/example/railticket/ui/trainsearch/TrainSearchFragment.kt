package com.example.railticket.ui.trainsearch

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
// import android.util.Log // Log import removed
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.railticket.R
import com.example.railticket.databinding.FragmentTrainSearchBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrainSearchFragment : Fragment() {

    private var _binding: FragmentTrainSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var trainSearchViewModel: TrainSearchViewModel

    private var validatedFromCity: String? = null
    private var validatedToCity: String? = null
    private var validatedTravelClass: String? = null
    private var validatedTrainName: String? = null

    private lateinit var cityNamesArray: Array<String>
    private lateinit var trainClassesArray: Array<String>
    private lateinit var trainNamesArray: Array<String>

    private val journeyCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trainSearchViewModel = ViewModelProvider(this, TrainSearchViewModelFactory())[TrainSearchViewModel::class.java]

        cityNamesArray = resources.getStringArray(R.array.city_names_array)
        trainClassesArray = resources.getStringArray(R.array.train_classes)
        trainNamesArray = resources.getStringArray(R.array.train_names_array)

        setupUI()
        observeViewModel()
    }

    private fun showKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupLockedAutocompleteField(
        autoCompleteTextView: AutoCompleteTextView,
        sourceArray: Array<String>,
        getValidationState: () -> String?,
        setValidationState: (String?) -> Unit
    ) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sourceArray)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 1

        autoCompleteTextView.isFocusable = true
        autoCompleteTextView.isFocusableInTouchMode = true
        autoCompleteTextView.isCursorVisible = true
        setValidationState(null) 

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            setValidationState(selectedItem)
            autoCompleteTextView.setText(selectedItem, false) 
            autoCompleteTextView.error = null
            autoCompleteTextView.isCursorVisible = false 
            autoCompleteTextView.isFocusable = false 
            autoCompleteTextView.isFocusableInTouchMode = false 
            autoCompleteTextView.clearFocus() 
        }

        autoCompleteTextView.setOnClickListener {
            if (getValidationState() != null) { 
                autoCompleteTextView.setText("", false) 
                setValidationState(null)                
                autoCompleteTextView.isFocusable = true
                autoCompleteTextView.isFocusableInTouchMode = true
                autoCompleteTextView.isCursorVisible = true
                showKeyboard(autoCompleteTextView)
                autoCompleteTextView.showDropDown()
            } else {
                if (!autoCompleteTextView.isFocused) {
                   showKeyboard(autoCompleteTextView) 
                }
                 autoCompleteTextView.showDropDown()
            }
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) { 
                    if (getValidationState() != null) { 
                         setValidationState(null)
                    }
                    if(!autoCompleteTextView.isFocusableInTouchMode){
                        autoCompleteTextView.isFocusable = true
                        autoCompleteTextView.isFocusableInTouchMode = true
                        autoCompleteTextView.isCursorVisible = true
                    }
                } else {
                    val currentText = s.toString()
                    if (currentText != getValidationState() && !sourceArray.contains(currentText)) {
                        setValidationState(null)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && getValidationState() != null && !autoCompleteTextView.isFocusableInTouchMode) {
                autoCompleteTextView.showDropDown()
            } else if (!hasFocus) { 
                 val currentText = autoCompleteTextView.text.toString()
                 if (!currentText.isNullOrEmpty() && !sourceArray.contains(currentText)) {
                    setValidationState(null)
                 }
            }
        }
    }

    private fun setupEditableAutocompleteField(
        autoCompleteTextView: AutoCompleteTextView,
        sourceArray: Array<String>,
        setValidationState: (String?) -> Unit
    ) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sourceArray)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 1

        autoCompleteTextView.isFocusable = true
        autoCompleteTextView.isFocusableInTouchMode = true
        autoCompleteTextView.isCursorVisible = true
        val initialText = autoCompleteTextView.text.toString()
        if (initialText.isNotEmpty() && sourceArray.contains(initialText)) {
            setValidationState(initialText)
        } else {
            setValidationState(null)
        }

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            setValidationState(selectedItem)
            autoCompleteTextView.error = null
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s.toString()
                if (currentText.isEmpty()) {
                    setValidationState(null)
                } else {
                    if (!sourceArray.contains(currentText)) {
                         setValidationState(null)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        autoCompleteTextView.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) { 
                val currentText = autoCompleteTextView.text.toString()
                if (currentText.isNotEmpty()) {
                    if (sourceArray.contains(currentText)) {
                        setValidationState(currentText)
                        autoCompleteTextView.error = null
                    } else {
                        setValidationState(null)
                        autoCompleteTextView.error = "Select a valid option from the list."
                    }
                } else {
                    setValidationState(null) 
                    autoCompleteTextView.error = null 
                }
            } else { 
                autoCompleteTextView.error = null 
            }
        }
    }

    private fun setupUI() {
        setupEditableAutocompleteField(binding.fromCityAutocomplete, cityNamesArray, { validatedFromCity = it })
        setupEditableAutocompleteField(binding.toCityAutocomplete, cityNamesArray, { validatedToCity = it })
        setupEditableAutocompleteField(binding.trainNameAutocomplete, trainNamesArray, { validatedTrainName = it })
        setupLockedAutocompleteField(binding.chooseClassAutocomplete, trainClassesArray, { validatedTravelClass }, { validatedTravelClass = it })

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            journeyCalendar.set(Calendar.YEAR, year)
            journeyCalendar.set(Calendar.MONTH, monthOfYear)
            journeyCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(journeyCalendar)
        }
        binding.journeyDate.setOnClickListener { 
            val today = Calendar.getInstance()
            val dialog = DatePickerDialog(
                requireContext(), dateSetListener, 
                journeyCalendar.get(Calendar.YEAR), journeyCalendar.get(Calendar.MONTH), journeyCalendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.minDate = today.timeInMillis
            val maxDateCalendar = Calendar.getInstance()
            maxDateCalendar.add(Calendar.DAY_OF_YEAR, 10)
            dialog.datePicker.maxDate = maxDateCalendar.timeInMillis
            dialog.show()
        }
        binding.submitButton.setOnClickListener { handleSubmit() }
    }

    private fun updateLabel(calendar: Calendar) {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        binding.journeyDate.setText(sdf.format(calendar.time))
    }

    private fun observeViewModel() {
        trainSearchViewModel.bookingState.observe(viewLifecycleOwner) { bookingProgress ->
            binding.loadingProgressBar.isVisible = bookingProgress.isLoading
            binding.submitButton.isEnabled = !bookingProgress.isLoading
            bookingProgress.message?.let { 
                if(it.startsWith("Starting booking") || it.startsWith("Searching for trips") || it.startsWith("Fetching seat layout") || it.startsWith("Initiating OTP")) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
            bookingProgress.error?.getContentIfNotHandled()?.let {
                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            }
            bookingProgress.success?.getContentIfNotHandled()?.let {
                Toast.makeText(context, "Success: $it", Toast.LENGTH_LONG).show()
            }
        }

        trainSearchViewModel.seatReservationAttemptEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { attemptDetails ->
                binding.loadingProgressBar.isVisible = false 
                binding.submitButton.isEnabled = true

                if (attemptDetails.error != null) { 
                    AlertDialog.Builder(requireContext())
                        .setTitle("Reservation Failed")
                        .setMessage(attemptDetails.error)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else if (attemptDetails.reservedSeatsS > 0) {
                    if (attemptDetails.reservedSeatsS < attemptDetails.requestedSeatsN) {
                        // Partial success - show confirmation dialog
                        AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Reservation")
                            .setMessage("We could only reserve ${attemptDetails.reservedSeatsS} out of your requested ${attemptDetails.requestedSeatsN} seats. Do you want to proceed with these ${attemptDetails.reservedSeatsS} seats?")
                            .setPositiveButton("Proceed with ${attemptDetails.reservedSeatsS} seats") { _, _ ->
                                trainSearchViewModel.proceedToOtpStage(attemptDetails)
                            }
                            .setNegativeButton("Cancel") { dialog, _ -> 
                                Toast.makeText(context, "Booking cancelled by user.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss() 
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        // Full success
                        Toast.makeText(context, "${attemptDetails.reservedSeatsS} seat(s) reserved. Proceeding to OTP...", Toast.LENGTH_SHORT).show()
                        trainSearchViewModel.proceedToOtpStage(attemptDetails)
                    }
                } else {
                    // No seats reserved (S == 0) and no specific error from backend
                    AlertDialog.Builder(requireContext())
                        .setTitle("Reservation Failed")
                        .setMessage("No seats could be reserved. Please try again.")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }

        trainSearchViewModel.navigateToOtpEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { navData ->
                val action = TrainSearchFragmentDirections.actionTrainSearchFragmentToOtpVerificationFragment(
                    navData.tripId, navData.tripRouteId, navData.ticketIds, navData.authToken, navData.numberOfSeats, 
                    navData.boardingPointId, navData.fromCity, navData.toCity, navData.dateOfJourney, navData.seatClass
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun handleSubmit() {
        val currentFromCityText = binding.fromCityAutocomplete.text.toString()
        if (validatedFromCity == null && cityNamesArray.contains(currentFromCityText)) {
            validatedFromCity = currentFromCityText
        } else if (!cityNamesArray.contains(currentFromCityText)){
             validatedFromCity = null 
        }

        val currentToCityText = binding.toCityAutocomplete.text.toString()
        if (validatedToCity == null && cityNamesArray.contains(currentToCityText)) {
            validatedToCity = currentToCityText
        } else if (!cityNamesArray.contains(currentToCityText)) {
            validatedToCity = null
        }

        val currentTrainNameText = binding.trainNameAutocomplete.text.toString()
        if (validatedTrainName == null && trainNamesArray.contains(currentTrainNameText)) {
            validatedTrainName = currentTrainNameText
        } else if (!trainNamesArray.contains(currentTrainNameText)) {
            validatedTrainName = null
        }

        val fromCitySubmit = validatedFromCity
        val toCitySubmit = validatedToCity
        val classSubmit = validatedTravelClass 
        val trainNameSubmit = validatedTrainName
        
        val journeyDate = binding.journeyDate.text.toString().trim()
        val numberOfSeatsStr = binding.numOfSeats.text.toString().trim()
        var isValid = true

        if (fromCitySubmit == null) {
            binding.fromCityAutocomplete.error = "Please select or enter a valid city."
            isValid = false
        }
        if (toCitySubmit == null) {
            binding.toCityAutocomplete.error = "Please select or enter a valid city."
            isValid = false
        }
        if (classSubmit == null) {
            binding.chooseClassAutocomplete.error = "Please select a valid class from the list."
            isValid = false
        }
        if (trainNameSubmit == null) {
            binding.trainNameAutocomplete.error = "Please select or enter a valid train name."
            isValid = false
        }
        if (journeyDate.isEmpty()) {
            Toast.makeText(requireContext(), "Journey date is required.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (numberOfSeatsStr.isEmpty()) {
            binding.numOfSeats.error = "Number of seats is required."
            isValid = false
        }
        if (!isValid) {
            Toast.makeText(requireContext(), "Please correct the highlighted fields.", Toast.LENGTH_LONG).show()
            return
        }
        val numberOfSeats = try { numberOfSeatsStr.toInt() } catch (e: NumberFormatException) {
            binding.numOfSeats.error = "Invalid number."
            Toast.makeText(requireContext(), "Invalid number of seats.", Toast.LENGTH_LONG).show()
            return
        }
        if (numberOfSeats <= 0) {
            binding.numOfSeats.error = "Must be > 0."
            Toast.makeText(requireContext(), "Number of seats must be greater than zero.", Toast.LENGTH_LONG).show()
            return
        }
        if (numberOfSeats > 4) { 
            binding.numOfSeats.error = "Max 4."
            Toast.makeText(requireContext(), "Max 4 seats per transaction.", Toast.LENGTH_LONG).show()
            return
        }
        trainSearchViewModel.clearBookingProgressState()
        trainSearchViewModel.startBookingProcess(
            fromCitySubmit!!, toCitySubmit!!, journeyDate, classSubmit!!, 
            trainNameSubmit!!, classSubmit, numberOfSeats
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
