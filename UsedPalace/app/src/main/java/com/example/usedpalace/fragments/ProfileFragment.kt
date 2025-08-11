package com.example.usedpalace.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.usedpalace.R
import com.example.usedpalace.profilemenus.AboutActivity
import com.example.usedpalace.profilemenus.InformationActivity
import com.example.usedpalace.profilemenus.HelpActivity
import com.example.usedpalace.profilemenus.OwnSalesActivity
import com.example.usedpalace.profilemenus.ProfileActivity
import com.example.usedpalace.profilemenus.SettingsActivity
import com.example.usedpalace.profilemenus.SupportActivity

class ProfileFragment : Fragment() {
    private lateinit var buttonProfile: Button
    private lateinit var buttonCreateSale: Button
    private lateinit var buttonOwnSales: Button
    private lateinit var buttonHelp: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonSupport: Button
    private lateinit var buttonAbout: Button
    private lateinit var buttonContacts: Button

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

        setupViews(view)
        setupClickListeners()


        return view
    }

    private fun setupViews(view: View) {

        buttonProfile = view.findViewById(R.id.profile)
        buttonCreateSale = view.findViewById(R.id.createSale)
        buttonOwnSales = view.findViewById(R.id.ownSales)
        buttonHelp = view.findViewById(R.id.help)
        buttonSettings = view.findViewById(R.id.settings)
        buttonSupport = view.findViewById(R.id.support)
        buttonAbout = view.findViewById(R.id.about)
        buttonContacts = view.findViewById(R.id.contacts)
    }

    private fun setupClickListeners() {
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
            startActivity(Intent(requireActivity(), InformationActivity::class.java))
        }
    }

}