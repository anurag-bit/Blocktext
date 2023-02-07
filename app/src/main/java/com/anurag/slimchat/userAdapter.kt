package com.anurag.slimchat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import  androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class userAdapter(private val context: Context, private val userList: ArrayList<user>) :
    RecyclerView.Adapter<userAdapter.userviewholder>() {
    class userviewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.txt_name)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): userviewholder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.userlayout, parent, false)
        return userviewholder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: userviewholder, position: Int) {
        val currenUser = userList[position]
        holder.textName.text = currenUser.name

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java )
            intent.putExtra("name", currenUser.name)
            intent.putExtra("uid", currenUser.uid)

            context.startActivity(intent)
        }
    }
}