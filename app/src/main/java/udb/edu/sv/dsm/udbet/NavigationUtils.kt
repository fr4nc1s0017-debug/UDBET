package udb.edu.sv.dsm.udbet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

const val EXTRA_SELECTED_TAB = "extra_selected_tab"

/**
 * Conecta la barra de navegación de una Activity secundaria (Saldo, Tarjeta, Monto)
 * con las pestañas de MainActivity: al tocar una pestaña, regresa ahí mostrando
 * esa sección, en vez de intentar dibujar el contenido en esta misma Activity.
 */
fun AppCompatActivity.setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
    bottomNavigationView.setOnItemSelectedListener { item ->
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_SELECTED_TAB, item.itemId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
        true
    }
}