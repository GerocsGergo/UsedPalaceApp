package com.example.usedpalace

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.usedpalace.profilemenus.AboutActivity
import com.example.usedpalace.profilemenus.ContactsActivity
import com.example.usedpalace.profilemenus.CreateSaleActivity
import com.example.usedpalace.profilemenus.HelpActivity
import com.example.usedpalace.profilemenus.OwnSalesActivity
import com.example.usedpalace.profilemenus.ProfileActivity
import com.example.usedpalace.profilemenus.SettingsActivity
import com.example.usedpalace.profilemenus.SupportActivity

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_profile, container, false)

        val buttonProfile: Button = view.findViewById(R.id.profile)
        val buttonCreateSale: Button = view.findViewById(R.id.createSale)
        val buttonOwnSales: Button = view.findViewById(R.id.ownSales)
        val buttonHelp: Button = view.findViewById(R.id.help)
        val buttonSettings: Button = view.findViewById(R.id.settings)
        val buttonSupport: Button = view.findViewById(R.id.support)
        val buttonAbout: Button = view.findViewById(R.id.about)
        val buttonContacts: Button = view.findViewById(R.id.contacts)

        buttonProfile.setOnClickListener {
            startActivity(Intent(requireActivity(), ProfileActivity::class.java))
        }

        buttonCreateSale.setOnClickListener {
            startActivity(Intent(requireActivity(), CreateSaleActivity::class.java))
        }
        buttonOwnSales.setOnClickListener {
            startActivity(Intent(requireActivity(), OwnSalesActivity::class.java))
        }
        buttonHelp.setOnClickListener {
            startActivity(Intent(requireActivity(), HelpActivity::class.java))
        }
        buttonSettings.setOnClickListener {
            startActivity(Intent(requireActivity(), SettingsActivity::class.java))
        }
        buttonSupport.setOnClickListener {
            startActivity(Intent(requireActivity(), SupportActivity::class.java))
        }
        buttonAbout.setOnClickListener {
            startActivity(Intent(requireActivity(), AboutActivity::class.java))
        }
        buttonContacts.setOnClickListener {
            startActivity(Intent(requireActivity(), ContactsActivity::class.java))
        }

        return view
    }

}