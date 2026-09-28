package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SaldoActivity : AppCompatActivity() {

    private lateinit var txtSaldoAmount: TextView

    private val auth = FirebaseAuth.getInstance()
    private val userRef by lazy {
        FirebaseDatabase.getInstance().reference.child("users").child(auth.currentUser?.uid ?: "unknown")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saldo)

        txtSaldoAmount = findViewById(R.id.txtSaldoAmount)

        findViewById<ImageView>(R.id.btnCloseSaldo).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnRetirar).setOnClickListener {
            startActivity(
                Intent(this, TarjetaActivity::class.java)
                    .putExtra(EXTRA_MODO, MODO_RETIRO)
            )
        }

        findViewById<MaterialButton>(R.id.btnDepositar).setOnClickListener {
            startActivity(
                Intent(this, TarjetaActivity::class.java)
                    .putExtra(EXTRA_MODO, MODO_DEPOSITO)
            )
        }

        setupBottomNavigation(findViewById<BottomNavigationView>(R.id.bottomNavigationSaldo))
    }

    override fun onResume() {
        super.onResume()
        loadBalance()
    }

    private fun loadBalance() {
        userRef.child("balance").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.getValue(Double::class.java) ?: 0.0
                txtSaldoAmount.text = String.format("$%.2f", balance)
            }

            override fun onCancelled(error: DatabaseError) {
                txtSaldoAmount.text = "$0.00"
            }
        })
    }

    companion object {
        const val EXTRA_MODO = "extra_modo"
        const val MODO_RETIRO = "retiro"
        const val MODO_DEPOSITO = "deposito"
    }
}