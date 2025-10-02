package com.example.usedpalace

    object UserSession {
        // Store user data
        private var userId: Int? = null
        private var userName: String? = null
        private var isAdmin: Boolean = false

        // Set user data after login
        fun setUserData(id: Int, name: String, isAdmin: Boolean) {
            userId = id
            userName = name
            this.isAdmin = isAdmin
        }

        // Get user ID (nullable)
        fun getUserId(): Int? = userId

        //get isAdmin
        fun getUserIsAdmin(): Boolean = this.isAdmin

        // Clear on logout
        fun clear() {
            userId = null
            userName = null
            this.isAdmin = false
        }

}