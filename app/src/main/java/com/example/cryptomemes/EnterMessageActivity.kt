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
import junit.framework.Assert.assertTrue
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
                val imgUri = Uri.parse(intent.getStringExtra("img"))
                val msg = msg_edittxt.text.toString()

                uploadEncryptedMsg(imgUri, msg)
            }
        }
    }

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

        val encryptedMsg = encryptMsg(msg)
        Log.d(TAG, "returned encMsg len: ${encryptedMsg.length}")

        val filename = imgurUrl.split('/').last()
        Log.d(TAG, "imgur url: $filename")

        val ref = FirebaseStorage.getInstance().getReference("/msgs/$filename")
        var metadata = StorageMetadata.Builder()
            .setCustomMetadata("id", FirebaseAuth.getInstance().uid)
            .setCustomMetadata("url", imgurUrl)
            .setCustomMetadata("size", encryptedMsg.length.toString())
            .build()

        //dodać encMsg to img

        ref.putFile(imgUri!!, metadata)
            .addOnSuccessListener {
                Log.d(TAG, "GOOD Successfully uploaded image: ${it.metadata?.path}")
                Log.d(TAG, "GOOD Metadata set for $it")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "File url: $it")
                }
                ref.metadata.addOnSuccessListener {
                    Log.d(TAG, "GOOD Metadata id: ${it.getCustomMetadata("id")}")
                    Log.d(TAG, "GOOD Metadata imgurUrl: ${it.getCustomMetadata("url")}")
                    Log.d(TAG, "GOOD Metadata msgSize: ${it.getCustomMetadata("size")}")

                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Imgur URL", imgurUrl)
                    clipboard.primaryClip = clip

                    // TODO zamienić na alert?
                    if (clipboard.hasPrimaryClip()) {
                        Toast.makeText(this, "$imgurUrl is in your clipboard.\nYou can now send it to you friend.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "BAD Failed to upload image: ${it.message}")
            }
    }

    private fun encryptMsg(msg: String): String {
        //val uid = FirebaseAuth.getInstance().uid ?: ""

        /*
        var myPublicKey = ""
        var myPrivateKey = ""

        val refPub = FirebaseDatabase.getInstance().getReference("/users/pub").child(uid)
        refPub.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                myPublicKey = user?.publicKey as String
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
        */
        val publicKey = intent.getStringExtra("publicKey")//klucz wcześniej wybranego usera

        Log.d(TAG, "GOOD encryptMsg: $msg")
        Log.d(TAG, "$publicKey")

        Pgp.setPublicKey(publicKey)
        //Pgp.setPublicKey(myPublicKey)
        //Pgp.setPrivateKey(myPrivateKey)
        //val pass = FirebaseAuth.getInstance().uid + FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp.toString()
        //Log.d(TAG, "pass:\n$pass")

        //etest
        val encString = Pgp.encrypt(msg)
        Log.d(TAG, "size of original: ${encString?.length}")
        val zippedEncString = gzip(encString!!)
        Log.d(TAG, "size zipped: ${zippedEncString.size}")
        val zipped2B64 = Base64.getEncoder().encodeToString(zippedEncString)
        Log.d(TAG, "size encZipped2Base64: ${zipped2B64.length}")

        //dtest
        val b64toZip = Base64.getDecoder().decode(zipped2B64)
        Log.d(TAG, "size b64toZip: ${b64toZip.size}")
        val unzipped = ungzip(b64toZip)
        Log.d(TAG, "size unzipped: ${unzipped.length}")
        Log.d(TAG, "${assertTrue(unzipped == encString)}")

        return zipped2B64
    }

    private fun concat(img: String, msg: String): String {
        return img+msg
    }

    fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }

    fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
}