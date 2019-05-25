package com.example.cryptomemes

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_action.*

class ActionActivity : AppCompatActivity() {

    val TAG = "ActionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action)

        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        encrypt_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        decrypt_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == android.support.v7.app.AppCompatActivity.RESULT_OK && data != null) {
            val msgImgUri = data.data
            val cr = this.contentResolver
            val type = cr.getType(data.data)

            if (type.split("/").last().replace("\"", "") == "png") {
                Toast.makeText(this, "File should be in .jpg format", Toast.LENGTH_LONG).show()
                Log.d(TAG, "msgImgUri: $msgImgUri")
                Log.d(TAG, "mimeType: $type")
                Log.d(TAG, "${type.split("/").last().replace("\"", "")}")
                return
            }

            Log.d(TAG, "msgImgUri: $msgImgUri")
            Log.d(TAG, "mimeType: $type")
            Log.d(TAG, "${type.split("/").last().replace("\"", "")}")
        }
    }
}
