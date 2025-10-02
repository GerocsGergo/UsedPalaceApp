package com.example.usedpalace.adminMenus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.adminMenus.responses.GetUsersResponse
import com.google.android.material.button.MaterialButton

class UsersMenuAdapter(
    private var users: List<GetUsersResponse.UserData>,
    private val listener: OnUserActionListener
) : RecyclerView.Adapter<UsersMenuAdapter.UserViewHolder>() {

    interface OnUserActionListener {
        fun onEdit(user: GetUsersResponse.UserData)
        fun onDelete(user: GetUsersResponse.UserData)
        fun onOpenSales(user: GetUsersResponse.UserData)
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.textUserName)
        val textAdminLabel: TextView = itemView.findViewById(R.id.textAdminLabel)
        val userId: TextView = itemView.findViewById(R.id.textUserId)
        val buttonEdit: MaterialButton = itemView.findViewById(R.id.buttonEditUser)
        val buttonDelete: MaterialButton = itemView.findViewById(R.id.buttonDeleteUser)
        val buttonOpenSales: MaterialButton = itemView.findViewById(R.id.buttonOpenSales)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_adminmenu_user_card, parent, false) // ez a card XML amit csináltál
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.name
        holder.userId.text = "ID: ${user.id}"
        holder.textAdminLabel.visibility = if (user.isAdmin) View.VISIBLE else View.GONE

        holder.buttonEdit.setOnClickListener { listener.onEdit(user) }
        holder.buttonDelete.setOnClickListener { listener.onDelete(user) }
        holder.buttonOpenSales.setOnClickListener { listener.onOpenSales(user) }
    }

    fun updateUsers(newUsers: List<GetUsersResponse.UserData>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
