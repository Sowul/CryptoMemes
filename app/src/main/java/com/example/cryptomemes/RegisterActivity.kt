package com.example.cryptomemes

import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import net.kibotu.pgp.Pgp
import java.util.*

class RegisterActivity : AppCompatActivity() {

    val TAG = "RegisterActivity"
    var avaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_btn.setOnClickListener {
            register()
        }

        have_acc_txtview.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        ava_btn_reg.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == android.support.v7.app.AppCompatActivity.RESULT_OK && data != null) {
            avaUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, avaUri)

            ava_imageview_reg.setImageBitmap(bitmap)
            ava_btn_reg.alpha = 0f
        }
    }

    private fun register() {
        val email = email_edittxt_reg.text.toString()
        val password = pass_edittxt_reg.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in email/password field", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Creating user with email: $email")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                Log.d(TAG, "GOOD Created user with uid: ${it.result?.user?.uid}")

                uploadAva(avaUri)
            }
            .addOnFailureListener {
                Log.d(TAG, "BAD Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadAva(avaUri: Uri?) {
        if (avaUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(avaUri!!)
            .addOnSuccessListener {
                Log.d(TAG, "GOOD Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "File url: $it")

                    saveUser(it.toString())
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "BAD Failed to upload image: ${it.message}")
            }
    }

    private fun saveUser(avaUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val timestamp = FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp.toString()
        val ref = FirebaseDatabase.getInstance().getReference("/users")

        val krgen = Pgp.generateKeyRingGenerator((uid + timestamp).toCharArray())
        val publicKey = Pgp.genPGPPublicKey(krgen)
        val privateKey = Pgp.genPGPPrivKey(krgen)

        val user = User(uid, username_edittxt_reg.text.toString(), email_edittxt_reg.text.toString(), avaUrl, publicKey, privateKey)

        saveUserData(user, ref)
    }

    private fun saveUserData(user: User, ref: DatabaseReference) {
        try {
            ref.child("pub").child(user.uid).child("uid").setValue(user.uid)
            ref.child("pub").child(user.uid).child("username").setValue(user.username)
            ref.child("pub").child(user.uid).child("avaUrl").setValue(user.avaUrl)
            ref.child("pub").child(user.uid).child("publicKey").setValue(user.publicKey)
            ref.child("priv").child(user.uid).child("uid").setValue(user.uid)
            ref.child("priv").child(user.uid).child("privateKey").setValue(user.privateKey)
            ref.child("priv").child(user.uid).child("email").setValue(user.email)

            Log.d(TAG, "GOOD User saved to database")

            val intent = Intent(this, ActionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        catch (t: Throwable) {
            Log.d(TAG, "BAD Failed to save user to database: ${t.message}")
        }
        finally {}
    }
}