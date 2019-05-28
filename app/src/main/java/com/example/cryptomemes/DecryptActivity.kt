package com.example.cryptomemes

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import junit.framework.Assert
import net.kibotu.pgp.Pgp
import java.util.*

class DecryptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)
    }
}