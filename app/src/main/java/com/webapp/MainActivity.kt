package com.webapp

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import android.annotation.SuppressLint

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var locationRequestOrigin: String? = null
    private var locationRequestCallback: GeolocationPermissions.Callback? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = initEncryptedSharedPreferences()
        webView = setupWebView()
        webView.loadUrl(getString(R.string.app_url))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(): WebView {
        val wv: WebView = findViewById(R.id.webview)
        wv.settings.apply {
            javaScriptEnabled = true
            allowContentAccess = true
            domStorageEnabled = true
            useWideViewPort = true
            setGeolocationEnabled(true)
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
        }

        wv.webViewClient = object : WebViewClient() {
            // keep new URLs in webview
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.let {
                    view?.loadUrl(it.url.toString())
                }
                return true
            }

            // inject credential management
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                credentialsAutofill()
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                locationPermissions(origin, callback)
            }
        }

        wv.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun saveUserName(username: String) {
                sharedPreferences.edit().putString("username", username).apply()
            }
            @JavascriptInterface
            fun savePassword(password: String) {
                sharedPreferences.edit().putString("password", password).apply()
            }
        }, "Android")


        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(wv, true)

        return wv
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // location permissions
    private fun locationPermissions(origin: String?, callback: GeolocationPermissions.Callback?) {
        locationRequestOrigin = null
        locationRequestCallback = null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationRequestOrigin = origin
            locationRequestCallback = callback
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            callback?.invoke(origin, true, false)
        }
    }

    private val requestLocationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationRequestCallback?.invoke(locationRequestOrigin, true, false)
            } else {
                locationRequestCallback?.invoke(locationRequestOrigin, false, false)
            }
        }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // credentials management
    private fun initEncryptedSharedPreferences(): SharedPreferences {
        // Create or retrieve the MasterKey for encryption/decryption
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        return EncryptedSharedPreferences.create(
            applicationContext,
            "encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // called on each load
    private fun credentialsAutofill() {
        val user = sharedPreferences.getString("username", "")
        val pass = sharedPreferences.getString("password", "")

        // Listen for form submission and update credentials if changed
        val js = """
            const user_fld = document.querySelector("input[name='username'],input[id='username'],input[autofill='username'],input[name='user'],input[id='user'],input[autofill='user']")
            const pass_fld = document.querySelector("input[name*='password'],input[id*='password'],input[autofill*='password']")
            document.querySelector("form").addEventListener("submit", function() {
                if (user_fld) Android.saveUserName(user_fld.value)
                if (pass_fld) Android.savePassword(pass_fld.value)
            })
            
            if (user_fld && '$user') user_fld.value = '$user'
            if (pass_fld && '$pass') pass_fld.value = '$pass'
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }
}
