package udb.edu.sv.dsm.udbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import udb.edu.sv.dsm.udbet.data.GameBridge

class GameActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val gameFolder = intent.getStringExtra(EXTRA_GAME_FOLDER) ?: "traga_monedas"
        val gameTitle = intent.getStringExtra(EXTRA_GAME_TITLE) ?: ""
        title = gameTitle

        webView = findViewById(R.id.webViewGame)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d("UDBET_DEBUG", "Página cargada. Esperando 1 segundo...")
                // Esperamos 1 segundo completo para asegurar que el JS esté listo
                Handler(Looper.getMainLooper()).postDelayed({
                    iniciarEscuchaDeSaldo()
                }, 1000)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("WebViewConsole", "${message.message()} -- línea ${message.lineNumber()}")
                return true
            }
        }

        webView.addJavascriptInterface(GameBridge(this, gameTitle, webView, gameFolder), "AndroidBridge")
        webView.loadUrl("file:///android_asset/games/$gameFolder/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun iniciarEscuchaDeSaldo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Usamos addValueEventListener para que se actualice EN VIVO si el saldo cambia
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("balance")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val balance = snapshot.getValue(Double::class.java) ?: 0.0
                    Log.d("UDBET_DEBUG", "Saldo desde Firebase: $balance")

                    // Mostramos un Toast para que TÚ veas que sí se está leyendo
                    Toast.makeText(this@GameActivity, "Saldo Firebase: $balance", Toast.LENGTH_SHORT).show()

                    // Inyectamos el saldo
                    val js = "if (typeof window.actualizarSaldoDesdeAndroid === 'function') { window.actualizarSaldoDesdeAndroid($balance); } else { console.log('ERROR: actualizarSaldoDesdeAndroid NO EXISTE'); }"
                    webView.evaluateJavascript(js) { result ->
                        Log.d("UDBET_DEBUG", "Resultado JS: $result")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UDBET_DEBUG", "Error Firebase: ${error.message}")
                }
            })
    }

    companion object {
        const val EXTRA_GAME_FOLDER = "extra_game_folder"
        const val EXTRA_GAME_TITLE = "extra_game_title"
    }
}