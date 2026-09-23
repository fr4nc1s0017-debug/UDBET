package udb.edu.sv.dsm.udbet

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlaceholderFragment : Fragment(R.layout.activity_placeholder_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val iconRes = arguments?.getInt(ARG_ICON) ?: android.R.drawable.ic_menu_help

        view.findViewById<TextView>(R.id.txtPlaceholderTitle).text = title
        view.findViewById<ImageView>(R.id.imgPlaceholderIcon).setImageResource(iconRes)
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ICON = "arg_icon"

        fun newInstance(title: String, iconRes: Int): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_ICON, iconRes)
            }
            return fragment
        }
    }
}
