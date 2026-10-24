package udb.edu.sv.dsm.udbet

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import udb.edu.sv.dsm.udbet.data.GameBridge
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient

import android.widget.Toast


class GameActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val balanceVarByFolder = mapOf(
        "traga_monedas" to "state.balance",
        "blackjack" to "state.balance",
        "cross_road" to "G.balance"
    )

    private val refreshCall =
        "if (typeof updateUI === 'function') updateUI(); if (typeof render === 'function') render();"

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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                syncRealBalanceIntoGame(gameFolder)
            }
        }

        // Manda a Logcat cualquier error de JS dentro del WebView (filtra por tag "WebViewConsole")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(
                    "WebViewConsole",
                    "${message.message()} -- línea ${message.lineNumber()} de ${message.sourceId()}"
                )
                return true
            }
        }

        webView.addJavascriptInterface(GameBridge(this, gameTitle), "AndroidBridge")
        webView.loadUrl("file:///android_asset/games/$gameFolder/index.html")
    }

    private fun syncRealBalanceIntoGame(gameFolder: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val balanceVar = balanceVarByFolder[gameFolder] ?: return

        if (uid == null) {
            Toast.makeText(this, "No se pudo identificar tu cuenta", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("balance")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val balance = snapshot.getValue(Double::class.java) ?: 0.0
                    webView.evaluateJavascript("$balanceVar = $balance; $refreshCall", null)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Antes esto fallaba en silencio y el juego se quedaba con saldo $0
                    Toast.makeText(
                        this@GameActivity,
                        "No se pudo cargar tu saldo: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        const val EXTRA_GAME_FOLDER = "extra_game_folder"
        const val EXTRA_GAME_TITLE = "extra_game_title"
    }
}