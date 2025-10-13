package com.example.railticket.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener // Using this for afterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Added for activityViewModels
import androidx.lifecycle.Observer
// ViewModelProvider is no longer needed directly here if using activityViewModels for LoginViewModel
import androidx.navigation.fragment.findNavController
import com.example.railticket.R
import com.example.railticket.data.model.LoginForm
import com.example.railticket.databinding.FragmentLoginBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LoginFragment : Fragment() {

    // Changed to use activityViewModels to share with OtpVerificationFragment and others
    private val loginViewModel: LoginViewModel by activityViewModels { LoginViewModelFactory() }
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val TAG = "LoginFragment"

    // Constant for security code calculation
    private val SECURITY_CODE_SUBTRACT_CONSTANT = 112150L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return type View is non-nullable
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun calculateDailySecurityCode(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("ddMMYYYY", Locale.getDefault())
        val dateString = dateFormat.format(calendar.time)
        
        val dateNumber = dateString.toLong()
        val intermediateNumber = dateNumber - SECURITY_CODE_SUBTRACT_CONSTANT
        return intermediateNumber.toString(16).uppercase(Locale.getDefault())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val securityCodeEditText = binding.securityCode // Assuming an id 'security_code' in your binding
        val securityCodeInputLayout = binding.securityCodeTextInputLayout // Assuming 'security_code_text_input_layout'
        val usernameEditText = binding.mobileNumber
        val passwordEditText = binding.password
        val loginButton = binding.login
        val loadingProgressBar = binding.loading

        // Observe form state for mobile number and password (Security code is validated separately)
        loginViewModel.loginFormState.observe(viewLifecycleOwner, Observer { loginState: LoginFormState? ->
            val currentLoginState = loginState ?: return@Observer

            // Keep login button enabled based on mobile/password validity initially, 
            // but security code check will gate the actual login action.
            loginButton.isEnabled = currentLoginState.isDataValid 

            if (currentLoginState.mobileNumberError != null) {
                usernameEditText.error = currentLoginState.mobileNumberError
            } else {
                usernameEditText.error = null // Explicitly clear error
            }
            if (currentLoginState.passwordError != null) {
                passwordEditText.error = currentLoginState.passwordError
            } else {
                passwordEditText.error = null // Explicitly clear error
            }
        })

        loginViewModel.loginResult.observe(viewLifecycleOwner, Observer { loginResult: LoginResult? ->
            Log.d(TAG, "loginResult observer triggered with: $loginResult")
            val currentLoginResult = loginResult ?: run {
                Log.d(TAG, "loginResult is null, returning from observer.")
                loadingProgressBar.visibility = View.GONE // Ensure loading is hidden
                return@Observer
            }
            loadingProgressBar.visibility = View.GONE

            currentLoginResult.error?.let {
                Log.d(TAG, "Login error observed: $it")
                val appContext = context?.applicationContext ?: return@Observer
                Toast.makeText(appContext, it, Toast.LENGTH_LONG).show()
            }

            currentLoginResult.success?.let {
                Log.d(TAG, "Login success observed: $it")
                val successMessage = it.message ?: "Login is successful!"
                val appContext = context?.applicationContext ?: return@Observer
                Toast.makeText(appContext, successMessage, Toast.LENGTH_LONG).show()
                Log.d(TAG, "Attempting to navigate to action_loginFragment_to_trainSearchFragment")
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_trainSearchFragment)
                    Log.d(TAG, "Navigation called successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation failed!", e)
                    Toast.makeText(appContext, "Navigation Error! Check logs.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Listener for Security Code to clear error when user types
        securityCodeEditText.addTextChangedListener {
            securityCodeInputLayout.error = null 
        }

        usernameEditText.addTextChangedListener { editable ->
            loginViewModel.loginDataChanged(
                LoginForm(
                    mobileNumber = editable.toString(),
                    password = passwordEditText.text.toString()
                )
            )
        }

        passwordEditText.addTextChangedListener { editable ->
            loginViewModel.loginDataChanged(
                 LoginForm(
                    mobileNumber = usernameEditText.text.toString(),
                    password = editable.toString()
                )
            )
        }

        loginButton.setOnClickListener {
            Log.d(TAG, "Login button clicked.")
            val enteredSecurityCode = securityCodeEditText.text.toString().trim().uppercase(Locale.getDefault())
            val expectedSecurityCode = calculateDailySecurityCode()

            if (enteredSecurityCode == expectedSecurityCode) {
                securityCodeInputLayout.error = null // Clear error if successful
                // Proceed with username/password validation via ViewModel
                loadingProgressBar.visibility = View.VISIBLE
                loginViewModel.login(
                    LoginForm(
                        mobileNumber = usernameEditText.text.toString(),
                        password = passwordEditText.text.toString()
                    )
                )
            } else {
                Log.d(TAG, "Security code mismatch. Entered: $enteredSecurityCode, Expected: $expectedSecurityCode")
                securityCodeInputLayout.error = "Invalid Security Code"
                loadingProgressBar.visibility = View.GONE // Ensure loading is hidden on immediate validation fail
                // Optionally, shake animation or other visual feedback
                Toast.makeText(requireContext(), "Invalid Security Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
