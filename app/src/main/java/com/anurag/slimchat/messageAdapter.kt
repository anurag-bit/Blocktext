package com.anurag.slimchat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class messageAdapter(val context: Context, val messageList: ArrayList<message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    val ITEM_RECEIVED = 1
    val ITEM_SENT = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_RECEIVED) {
            // Inflate received layout
            val view: View = LayoutInflater.from(context).inflate(R.layout.recieved, parent, false)
            ReceivedViewHolder(view)
        } else {
            // Inflate sent layout
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
            SentViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        
        // Format timestamp
        val timeString = if (currentMessage.timestamp != null) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(currentMessage.timestamp!!))
        } else {
            "Just now"
        }
        
        if (holder is SentViewHolder) {
            // Process for sent view
            holder.sentMessage.text = currentMessage.message
            holder.messageTime.text = timeString
            
            // Set read status indicator
            holder.statusIcon.setImageResource(
                if (currentMessage.isRead == true) android.R.drawable.ic_dialog_email 
                else android.R.drawable.ic_menu_send
            )
            holder.statusIcon.visibility = View.VISIBLE
        } else if (holder is ReceivedViewHolder) {
            // Process for received view
            holder.receivedMessage.text = currentMessage.message
            holder.messageTime.text = timeString
            
            // Show avatar for first message from this sender or after different sender
            val showAvatar = position == 0 || 
                (position > 0 && messageList[position-1].senderId != currentMessage.senderId)
            
            holder.senderAvatar.visibility = if (showAvatar) View.VISIBLE else View.INVISIBLE
            
            // Mark message as read
            if (currentMessage.isRead == false) {
                currentMessage.isRead = true
                // Update in database
                updateMessageReadStatus(currentMessage)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return if (FirebaseAuth.getInstance().currentUser?.uid == currentMessage.senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVED
        }
    }

    private fun updateMessageReadStatus(message: message) {
        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val otherUserId = message.senderId
        
        if (currentUserId != null && otherUserId != null) {
            // Create room ID
            val roomId = otherUserId + currentUserId
              // Find message in database by content and sender ID
            FirebaseDatabase.getInstance().getReference("chats")
                .child(roomId)
                .child("messages")
                .orderByChild("timestamp")
                .equalTo(message.timestamp?.toDouble() ?: 0.0)
                .limitToFirst(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        for (messageSnap in snapshot.children) {
                            // Update read status
                            messageSnap.ref.child("isRead").setValue(true)
                        }
                    }
                }
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageBubble: MaterialCardView = itemView.findViewById(R.id.message_bubble_sent)
        val sentMessage: TextView = itemView.findViewById(R.id.txt_sent)
        val messageTime: TextView = itemView.findViewById(R.id.message_time_sent)
        val statusIcon: ImageView = itemView.findViewById(R.id.status_sent)
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageBubble: MaterialCardView = itemView.findViewById(R.id.message_bubble_received)
        val receivedMessage: TextView = itemView.findViewById(R.id.txt_recieved)
        val messageTime: TextView = itemView.findViewById(R.id.message_time_received)
        val senderAvatar: ShapeableImageView = itemView.findViewById(R.id.sender_avatar)
    }
}