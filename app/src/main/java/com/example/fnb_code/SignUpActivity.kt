package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etNama       = findViewById<TextInputEditText>(R.id.etNama)
        val etUsername   = findViewById<TextInputEditText>(R.id.etUsernameSignUp)
        val etPassword   = findViewById<TextInputEditText>(R.id.etPasswordSignUp)
        val etKonfirmasi = findViewById<TextInputEditText>(R.id.etKonfirmasiPassword)
        val tvError      = findViewById<TextView>(R.id.tvErrorSignUp)
        val btnDaftar    = findViewById<MaterialButton>(R.id.btnDaftar)
        val tvGoLogin    = findViewById<TextView>(R.id.tvGoToLogin)

        btnDaftar.setOnClickListener {
            val nama       = etNama.text.toString().trim()
            val username   = etUsername.text.toString().trim()
            val password   = etPassword.text.toString()
            val konfirmasi = etKonfirmasi.text.toString()

            when {
                nama.isEmpty() -> {
                    tvError.text = "Nama tidak boleh kosong"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                username.isEmpty() -> {
                    tvError.text = "Username tidak boleh kosong"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                username.length < 4 -> {
                    tvError.text = "Username minimal 4 karakter"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    tvError.text = "Password tidak boleh kosong"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    tvError.text = "Password minimal 6 karakter"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                password != konfirmasi -> {
                    tvError.text = "Konfirmasi password tidak cocok"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
            }

            val berhasil = UserPrefs.register(this, nama, username, password)
            if (berhasil) {
                UserPrefs.login(this, username, password)
                startActivity(Intent(this, MenuActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } else {
                tvError.text = "Username sudah digunakan, coba yang lain"
                tvError.visibility = View.VISIBLE
            }
        }

        tvGoLogin.setOnClickListener {
            finish()
        }
    }
}