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
                val script = """
                    (function() {
                        // Intercept fetch
                        const originalFetch = window.fetch;
                        window.fetch = function(url, options) {
                            if (typeof url === 'string' && url.includes('https://railspaapi.shohoz.com/v1.0/web/auth/sign-in')) {
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

                        // Intercept XMLHttpRequest
                        const originalXHRSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.send = function(data) {
                            this.addEventListener('load', function() {
                                if (this.responseURL.includes('https://railspaapi.shohoz.com/v1.0/web/auth/sign-in')) {
                                    try {
                                        const responseData = JSON.parse(this.responseText);
                                        if (responseData && responseData.data && responseData.data.token) {
                                            Android.postToken(responseData.data.token);
                                        }
                                    } catch (e) {
                                        // Not a JSON response or parsing error, ignore.
                                        console.error('Error parsing response from sign-in API', e);
                                    }
                                }
                            });
                            originalXHRSend.apply(this, arguments);
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
