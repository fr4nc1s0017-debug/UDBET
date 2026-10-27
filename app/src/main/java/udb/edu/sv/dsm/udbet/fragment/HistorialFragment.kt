package udb.edu.sv.dsm.udbet.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import udb.edu.sv.dsm.udbet.R

class HistorialFragment : Fragment(R.layout.fragment_historial) {

    private var container: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.historialContainer)
    }

    override fun onResume() {
        super.onResume()
        cargarHistorial()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        container = null
    }

    private fun cargarHistorial() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val historyRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("history")

        historyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entradas = snapshot.children
                    .mapNotNull { it.getValue(HistorialEntry::class.java) }
                    .sortedByDescending { it.timestamp }
                mostrarEntradas(entradas)
            }

            override fun onCancelled(error: DatabaseError) {
                // Sin conexión o sin permisos
            }
        })
    }

    private fun mostrarEntradas(entradas: List<HistorialEntry>) {
        val cont = container ?: return
        cont.removeAllViews()

        if (entradas.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "Todavía no has jugado ninguna apuesta"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            cont.addView(txt)
            return
        }

        entradas.forEach { entrada ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_history_entry, cont, false)

            itemView.findViewById<TextView>(R.id.txtHistGameName).text = entrada.gameName
            itemView.findViewById<TextView>(R.id.txtHistGanado).text =
                "Ganado: $%.2f".format(entrada.won)
            itemView.findViewById<TextView>(R.id.txtHistPerdido).text =
                "Perdido: $%.2f".format(entrada.lost)

            cont.addView(itemView)
        }
    }
}