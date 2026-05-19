package com.example.railticket.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.railticket.R
import com.example.railticket.data.DeviceKeyManager
import com.example.railticket.data.TokenManager
import com.example.railticket.data.UserManager
import com.example.railticket.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun postToken(token: String) {
            Log.d("LoginFragment", "postToken CALLED. Token received: $token")
            TokenManager.authToken = token
            activity?.runOnUiThread {
                Log.d("LoginFragment", "Attempting to navigate to SecurityCodeFragment...")
                if (isAdded && findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_login_to_security)
                    Log.d("LoginFragment", "Navigation command sent.")
                } else {
                    Log.e("LoginFragment", "Navigation FAILED. Fragment not added or already navigating.")
                }
            }
        }

        @JavascriptInterface
        fun postMobileNumber(mobileNumber: String) {
            Log.d("LoginFragment", "postMobileNumber CALLED. Mobile: $mobileNumber")
            UserManager.mobileNumber = mobileNumber
        }

        @JavascriptInterface
        fun postDeviceKey(deviceKey: String) {
            Log.d("LoginFragment", "postDeviceKey CALLED. Device Key: $deviceKey")
            DeviceKeyManager.deviceKey = deviceKey
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = binding.webview
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // Enable WebView debugging
        WebView.setWebContentsDebuggingEnabled(true)

        context?.let {
            webView.addJavascriptInterface(WebAppInterface(it), "Android")
        }

        // This allows console.log messages from JS to appear in Android's Logcat
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("WebViewConsole", "${consoleMessage.message()} -- From line " +
                        "${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LoginFragment", "onPageFinished: Injecting improved script into $url")
                val script = """
                    (function() {
                        console.log('Shohoz Interceptor v3: Running...');
                    
                        // --- Intercept fetch API ---
                        const originalFetch = window.fetch;
                        window.fetch = function(url, options) {
                            if (typeof url === 'string' && url.includes('/v1.0/web/auth/sign-in')) {
                                console.log('Shohoz Interceptor v3: Intercepted FETCH sign-in call.');
                                // Capture mobile number from the REQUEST
                                if (options && options.body) {
                                    try {
                                        const body = JSON.parse(options.body);
                                        if (body.mobile_number) {
                                            console.log('Shohoz Interceptor v3: Found mobile in fetch request:', body.mobile_number);
                                            Android.postMobileNumber(body.mobile_number);
                                        }
                                    } catch (e) {
                                        console.error('Shohoz Interceptor v3: Error parsing fetch request body', e);
                                    }
                                }
                                
                                // Capture X-Device-Key from REQUEST headers
                                if (options && options.headers) {
                                    const headers = options.headers;
                                    let deviceKey = '';
                                    if (headers instanceof Headers) {
                                        deviceKey = headers.get('X-Device-Key');
                                    } else { // Handle plain object headers
                                        const lowerCaseHeaders = Object.keys(headers).reduce((acc, key) => {
                                            acc[key.toLowerCase()] = headers[key];
                                            return acc;
                                        }, {});
                                        deviceKey = lowerCaseHeaders['x-device-key'];
                                    }
                                    if (deviceKey) {
                                        console.log('Shohoz Interceptor v3: Found X-Device-Key in fetch request:', deviceKey);
                                        Android.postDeviceKey(deviceKey);
                                    }
                                }
                    
                                // Capture token from the RESPONSE
                                return originalFetch.apply(this, arguments).then(response => {
                                    if (response.ok) {
                                        response.clone().json().then(data => {
                                            if (data && data.data && data.data.token) {
                                                console.log('Shohoz Interceptor v3: Found token in fetch response.');
                                                Android.postToken(data.data.token);
                                            }
                                        });
                                    }
                                    return response;
                                });
                            }
                            return originalFetch.apply(this, arguments);
                        };
                    
                        // --- Intercept XMLHttpRequest API ---
                        const originalXHROpen = XMLHttpRequest.prototype.open;
                        const originalXHRSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

                        XMLHttpRequest.prototype.open = function(method, url) {
                            this._url = url; // Store URL for later use
                            return originalXHROpen.apply(this, arguments);
                        };

                        XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                            if (name.toLowerCase() === 'x-device-key') {
                                console.log('Shohoz Interceptor v3: Found X-Device-Key in XHR request:', value);
                                Android.postDeviceKey(value);
                            }
                            return originalXHRSetRequestHeader.apply(this, arguments);
                        };
                    
                        const originalXHRSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.send = function(data) {
                            if (this._url && typeof this._url === 'string' && this._url.includes('/v1.0/web/auth/sign-in')) {
                                console.log('Shohoz Interceptor v3: Intercepted XHR sign-in call.');
                                // Capture mobile number from the REQUEST
                                try {
                                    const body = JSON.parse(data);
                                    if (body.mobile_number) {
                                        console.log('Shohoz Interceptor v3: Found mobile in XHR request:', body.mobile_number);
                                        Android.postMobileNumber(body.mobile_number);
                                    }
                                } catch (e) {
                                    console.error('Shohoz Interceptor v3: Error parsing XHR request body', e);
                                }
                            }
                    
                            // Capture token from the RESPONSE
                            this.addEventListener('load', function() {
                                if (this.readyState === 4 && this.status === 200 && this._url && typeof this._url === 'string' && this._url.includes('/v1.0/web/auth/sign-in')) {
                                    try {
                                        const responseData = JSON.parse(this.responseText);
                                        if (responseData && responseData.data && responseData.data.token) {
                                            console.log('Shohoz Interceptor v3: Found token in XHR response.');
                                            Android.postToken(responseData.data.token);
                                        }
                                    } catch (e) {
                                        console.error('Shohoz Interceptor v3: Error parsing XHR response', e);
                                    }
                                }
                            });
                            return originalXHRSend.apply(this, arguments);
                        };
                    })();
                """
                view?.evaluateJavascript(script, null)
            }
        }
        webView.loadUrl("https://eticket.railway.gov.bd/login")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
