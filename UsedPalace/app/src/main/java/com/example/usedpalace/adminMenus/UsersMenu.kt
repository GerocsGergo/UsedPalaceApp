package com.example.usedpalace.adminMenus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.adminMenus.requests.UserCreateRequest
import com.example.usedpalace.adminMenus.requests.UserDeleteRequest
import com.example.usedpalace.adminMenus.requests.UserModifyRequest
import com.example.usedpalace.adminMenus.responses.GetUsersResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import network.RetrofitClient

class UsersMenu : AppCompatActivity(), UsersMenuAdapter.OnUserActionListener {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 1

    private lateinit var buttonFilter: Button
    private lateinit var buttonBack: Button
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicator: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UsersMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_users_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupClickListeners()
        initialize()
        setupPagination()
        getUsers()
    }

    private fun setupViews() {
        buttonFilter = findViewById(R.id.buttonFilter)
        buttonBack = findViewById(R.id.buttonBack)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageIndicator = findViewById(R.id.pageIndicator)
        recyclerView = findViewById(R.id.recyclerViewUsers) // az XML-ben így hívod

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsersMenuAdapter(emptyList(), this)
        recyclerView.adapter = adapter
    }

    private fun setupPagination() {
        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                getUsers()
            }
        }
        nextPageButton.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                getUsers()
            }
        }
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun getUsers(adminOnly: Boolean? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getUsers(
                    page = currentPage,
                    pageSize = pageSize,
                    adminOnly = adminOnly
                )
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        adapter.updateUsers(response.data)
                        totalPages = response.totalPages
                        pageIndicator.text = "${response.currentPage} / ${response.totalPages}"
                    } else {
                        ErrorHandler.toaster(this@UsersMenu, "Nincs megjeleníthető felhasználó")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@UsersMenu, e)
                }
            }
        }
    }


    // Adapter események kezelése
    override fun onEdit(user: GetUsersResponse.UserData) {
        showModifyDialog(user.id)
    }

    override fun onDelete(user: GetUsersResponse.UserData) {
        // Ellenőrizzük, hogy nem admin a törlendő user (ha kell)
        if (user.isAdmin) {
            Toast.makeText(this, "Nem törölhetsz admin felhasználót!", Toast.LENGTH_SHORT).show()
            return
        }

        // AlertDialog létrehozása
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Felhasználó törlése")
        builder.setMessage("Biztosan törlöd ${user.name} felhasználót?")
        builder.setPositiveButton("Igen") { dialog, _ ->
            // Itt hívhatjuk meg az API-t a törléshez
            deleteUserAdmin(user.id)
            dialog.dismiss()
        }
        builder.setNegativeButton("Mégse") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }


    // API hívás a törléshez
    private fun deleteUserAdmin(userId: Int) {
        val adminId = prefs.getInt("userId", -1) // vagy ahonnan az admin id jön
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.deleteUserAdmin(UserDeleteRequest(adminId, userId))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@UsersMenu, "Felhasználó törölve", Toast.LENGTH_SHORT).show()
                        getUsers() // Frissítjük a listát
                    } else {
                        Toast.makeText(this@UsersMenu, response.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@UsersMenu, e)
                }
            }
        }
    }

    override fun onOpenSales(user: GetUsersResponse.UserData) {
        val intent = Intent(this, UsersSales::class.java)
        intent.putExtra("userId", user.id)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }

        buttonFilter.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.users_admin_filter_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.filter_all -> {
                        Toast.makeText(this, "Összes felhasználó", Toast.LENGTH_SHORT).show()
                        getUsers()
                    }
                    R.id.filter_admin -> {
                        Toast.makeText(this, "Csak adminok", Toast.LENGTH_SHORT).show()
                        getUsers(adminOnly = true) // szükséges paraméter
                    }
                    R.id.filter_non_admin -> {
                        Toast.makeText(this, "Csak nem adminok", Toast.LENGTH_SHORT).show()
                        getUsers(adminOnly = false)
                    }
                    R.id.add_user -> {
                        showAddUserDialog()
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val emailInput = dialogView.findViewById<EditText>(R.id.emailInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.phoneInput)
        val isAdminCheckbox = dialogView.findViewById<CheckBox>(R.id.isAdminCheckbox)
        val isVerifiedCheckbox = dialogView.findViewById<CheckBox>(R.id.isVerified)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Új felhasználó hozzáadása")
            .setView(dialogView)
            .setPositiveButton("Hozzáadás") { d, _ ->
                val name = nameInput.text.toString()
                val email = emailInput.text.toString()
                val password = passwordInput.text.toString()
                val phone = phoneInput.text.toString()
                val isAdmin = isAdminCheckbox.isChecked
                val isVerified = isVerifiedCheckbox.isChecked

                createUser(name, email, password, phone, isAdmin, isVerified)

                d.dismiss()
            }
            .setNegativeButton("Mégse") { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun createUser(
        name: String,
        email: String,
        password: String,
        phone: String,
        isAdmin: Boolean,
        verified: Boolean
    ) {
        val adminId = prefs.getInt("userId", -1)
        if (adminId == -1) {
            Toast.makeText(this, "Admin azonosító nem elérhető", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UserCreateRequest(
            adminId = adminId,
            fullname = name,
            email = email,
            password = password,
            phoneNumber = phone,
            isAdmin = isAdmin,
            isVerified = verified
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.createUserAdmin(request)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@UsersMenu, "Felhasználó létrehozva", Toast.LENGTH_SHORT).show()
                        getUsers() // frissítjük a listát
                    } else {
                        Toast.makeText(this@UsersMenu, response.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@UsersMenu, e)
                }
            }
        }
    }


    private fun showModifyDialog(userId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val emailInput = dialogView.findViewById<EditText>(R.id.emailInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.phoneInput)
        val isAdminCheckbox = dialogView.findViewById<CheckBox>(R.id.isAdminCheckbox)
        val isVerifiedCheckbox = dialogView.findViewById<CheckBox>(R.id.isVerified)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Felhasználó módosítása")
            .setView(dialogView)
            .setPositiveButton("Mentés") { d, _ ->
                modifyUser(
                    userId,
                    nameInput.text.toString(),
                    emailInput.text.toString(),
                    passwordInput.text.toString(),
                    phoneInput.text.toString(),
                    isAdminCheckbox.isChecked,
                    isVerifiedCheckbox.isChecked
                )
                d.dismiss()
            }
            .setNegativeButton("Mégse") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun modifyUser(
        userId: Int,
        name: String,
        email: String,
        password: String,
        phone: String,
        isAdmin: Boolean,
        isVerified: Boolean
    ) {
        val adminId = prefs.getInt("userId", -1)
        if (adminId == -1) {
            Toast.makeText(this, "Admin azonosító nem elérhető", Toast.LENGTH_SHORT).show()
            return
        }

        // Csak a nem üres mezőket küldjük
        val request = UserModifyRequest(
            adminId = adminId,
            userId = userId,
            fullname = name.ifEmpty { null },
            email = email.ifEmpty { null },
            password = password.ifEmpty { null },
            phoneNumber = phone.ifEmpty { null },
            isAdmin = isAdmin,
            isVerified = isVerified
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.modifyUserAdmin(request)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@UsersMenu, "Felhasználó módosítva", Toast.LENGTH_SHORT).show()
                        getUsers()
                    } else {
                        Toast.makeText(this@UsersMenu, response.message ?: "Hiba történt", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@UsersMenu, e)
                }
            }
        }
    }



    private fun navigateBackToProfile() {
        val intent = Intent(this, AdminMainMenu::class.java)
        startActivity(intent)
        finish()
    }
}
