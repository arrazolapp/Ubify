package com.arrazolapp.gpstracker

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*

/**
 * NavActivity — App de navegación standalone
 * Carga map_agent_nav.html (sin tracking, solo mapa + clientes + rutas)
 * Tiene su propio ícono de launcher en el manifest.
 */
class NavActivity : AppCompatActivity() {

    private lateinit var webNav: WebView
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    @SuppressLint("SetJavaScriptEnabled", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_nav)

        webNav = findViewById(R.id.webNav)

        webNav.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
        }
        webNav.webChromeClient = WebChromeClient()

        webNav.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Pasar solo el company — esta versión no necesita userId de tracking
                val prefs = getSharedPreferences("agent_config", MODE_PRIVATE)
                val company = prefs.getString("company", "demo_corp") ?: "demo_corp"
                val nombre  = prefs.getString("nombre", "Navigator") ?: "Navigator"
                // USER_ID vacío — la versión nav no lo necesita
                webNav.evaluateJavascript("setConfig('$company','','$nombre')", null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleExternalUrl(url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return handleExternalUrl(url)
            }
        }

        webNav.addJavascriptInterface(object : Any() {

            @JavascriptInterface
            fun openWaze(lat: Double, lng: Double) {
                runOnUiThread {
                    val wazeUri = Uri.parse("waze://?ll=$lat,$lng&navigate=yes")
                    val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                        setPackage("com.waze")
                    }
                    try {
                        startActivity(wazeIntent)
                    } catch (e: Exception) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
                        } catch (e2: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.waze")))
                        }
                    }
                }
            }

            @JavascriptInterface
            fun openGoogleMaps(lat: Double, lng: Double) {
                runOnUiThread {
                    val gmmUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
                    val gmmIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (gmmIntent.resolveActivity(packageManager) != null) {
                        startActivity(gmmIntent)
                    } else {
                        val browserUri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"
                        )
                        startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                }
            }

            @JavascriptInterface
            fun fetchRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
                Thread {
                    try {
                        val url = java.net.URL(
                            "https://router.project-osrm.org/route/v1/driving/" +
                            "$originLng,$originLat;$destLng,$destLat" +
                            "?overview=full&geometries=geojson&steps=true"
                        )
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        val json = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        runOnUiThread {
                            val jsCode = "window._routeJson = $json; onRouteResultFromAndroid();"
                            webNav.evaluateJavascript(jsCode, null)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            webNav.evaluateJavascript("onRouteError()", null)
                        }
                    }
                }.start()
            }
        }, "Android")

        webNav.loadUrl("file:///android_asset/map_agent_nav.html")

        // GPS — actualizar posición en el mapa
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val locReq = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val bearing = if (loc.hasBearing()) loc.bearing else 0f
                val speed   = if (loc.hasSpeed()) loc.speed else 0f
                webNav.evaluateJavascript(
                    "updateUserPosition(${loc.latitude},${loc.longitude},${loc.accuracy.toInt()},$speed,$bearing)",
                    null
                )
            }
        }

        try {
            fusedClient.requestLocationUpdates(locReq, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {}
    }

    private fun handleExternalUrl(url: String): Boolean {
        return when {
            url.startsWith("waze://") || url.contains("waze.com") -> {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze")))
                }
                true
            }
            url.startsWith("google.navigation:") || (url.contains("google.com/maps") && !url.startsWith("file://")) -> {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                true
            }
            url.startsWith("http://") || url.startsWith("https://") -> {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                true
            }
            url.startsWith("file://") -> false
            else -> {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }
}
