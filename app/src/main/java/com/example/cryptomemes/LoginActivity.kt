package com.example.cryptomemes

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_btn.setOnClickListener {
            val email = email_edittxt_log.text.toString()
            val password = pass_edittxt_log.text.toString()

            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Password: $password")
        }

        back2reg_txtview.setOnClickListener {
            finish()
        }
    }
}