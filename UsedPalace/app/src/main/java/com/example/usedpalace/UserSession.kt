package com.example.usedpalace

    object UserSession {
        // Store user data
        private var userId: Int? = null
        private var userName: String? = null

        // Set user data after login
        fun setUserData(id: Int, name: String) {
            userId = id
            userName = name
        }

        // Get user ID (nullable)
        fun getUserId(): Int? = userId

        // Get user name (nullable)
        fun getUserName(): String? = userName

        // Clear on logout
        fun clear() {
            userId = null
            userName = null
        }

}