package udb.edu.sv.dsm.udbet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: Fragment
    private lateinit var searchFragment: Fragment
    private lateinit var sportsFragment: Fragment
    private lateinit var casinoFragment: Fragment
    private lateinit var historyFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            // Primera vez que se crea la Activity: se crean los 5 fragments una sola vez.
            homeFragment = HomeFragment()
            searchFragment = PlaceholderFragment.newInstance("Búsqueda", android.R.drawable.ic_menu_search)
            sportsFragment = PlaceholderFragment.newInstance("Deportes", android.R.drawable.ic_menu_compass)
            casinoFragment = PlaceholderFragment.newInstance("Casino", android.R.drawable.ic_menu_gallery)
            historyFragment = PlaceholderFragment.newInstance("Historial", android.R.drawable.ic_menu_recent_history)

            // Se agregan todos de una vez y se ocultan todos menos "Inicio".
            // Así, cambiar de pestaña es solo mostrar/ocultar (fluido, sin recargas ni parpadeos),
            // en vez de destruir y recrear la vista cada vez.
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, historyFragment, TAG_HISTORY).hide(historyFragment)
                .add(R.id.fragmentContainer, casinoFragment, TAG_CASINO).hide(casinoFragment)
                .add(R.id.fragmentContainer, sportsFragment, TAG_SPORTS).hide(sportsFragment)
                .add(R.id.fragmentContainer, searchFragment, TAG_SEARCH).hide(searchFragment)
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .commit()

            activeFragment = homeFragment
        } else {
            // La Activity se recreó (ej. rotación de pantalla): se recuperan los fragments
            // ya existentes en vez de crear unos nuevos.
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME)!!
            searchFragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)!!
            sportsFragment = supportFragmentManager.findFragmentByTag(TAG_SPORTS)!!
            casinoFragment = supportFragmentManager.findFragmentByTag(TAG_CASINO)!!
            historyFragment = supportFragmentManager.findFragmentByTag(TAG_HISTORY)!!

            activeFragment = listOf(homeFragment, searchFragment, sportsFragment, casinoFragment, historyFragment)
                .first { !it.isHidden }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val selected: Fragment? = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_search -> searchFragment
                R.id.nav_sports -> sportsFragment
                R.id.nav_casino -> casinoFragment
                R.id.nav_history -> historyFragment
                else -> null
            }

            if (selected != null && selected !== activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(selected)
                    .commit()
                activeFragment = selected
            }

            selected != null
        }
    }

    /**
     * Cambia a la pestaña indicada de forma programática (ej. al tocar un ícono
     * de categoría dentro de HomeFragment). Reutiliza la misma lógica del listener
     * de la barra: mueve el indicador visual y dispara el cambio de fragment.
     */
    fun navigateToTab(itemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = itemId
    }

    companion object {
        private const val TAG_HOME = "inicio"
        private const val TAG_SEARCH = "busqueda"
        private const val TAG_SPORTS = "deportes"
        private const val TAG_CASINO = "casino"
        private const val TAG_HISTORY = "historial"
    }
}
