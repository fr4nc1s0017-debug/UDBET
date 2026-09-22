package udb.edu.sv.dsm.udbet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)
        // Mansajes al sellecionar pantallas
        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_search -> {
                    Toast.makeText(this, "Búsqueda", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_sports -> {
                    Toast.makeText(this, "Deportes", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_casino -> {
                    Toast.makeText(this, "Casino", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_history -> {
                    Toast.makeText(this, "Historial", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }
    }
}