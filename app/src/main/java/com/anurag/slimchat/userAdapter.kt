package com.anurag.slimchat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class userAdapter(private val context: Context, private val userList: ArrayList<user>) :
    RecyclerView.Adapter<userAdapter.UserViewHolder>() {
    
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userCard: MaterialCardView = itemView.findViewById(R.id.user_card)
        val textName: TextView = itemView.findViewById(R.id.txt_name)
        val avatar: ShapeableImageView = itemView.findViewById(R.id.user_avatar)
        val lastMessage: TextView = itemView.findViewById(R.id.txt_last_message)
        val messageTime: TextView = itemView.findViewById(R.id.txt_time)
        val unreadIndicator: TextView = itemView.findViewById(R.id.unread_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.userlayout, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        
        // Set user name
        holder.textName.text = currentUser.name
        
        // Set default values
        holder.lastMessage.text = "No messages yet"
        holder.messageTime.text = ""
        holder.unreadIndicator.visibility = View.GONE
        
        // Setup room IDs for fetching last message
        val receiverId = currentUser.uid
        val senderId = currentUserId
        val senderRoom = receiverId + senderId
        
        // Fetch last message
        FirebaseDatabase.getInstance().getReference("chats")
            .child(senderRoom)
            .child("messages")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (messageSnap in snapshot.children) {
                            val message = messageSnap.getValue(message::class.java)
                            message?.let {
                                // Set last message text
                                holder.lastMessage.text = it.message
                                
                                // Format and set timestamp
                                if (it.timestamp != null) {
                                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                    holder.messageTime.text = sdf.format(Date(it.timestamp!!))
                                } else {
                                    holder.messageTime.text = "Recent"
                                }
                                
                                // Set unread indicator
                                if (it.isRead == false && it.senderId != currentUserId) {
                                    holder.unreadIndicator.visibility = View.VISIBLE
                                    holder.unreadIndicator.text = "1" // Could count unread messages
                                } else {
                                    holder.unreadIndicator.visibility = View.GONE
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // Set click listener
        holder.userCard.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            context.startActivity(intent)
        }
    }
}