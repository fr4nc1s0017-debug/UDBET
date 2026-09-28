package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class TarjetaActivity : AppCompatActivity() {

    private lateinit var modo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarjeta)

        modo = intent.getStringExtra(SaldoActivity.EXTRA_MODO) ?: SaldoActivity.MODO_DEPOSITO

        val editCardNumber = findViewById<EditText>(R.id.editCardNumber)
        val spinnerMes = findViewById<Spinner>(R.id.spinnerMes)
        val spinnerAnio = findViewById<Spinner>(R.id.spinnerAnio)

        spinnerMes.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (1..12).map { it.toString().padStart(2, '0') }
        )

        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        spinnerAnio.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (anioActual..anioActual + 10).map { it.toString() }
        )

        findViewById<MaterialButton>(R.id.btnContinuarTarjeta).setOnClickListener {
            val cardNumber = editCardNumber.text.toString().replace(" ", "")

            if (cardNumber.length < 12) {
                Toast.makeText(this, "Ingresa un número de tarjeta válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // NOTA: esto solo valida el formato para la maqueta del proyecto.
            // En una app real, el número de tarjeta se envía directo a un procesador
            // de pagos (Stripe, PayPal, etc.) y nunca se guarda tal cual en tu base
            // de datos ni pasa por tu propio backend en texto plano.

            startActivity(
                Intent(this, MontoActivity::class.java)
                    .putExtra(SaldoActivity.EXTRA_MODO, modo)
            )
        }

        setupBottomNavigation(findViewById<BottomNavigationView>(R.id.bottomNavigationTarjeta))
    }
}