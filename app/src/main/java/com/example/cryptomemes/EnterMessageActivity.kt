package com.example.cryptomemes

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class EnterMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_message)

        supportActionBar?.title = "Enter Message"
    }
}
