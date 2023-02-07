package com.anurag.slimchat

import android.content.Context
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.firebase.auth.FirebaseAuth

class messageAdapter(val context: Context, val messageList: ArrayList<message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val ITEM_RECIEVED = 1;
    val ITEM_SENT = 2;


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == 1) {
            //inflate recieved
            val view: View = LayoutInflater.from(context).inflate(R.layout.recieved, parent, false)
            return recievedViewholder(view)

        } else {
            //inflate sent
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
            return sentViewholder(view)
        }
    }

    override fun getItemCount(): Int {
         return messageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        if (holder.javaClass == sentViewholder::class.java) {
            //do the process for sent view


            val viewholder = holder as sentViewholder
            holder.sentMessage.text = currentMessage.message
        } else {
            //do process for recieved
            val viewholder = holder as recievedViewholder
            holder.recievedMessage.text = currentMessage.message

        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            return ITEM_SENT
        } else {
            return ITEM_RECIEVED

        }
    }

    class sentViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val sentMessage = itemView.findViewById<TextView>(R.id.txt_sent)
    }

    class recievedViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val recievedMessage = itemView.findViewById<TextView>(R.id.txt_recieved)
    }
}