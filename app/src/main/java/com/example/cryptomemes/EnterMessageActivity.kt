package com.example.cryptomemes

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.android.synthetic.main.activity_enter_message.*
import net.kibotu.pgp.Pgp
import okhttp3.Response
import org.json.JSONObject
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission is not granted")

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1)
        }

        Log.d(TAG, "encryptAndUploadMsg filename: $imgurUrl")
        Log.d(TAG, "encryptAndUploadMsg msg: $msg")

        val imgBase64 = Base64.getEncoder().encodeToString(contentResolver.openInputStream(imgUri!!).readBytes())
        Log.d(TAG, "returned imgBase64 len: ${imgBase64.length}")
        val encryptedMsg = encryptMsg(msg)
        Log.d(TAG, "returned encMsg len: ${encryptedMsg.length}")
        val msgInImg = concat(imgBase64, encryptedMsg)

        val filename = imgurUrl.split('/').last()
        Log.d(TAG, "imgur url: $filename")

        val ref = FirebaseStorage.getInstance().getReference("/msgs/$filename")
        var metadata = StorageMetadata.Builder()
            .setCustomMetadata("id", FirebaseAuth.getInstance().uid)
            .setCustomMetadata("url", imgurUrl)
            .setCustomMetadata("size", encryptedMsg.length.toString())
            .build()

        val imageByteArray = Base64.getDecoder().decode(msgInImg)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-kkmmss"))
        val outputFile = File(Environment.getExternalStorageDirectory().absolutePath + "/temp-$timestamp.jpg")
        outputFile.writeBytes(imageByteArray)
        Log.d(TAG, "outputFile: $outputFile")

        ref.putFile(Uri.fromFile(outputFile), metadata)
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

                    // TODO Toast zamienić na alert?
                    if (clipboard.hasPrimaryClip()) {
                        Toast.makeText(this, "$imgurUrl is in your clipboard.\nYou can now send it to you friend.", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "img start: ${imgBase64.substring(0, 10)}")
                        Log.d(TAG, "encMsg end: ${encryptedMsg.takeLast(10)}")
                        Log.d(TAG, "suma start: ${msgInImg.substring(0, 10)}")
                        Log.d(TAG, "suma end: ${msgInImg.takeLast(10)}")
                        if (outputFile.exists()) {
                            Log.d(TAG, "outputFile: $outputFile deleted")
                            outputFile.delete()
                        }
                        val intent = Intent(this, ActionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "BAD Failed to upload image: ${it.message}")
            }
    }

    private fun encryptMsg(msg: String): String {
        val publicKey = intent.getStringExtra("publicKey")

        Log.d(TAG, "GOOD encryptMsg: $msg")
        Log.d(TAG, "$publicKey")

        Pgp.setPublicKey(publicKey)

        val encString = Pgp.encrypt(msg)

        Log.d(TAG, "size of original: ${encString?.length}")
        val zippedEncString = gzip(encString!!)
        Log.d(TAG, "size zipped: ${zippedEncString.size}")
        val zipped2B64 = Base64.getEncoder().encodeToString(zippedEncString)
        Log.d(TAG, "size encZipped2Base64: ${zipped2B64.length}")

        return zipped2B64
    }

    private fun concat(img: String, msg: String): String {
        var testImg = ""
        if (img.takeLast(2) == "==") {
            testImg = img.dropLast(2)
            testImg += "++"
        }
        else if (img.takeLast(1) == "=") {
            testImg = img.dropLast(1)
            testImg += "+"
        }
        else {
            return img+msg
        }
        return testImg+msg
    }

    private fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }
}