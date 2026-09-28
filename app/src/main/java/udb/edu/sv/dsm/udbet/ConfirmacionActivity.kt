package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class ConfirmacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion)

        // Se muestra un momento y regresa solo a Saldo. FLAG_ACTIVITY_CLEAR_TOP
        // elimina Tarjeta/Monto/Confirmacion de la pila, así que al llegar a Saldo
        // el botón "atrás" no vuelve al formulario que ya se completó.
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SaldoActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 1800)
    }
}