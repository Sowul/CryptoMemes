package com.example.cryptomemes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.android.synthetic.main.activity_enter_message.*
import net.kibotu.pgp.Pgp
import okhttp3.Response
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8



class EnterMessageActivity : AppCompatActivity() {

    val TAG = "EnterMessageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_message)

        supportActionBar?.title = "Enter Message"

        msg_btn.setOnClickListener {
            if (msg_edittxt.text.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
            else {
                val msg = msg_edittxt.text.toString()
                val uid = FirebaseAuth.getInstance().uid ?: ""

                var myPublicKey = ""
                var myPrivateKey = ""

                val refPub = FirebaseDatabase.getInstance().getReference("/users/pub").child(uid)
                refPub.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        val user = p0.getValue(User::class.java)
                        myPublicKey = user?.publicKey as String
                        //Log.d(TAG, "myPublicKey:\n$myPublicKey")
                    }
                    override fun onCancelled(p0: DatabaseError) {}
                })

                val refPriv = FirebaseDatabase.getInstance().getReference("/users/priv").child(uid)
                refPriv.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        val user = p0.getValue(User::class.java)
                        myPrivateKey = user?.privateKey as String
                        //Log.d(TAG, "myPrivateKey:\n$myPrivateKey")
                    }
                    override fun onCancelled(p0: DatabaseError) {}
                })

                //val myPublicKey = ref.child("pub").child(uid).key("publicKey")

                val publicKey = intent.getStringExtra("publicKey")
                val imgUri = Uri.parse(intent.getStringExtra("img"))

                Log.d(TAG, "$imgUri")
                Log.d(TAG, msg)
                //Log.d(TAG, "$publicKey")

                Pgp.setPublicKey(publicKey)
                //Pgp.setPublicKey(myPublicKey)
                //Pgp.setPrivateKey(myPrivateKey)
                val pass = FirebaseAuth.getInstance().uid + FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp.toString()
                Log.d(TAG, "pass:\n$pass")
                val encrypted = Pgp.encrypt(msg)
                Log.d(TAG, "enc:\n${encrypted.toString()}")

                val content = encrypted.toString()
                Log.d(TAG, "size of original: ${content.length}")
                val zipped = gzip(content)
                Log.d(TAG, zipped.toString())
                Log.d(TAG, "size zipped: ${zipped.size}")
                val unzipped = ungzip(zipped)
                Log.d(TAG, "size unzipped: ${unzipped.length}")
                Log.d(TAG, "${assert(unzipped == content)}")
                //val decrypted = Pgp.decrypt(encrypted.toString(), pass)
                //Log.d(TAG, "dec:\n${decrypted.toString()}")

                /*
                var a = ""
                for (b in zipped) {
                    val st = String.format("%02X", b)
                    a += st
                }
                */
                //Log.d(TAG, "Co to: $a")

                //val f = contentResolver.openInputStream(img).readBytes()
                //Log.d(TAG, f.toString())


                uploadEncryptedMsg(imgUri, msg)
            }
        }
    }

    fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }

    fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }

    private fun uploadEncryptedMsg(imgUri: Uri?, msg: String) {
        val clientId = ""

        val img = Base64.getEncoder().encodeToString(contentResolver.openInputStream(imgUri!!).readBytes())
        Log.d(TAG, "a: $img")
        Log.d(TAG, "a: ${img.length}")

        val thread = Thread(Runnable {
            try {
                val response: Response = httpPost {

                    url("https://api.imgur.com/3/image")

                    header { "Authorization" to "Client-ID $clientId" }

                    body {
                        form {
                            "image" to img
                        }
                    }
                }
                Log.d(TAG, response.isSuccessful.toString())
                if (response.isSuccessful) {
                    val json = JSONObject(response.body()?.string())
                    val link = json.getJSONObject("data").get("link").toString()

                    Log.d(TAG, "imgur url: $link")

                    encryptAndUploadMsg(link, imgUri, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        thread.start()
    }

    private fun encryptAndUploadMsg(imgurUrl: String, imgUri: Uri?, msg: String) {
        Log.d(TAG, "encryptAndUploadMsg filename: $imgurUrl")
        Log.d(TAG, "encryptAndUploadMsg msg: $msg")

        //TODO tu spróbuję wrzucić to razem z metadatą
        //Działa
        val filename = imgurUrl.split('/').last()
        Log.d(TAG, "imgur url: $filename")

        val ref = FirebaseStorage.getInstance().getReference("/msgs/$filename")
        var metadata = StorageMetadata.Builder()
            .setCustomMetadata("imgurUrl", imgurUrl)
            .setCustomMetadata("msgSize", msg.length.toString())
            .build()

        ref.putFile(imgUri!!, metadata)
            .addOnSuccessListener {
                Log.d(TAG, "GOOD Successfully uploaded image: ${it.metadata?.path}")
                Log.d(TAG, "GOOD Metadata set for $it")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "File url: $it")
                }
                ref.metadata.addOnSuccessListener {
                    Log.d(TAG, "GOOD Metadata imgurUrl: ${it.getCustomMetadata("imgurUrl")}")
                    Log.d(TAG, "GOOD Metadata msgSize: ${it.getCustomMetadata("msgSize")}")

                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Imgur URL", imgurUrl)
                    clipboard.primaryClip = clip

                    if (clipboard.hasPrimaryClip()) {
                        Toast.makeText(this, "$imgurUrl is in your clipboard.\nYou can now send it to you friend.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "BAD Failed to upload image: ${it.message}")
            }
    }
}