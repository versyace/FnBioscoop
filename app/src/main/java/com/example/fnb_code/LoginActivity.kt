package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvError    = findViewById<TextView>(R.id.tvError)
        val btnLogin   = findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignUp   = findViewById<TextView>(R.id.tvGoToSignUp)
        val btnLoginAdmin = findViewById<MaterialButton>(R.id.btnLoginAdmin)

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

            // Cegah login admin lewat form pelanggan
            if (username == "admin") {
                tvError.text = "Gunakan tombol 'Login sebagai Admin' di bawah"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (UserPrefs.loginPelanggan(this, username, password)) {
                tvError.visibility = View.GONE
                val intent = Intent(this, MenuActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                tvError.text = "Username atau password salah"
                tvError.visibility = View.VISIBLE
            }
        }

        btnLoginAdmin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
