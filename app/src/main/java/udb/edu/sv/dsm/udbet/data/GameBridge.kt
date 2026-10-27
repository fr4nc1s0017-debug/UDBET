package udb.edu.sv.dsm.udbet.data

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class GameBridge(
    private val activity: Activity,
    private val gameName: String,
    private val webView: WebView,
    private val gameFolder: String
) {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid
    private val balanceRef get() = uid?.let {
        FirebaseDatabase.getInstance().reference.child("users").child(it).child("balance")
    }

    // Variable JS según el juego
    private val balanceVar = when (gameFolder) {
        "traga_monedas" -> "state.balance"
        "blackjack" -> "state.balance"
        "cross_road" -> "G.balance"
        else -> "balance"
    }

    @JavascriptInterface
    fun placeBet(amount: Double) {
        val ref = balanceRef ?: return
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Double::class.java) ?: 0.0
                if (current < amount) {
                    return Transaction.abort() // No hay saldo suficiente
                }
                currentData.value = current - amount
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    val newBalance = snapshot?.getValue(Double::class.java) ?: 0.0
                    // Actualizamos la UI del juego inmediatamente
                    activity.runOnUiThread {
                        webView.evaluateJavascript("$balanceVar = $newBalance; if (typeof updateUI === 'function') updateUI();", null)
                    }
                }
            }
        })
    }

    @JavascriptInterface
    fun reportResult(won: Boolean, payout: Double) {
        val currentUid = uid ?: return
        val ref = balanceRef ?: return

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = current + payout
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    val newBalance = snapshot?.getValue(Double::class.java) ?: 0.0
                    activity.runOnUiThread {
                        webView.evaluateJavascript("$balanceVar = $newBalance; if (typeof updateUI === 'function') updateUI();", null)
                    }
                }
            }
        })

        FirebaseDatabase.getInstance().reference
            .child("users").child(currentUid).child("history").push()
            .setValue(mapOf(
                "gameName" to gameName,
                "won" to payout,
                "lost" to 0.0,
                "timestamp" to System.currentTimeMillis()
            ))

        activity.runOnUiThread {
            val msg = if (won) "Ganaste $%.2f".format(payout) else "Perdiste la ronda"
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }
}