package com.example.cryptomemes

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_action.*
import android.content.DialogInterface
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.util.Patterns
import android.widget.EditText



class ActionActivity : AppCompatActivity() {

    val TAG = "ActionActivity"
    val ENCRYPT = 0
    val DECRYPT = 1

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
            startActivityForResult(intent, ENCRYPT)
        }

        decrypt_btn.setOnClickListener {
            val alert = AlertDialog.Builder(this)
            alert.setTitle("Get image")
            alert.setMessage("Please paste received URL below:")

            val input = EditText(this)
            alert.setView(input)

            alert.setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                val url = input.text.toString()
                Log.d(TAG, "URL: $url")

                if(Patterns.WEB_URL.matcher(url).matches()) {
                    val intent = Intent(this, SelectFriendActivity::class.java)
                    startActivity(intent)
                }
                else{
                    Toast.makeText(this, "Input text is not a valid URL", Toast.LENGTH_LONG).show()
                }
                return@OnClickListener
            })

            alert.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { _, _ ->
                    // TODO Auto-generated method stub
                    return@OnClickListener
                })
            alert.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ENCRYPT && resultCode == android.support.v7.app.AppCompatActivity.RESULT_OK && data != null) {
            val msgImgUri: Uri = data.data as Uri
            val cr = this.contentResolver
            val type: String = cr.getType(msgImgUri) as String

            if (type.split("/").last().replace("\"", "") == "png") {
                Toast.makeText(this, "File should be in .jpg format", Toast.LENGTH_LONG).show()
                Log.d(TAG, "requestCode: $requestCode")
                Log.d(TAG, "msgImgUri: $msgImgUri")
                Log.d(TAG, "mimeType: $type")
                Log.d(TAG, "${type.split("/").last().replace("\"", "")}")
            }
            else {
                Log.d(TAG, "requestCode: $requestCode")
                Log.d(TAG, "msgImgUri: $msgImgUri")
                Log.d(TAG, "mimeType: $type")
                Log.d(TAG, "${type.split("/").last().replace("\"", "")}")

                val intent = Intent(this, EnterMessageActivity::class.java)
                //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        else if (requestCode == DECRYPT && resultCode == android.support.v7.app.AppCompatActivity.RESULT_OK && data != null) {

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add_friends -> {
                val intent = Intent(this, AddFriendsActivity::class.java)
                startActivity(intent)
            }
            R.id.logout -> {
                Log.d(TAG, "User ${FirebaseAuth.getInstance().currentUser?.email} logging out")
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
