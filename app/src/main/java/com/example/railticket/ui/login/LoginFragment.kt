package com.example.railticket.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.railticket.R
import com.example.railticket.data.TokenManager
import com.example.railticket.data.UserManager
import com.example.railticket.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun postToken(token: String) {
            Log.d("LoginFragment", "Token from WebView: $token")
            TokenManager.authToken = token
            activity?.runOnUiThread {
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_trainSearchFragment)
                }
            }
        }

        @JavascriptInterface
        fun postMobileNumber(mobileNumber: String) {
            Log.d("LoginFragment", "Mobile Number - $mobileNumber")
            UserManager.mobileNumber = mobileNumber
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
        context?.let {
            webView.addJavascriptInterface(WebAppInterface(it), "Android")
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LoginFragment", "onPageFinished: Injecting comprehensive script into $url")
                val script = """
                    (function() {
                        console.log('Shohoz Interceptor v2: Running...');

                        // --- Intercept fetch API ---
                        const originalFetch = window.fetch;
                        window.fetch = function(url, options) {
                            if (typeof url === 'string' && url.includes('/v1.0/web/auth/sign-in')) {
                                console.log('Shohoz Interceptor v2: Intercepted FETCH sign-in call.');
                                if (options && options.body) {
                                    try {
                                        const body = JSON.parse(options.body);
                                        if (body.mobile_number) {
                                            console.log('Shohoz Interceptor v2: Found mobile in fetch body:', body.mobile_number);
                                            Android.postMobileNumber(body.mobile_number);
                                        }
                                    } catch (e) {
                                        console.error('Shohoz Interceptor v2: Error parsing fetch body', e);
                                    }
                                }
                                return originalFetch.apply(this, arguments).then(response => {
                                    if (response.ok) {
                                        response.clone().json().then(data => {
                                            if (data && data.data && data.data.token) {
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
                        XMLHttpRequest.prototype.open = function(method, url) {
                            this._url = url; // Store URL for later use
                            return originalXHROpen.apply(this, arguments);
                        };

                        const originalXHRSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.send = function(data) {
                            if (this._url && typeof this._url === 'string' && this._url.includes('/v1.0/web/auth/sign-in')) {
                                console.log('Shohoz Interceptor v2: Intercepted XHR sign-in call.');
                                try {
                                    const body = JSON.parse(data);
                                    if (body.mobile_number) {
                                        console.log('Shohoz Interceptor v2: Found mobile in XHR body:', body.mobile_number);
                                        Android.postMobileNumber(body.mobile_number);
                                    }
                                } catch (e) {
                                    console.error('Shohoz Interceptor v2: Error parsing XHR body', e);
                                }
                            }

                            this.addEventListener('load', function() {
                                if (this._url && typeof this._url === 'string' && this._url.includes('/v1.0/web/auth/sign-in')) {
                                    try {
                                        const responseData = JSON.parse(this.responseText);
                                        if (responseData && responseData.data && responseData.data.token) {
                                            Android.postToken(responseData.data.token);
                                        }
                                    } catch (e) {
                                        console.error('Shohoz Interceptor v2: Error parsing XHR response', e);
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
