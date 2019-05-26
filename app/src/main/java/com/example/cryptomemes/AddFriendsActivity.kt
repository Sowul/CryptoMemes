package com.example.cryptomemes

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_add_friends.*

class AddFriendsActivity : AppCompatActivity() {

    val TAG = "AddFriendsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friends)

        supportActionBar?.title = "Add friends"

        val adapter = GroupAdapter<ViewHolder>()
        rv_add_friends.adapter = adapter
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

                rv_add_friends.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {}
        })
    }
}