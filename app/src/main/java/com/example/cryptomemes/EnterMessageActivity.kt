package com.example.cryptomemes

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
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.android.synthetic.main.activity_enter_message.*
import net.kibotu.pgp.Pgp
import okhttp3.Response
import org.json.JSONObject
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8



class EnterMessageActivity : AppCompatActivity() {

    val TAG = "EnterMessageActivity"

    val clientId = ""
    val clientSecret = ""

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
                val img = Uri.parse(intent.getStringExtra("img"))

                Log.d(TAG, "$img")
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

                var a = ""
                for (b in zipped) {
                    val st = String.format("%02X", b)
                    a += st
                }
                Log.d(TAG, "Co to: $a")

                val f = contentResolver.openInputStream(img).readBytes()
                Log.d(TAG, f.toString())

                val thread = Thread(Runnable {
                    try {
                        val response: Response = httpPost {

                            url("https://api.imgur.com/3/image")

                            //param { ... }
                            header { "Authorization" to "Client-ID $clientId" }

                            //TODO FFS jak to zrobić?
                            body {
                                form {                              //  Resulting form will not contain ' ', '\t', '\n'
                                    bytes(File(img).readBytes())             //  login=user&
                                    //"type" to "file" //  email=john.doe@gmail.com
                                }
                            }
                            /* DZIAŁa
                            body {
                                form {                              //  Resulting form will not contain ' ', '\t', '\n'
                                    "image" to "https://i.imgur.com/35A6opg.png"               //  login=user&
                                    //"type" to "url" //  email=john.doe@gmail.com
                                }
                            }
                            */
                        }
                        Log.d(TAG, response.isSuccessful.toString())
                        val json = JSONObject(response.body()?.string()).getJSONObject("data").get("link")
                        Log.d(TAG, json.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })

                thread.start()
            }
            //val user =
            //val intent = Intent(Intent.ACTION_PICK)
            //intent.type = "image/*"
            //startActivityForResult(intent, 0)
        }
    }

    fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }

    fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }

    private fun createImageFile(uri: Uri): File {
        val stream = FileOutputStream(uri.toString())
        val writer = OutputStreamWriter(stream)
        writer.write("")
        writer.flush()
        writer.close()
        stream.close()
        return File(uri.toString())
    }
}