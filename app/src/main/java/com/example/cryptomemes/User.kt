package com.example.cryptomemes

import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.user_row.view.*

val TAG = "KEYS"

class User(val uid: String, val username: String, val email: String, val avaUrl: String,
           val publicKey: String, val privateKey: String) {

    constructor(): this("", "", "", "", "", "")
    constructor(uid: String, username: String, avaUrl: String, publicKey: String):
            this(uid, username, "", avaUrl, publicKey, "")
    constructor(uid: String, email: String, privateKey: String):
            this(uid, "", email, "", "", privateKey)
}

class UserItem(val user: User): Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.user_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.rv_username.text = user.username
        Picasso.get().load(user.avaUrl).into(viewHolder.itemView.rv_ava)
    }
}