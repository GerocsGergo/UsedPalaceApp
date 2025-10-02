package com.example.usedpalace.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.adminMenus.AdminMainMenu
import com.example.usedpalace.loginMenus.LogActivity
import com.example.usedpalace.profileMenus.AboutActivity
import com.example.usedpalace.profileMenus.CreateSaleActivity
import com.example.usedpalace.profileMenus.HelpActivity
import com.example.usedpalace.profileMenus.InformationActivity
import com.example.usedpalace.profileMenus.SupportActivity
import com.example.usedpalace.profileMenus.ownSalesActivity.OwnSalesActivity
import com.example.usedpalace.profileMenus.profileActivity.ProfileActivity
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import network.RetrofitClient


class ProfileFragment : Fragment() {
    private lateinit var buttonProfile: ImageButton
    private lateinit var textUsername: TextView

    private lateinit var buttonCreateSale: Button
    private lateinit var buttonOwnSales: Button
    private lateinit var buttonHelp: Button
    private lateinit var buttonSupport: Button
    private lateinit var buttonAbout: Button
    private lateinit var buttonContacts: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonAdminMenu: Button
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

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
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initialize()
        setupViews(view)
        setupClickListeners()
        setUsername()
        showAdminMenu()

        return view
    }

    private fun initialize() {
        prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(requireContext().applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupViews(view: View) {

        buttonProfile = view.findViewById(R.id.profile)
        buttonCreateSale = view.findViewById(R.id.createSale)
        buttonOwnSales = view.findViewById(R.id.ownSales)
        buttonAdminMenu = view.findViewById(R.id.adminMenu)
        buttonHelp = view.findViewById(R.id.help)
        buttonSupport = view.findViewById(R.id.support)
        buttonAbout = view.findViewById(R.id.about)
        buttonContacts = view.findViewById(R.id.contacts)
        buttonLogout = view.findViewById(R.id.logout)
        textUsername = view.findViewById(R.id.username)
    }

    private fun setupClickListeners() {
        buttonProfile.setOnClickListener {
            startActivity(Intent(requireActivity(), ProfileActivity::class.java))
        }
        buttonAdminMenu.setOnClickListener {
            startActivity(Intent(requireActivity(), AdminMainMenu::class.java))
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
        buttonSupport.setOnClickListener {
            startActivity(Intent(requireActivity(), SupportActivity::class.java))
        }
        buttonAbout.setOnClickListener {
            startActivity(Intent(requireActivity(), AboutActivity::class.java))
        }
        buttonContacts.setOnClickListener {
            startActivity(Intent(requireActivity(), InformationActivity::class.java))
        }
        buttonLogout.setOnClickListener {
            val intent = Intent(requireActivity(), LogActivity::class.java).apply {
                putExtra("forceLogout", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()

        }
    }

    private fun showAdminMenu(){
        if (UserSession.getUserIsAdmin()){
            buttonAdminMenu.visibility = View.VISIBLE
        } else {
            buttonAdminMenu.visibility = View.GONE
        }
    }


    private fun setUsername() {
        val userId = UserSession.getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchUsername(SearchRequestID(userId))
                val username: String

                if (response.success && response.fullname != null) {
                    username = response.fullname
                } else {
                    username = "Felhasználó" // fallback név
                    // Hibakezelés külön
                    withContext(Dispatchers.Main) {
                        ErrorHandler.handleApiError(requireContext(), null, response.message)
                    }
                }

                withContext(Dispatchers.Main) {
                    textUsername.text = username
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textUsername.text = "Felhasználó"
                    ErrorHandler.handleNetworkError(requireContext(), e)
                }
            }
        }
    }



}