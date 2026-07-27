package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val etUsername = findViewById<TextInputEditText>(R.id.etAdminUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etAdminPassword)
        val tvError    = findViewById<TextView>(R.id.tvAdminError)
        val btnLogin   = findViewById<MaterialButton>(R.id.btnAdminLogin)
        val btnBack    = findViewById<View>(R.id.btnBackAdmin)

        btnBack.setOnClickListener { finish() }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty()) {
                tvError.text = "Username tidak boleh kosong"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tvError.text = "Password tidak boleh kosong"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (UserPrefs.loginAdmin(this, username, password)) {
                tvError.visibility = View.GONE
                val intent = Intent(this, AdminDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                tvError.text = "Username atau password admin salah"
                tvError.visibility = View.VISIBLE
            }
        }
    }
}
