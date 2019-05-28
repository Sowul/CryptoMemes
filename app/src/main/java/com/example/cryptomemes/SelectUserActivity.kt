package com.example.cryptomemes

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_select_user.*

class SelectUserActivity : AppCompatActivity() {

    val TAG = "SelectUserActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user)

        supportActionBar?.title = "Select User"

        val adapter = GroupAdapter<ViewHolder>()
        rv_select_user.adapter = adapter

        getUsers(adapter)
    }

    private fun getUsers(adapter: GroupAdapter<ViewHolder>) {
        val ref = FirebaseDatabase.getInstance().getReference("/users/pub")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                val uid = FirebaseAuth.getInstance().uid ?: ""
                p0.children.forEach {
                    val user = it.getValue(User::class.java)

                    if (user != null && user.uid != uid) {
                        Log.d(TAG, "Adding user ${user.username} - uid: ${user.uid}")
                        adapter.add(UserItem(user))
                    }
                }

                adapter.setOnItemClickListener { item, view ->

                    val userItem = item as UserItem

                    Log.d(TAG, "User ${userItem.user.username} clicked")
                    Log.d(TAG, "PublicKey:]\n${userItem.user.publicKey}")

                    if (intent.getStringExtra("from") == "DECRYPT") {
                        Log.d(TAG, "FROM: ${intent.getStringExtra("from")}")
                        val intent2 = Intent(this@SelectUserActivity, DecryptActivity::class.java)
                        intent2.putExtra("from", intent.getStringExtra("from"))
                        intent2.putExtra("uid", userItem.user.uid)
                        intent2.putExtra("url", intent.getStringExtra("url"))
                        startActivity(intent2)
                    }
                    else if (intent.getStringExtra("from") == "ENCRYPT") {
                        Log.d(TAG, "FROM: ${intent.getStringExtra("from")}")
                        val intent2 = Intent(this@SelectUserActivity, EnterMessageActivity::class.java)
                        intent2.putExtra("from", intent.getStringExtra("from"))
                        intent2.putExtra("publicKey", userItem.user.publicKey)
                        intent2.putExtra("img", intent.getStringExtra("img"))
                        startActivity(intent2)
                    }
                    else {
                        Log.d(TAG, "Something went wrong")
                    }
                }

                rv_select_user.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {}
        })
    }
}