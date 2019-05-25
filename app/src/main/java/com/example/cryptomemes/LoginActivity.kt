package com.example.cryptomemes

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_btn.setOnClickListener {
            login()
        }

        back2reg_txtview.setOnClickListener {
            finish()
        }
    }

    private fun login() {
        val email = email_edittxt_log.text.toString()
        val password = pass_edittxt_log.text.toString()

        Log.d(TAG, "Email: $email")
        Log.d(TAG, "Password: $password")

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill email/password field.", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                Log.d(TAG, "GOOD Logged in as: ${it.result?.user?.uid}")

                val intent = Intent(this, ActionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "BAD Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}