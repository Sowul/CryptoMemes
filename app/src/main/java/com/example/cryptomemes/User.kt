package com.example.cryptomemes

import android.util.Log
import net.kibotu.pgp.Pgp
import java.util.*

val TAG = "KEYS"

class User(val uid: String, val username: String, val email: String, val avaUrl: String,
           val publicKey: String, val privateKey: String) {

    init {
        Log.d(TAG, "Public: $publicKey")
        Log.d(TAG, "Private: $privateKey")
    }
}