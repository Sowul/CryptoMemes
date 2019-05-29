package com.example.cryptomemes

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_decrypt.*
import kotlinx.android.synthetic.main.activity_enter_message.*
import moe.tlaster.kotlinpgp.KotlinPGP
import net.kibotu.pgp.Pgp
import java.io.File
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

class DecryptActivity : AppCompatActivity() {

    val TAG = "DecryptActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        supportActionBar?.title = "Decrypted message"

        Log.d(TAG, intent.getStringExtra("from"))
        Log.d(TAG, intent.getStringExtra("uid"))
        Log.d(TAG, intent.getStringExtra("url"))

        decryptMsg()
    }

    override fun onBackPressed() {
        val intent = Intent(this, ActionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun decryptMsg() {
        val uid = getInstance().uid ?: ""

        var myPublicKey = ""
        var myPrivateKey = ""

        val refPub = FirebaseDatabase.getInstance().getReference("/users/pub").child(uid)
        refPub.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                myPublicKey = user!!.publicKey
            }
            override fun onCancelled(p0: DatabaseError) {}
        })

        val refPriv = FirebaseDatabase.getInstance().getReference("/users/priv").child(uid)
        refPriv.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                myPrivateKey = user!!.privateKey
            }
            override fun onCancelled(p0: DatabaseError) {}
        })

        val pass = (uid + FirebaseAuth.getInstance().currentUser!!.metadata!!.creationTimestamp.toString())// as String?
        Log.d(TAG, "pass:\n$pass")

        val filename = intent.getStringExtra("url").split('/').last()

        try {
            FirebaseStorage.getInstance().getReference("/msgs/$filename")
        } catch (e: Exception) {
            Log.d(TAG, "testo")
            e.printStackTrace()
            Toast.makeText(this, "Message does not exist", Toast.LENGTH_LONG).show()
            val intent = Intent(this, ActionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        val ref = FirebaseStorage.getInstance().getReference("/msgs/$filename")

        var id = ""
        var metadataUrl = ""
        var msgSize = 0

        val progressOverlay = findViewById<FrameLayout>(R.id.progress_overlay)
        progressOverlay.visibility = View.VISIBLE
        val thread = Thread(Runnable {
            try {
                ref.metadata.addOnSuccessListener {
                    // Metadata now contains the metadata for 'images/forest.jpg'
                    id = it.getCustomMetadata("id")
                    metadataUrl = it.getCustomMetadata("url")
                    msgSize = it.getCustomMetadata("size").toInt()

                    Log.d(TAG, "id: $id")
                    Log.d(TAG, "url: $metadataUrl")
                    Log.d(TAG, "size: $msgSize")
                }.addOnFailureListener {
                    // Uh-oh, an error occurred!
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                progressOverlay.visibility = View.INVISIBLE
                Toast.makeText(this, "Message does not exist", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ActionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        thread.start()

        val thread2 = Thread(Runnable {
            try {
                val localFile = File.createTempFile("images", "jpg")
                Log.d(TAG, "localFile $localFile created")

                ref.getFile(localFile).addOnSuccessListener {
                    // Local temp file has been created
                    Log.d(TAG, "Local temp file has been created")
                    if (localFile.length() != 0L) {
                        progressOverlay.visibility = View.VISIBLE
                        val decodedMsg = test(localFile, id, metadataUrl, msgSize, myPublicKey, myPrivateKey, pass)
                        progressOverlay.visibility = View.INVISIBLE
                        decrypted_txtview.text = decodedMsg
                    }
                    else {
                        Log.d(TAG, "localFile puste, coś poszło źle :(")
                    }
                }.addOnFailureListener {
                    // Handle any errors
                    progressOverlay.visibility = View.INVISIBLE
                    Toast.makeText(this, "Message does not exist", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, ActionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        })

        thread2.start()
    }

    private fun test(localFile: File,
                     id: String,
                     metadataUrl: String,
                     msgSize: Int,
                     myPublicKey: String,
                     myPrivateKey: String,
                     pass: String) : String {

        Pgp.setPublicKey(myPublicKey)
        Pgp.setPrivateKey(myPrivateKey)

        val base64 = Base64.getEncoder().encodeToString(localFile.readBytes())
        Log.d(TAG, "base64 len: ${base64.length}")
        Log.d(TAG, "base64 start: ${base64.substring(0, 10)}")
        Log.d(TAG, "base64 end: ${base64.takeLast(10)}")

        Log.d(TAG, "msgSize: $msgSize")
        val zippedB64 = base64.takeLast(msgSize)
        Log.d(TAG, "encryptedMsg len: ${zippedB64.length}")
        Log.d(TAG, "encryptedMsg end: ${zippedB64.takeLast(10)}")
        val zippedEncString = Base64.getDecoder().decode(zippedB64)
        val encString = ungzip(zippedEncString)
        Log.d(TAG, "encString:${encString::class.java.simpleName}")

        if (localFile.exists()) {
            Log.d(TAG, "outputFile: $localFile deleted")
            localFile.delete()
        }

        Log.d(TAG, "NONE OF THESE IS NULL FFS")
        Log.d(TAG, encString)

        if (id == intent.getStringExtra("uid") && metadataUrl == intent.getStringExtra("url")) {
            val result = KotlinPGP.decrypt(
                myPrivateKey,
                pass,
                encString
            )
            return result.result
        }
        else {
            return "Could not verify whether received message is legitimate."
        }
    }

    private fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
}