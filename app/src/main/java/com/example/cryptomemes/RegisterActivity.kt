package com.example.cryptomemes

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_btn.setOnClickListener {
            register()
        }

        have_acc_txtview.setOnClickListener {
            Log.d(TAG, "Showing login activity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun register() {
        val email = email_edittxt_reg.text.toString()
        val password = pass_edittxt_reg.text.toString()

        Log.d(TAG, "Email: $email")
        Log.d(TAG, "Password: $password")
    }
}
