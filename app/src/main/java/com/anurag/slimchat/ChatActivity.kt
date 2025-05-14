package com.anurag.slimchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Timer
import java.util.TimerTask

class ChatActivity : AppCompatActivity() {
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var userAvatar: ShapeableImageView
    private lateinit var userName: TextView
    private lateinit var userStatus: TextView
    private lateinit var messageAdapter: messageAdapter
    private lateinit var messageList: ArrayList<message>
    private lateinit var mDbref: DatabaseReference

    private lateinit var receiverRoom: String
    private lateinit var senderRoom: String

    private lateinit var receiverChatUid: String
    private lateinit var currentChatSenderUid: String

    private var isOtherUserTyping = false
    private var otherUserIsOnline: Boolean? = null
    private var otherUserLastSeenTimestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        createNotificationChannel()

        val name = intent.getStringExtra("name")
        val receiverUidFromIntent = intent.getStringExtra("uid")
        val senderUidFromAuth = FirebaseAuth.getInstance().currentUser?.uid

        if (senderUidFromAuth == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            Log.e("ChatActivity", "Current user UID is null. Finishing activity.")
            finish()
            return
        }
        currentChatSenderUid = senderUidFromAuth

        if (receiverUidFromIntent == null) {
            Toast.makeText(this, "Unable to start chat. Recipient not found.", Toast.LENGTH_LONG).show()
            Log.e("ChatActivity", "Receiver UID from intent is null. Finishing activity.")
            finish()
            return
        }
        receiverChatUid = receiverUidFromIntent

        mDbref = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverChatUid + currentChatSenderUid
        receiverRoom = currentChatSenderUid + receiverChatUid

        toolbar = findViewById(R.id.chat_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        userName = findViewById(R.id.chat_username)
        userStatus = findViewById(R.id.chat_status)
        userAvatar = findViewById(R.id.chat_avatar)

        userName.text = name

        toolbar.setNavigationOnClickListener {
            finish()
        }

        messageRecyclerView = findViewById(R.id.chatrecyclervview)
        messageBox = findViewById(R.id.messagebox)
        sendButton = findViewById(R.id.sendbutton)
        messageList = ArrayList()
        messageAdapter = messageAdapter(this, messageList)

        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter

        mDbref.child("chats").child(senderRoom).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    if (messageList.size > 0) {
                        messageRecyclerView.smoothScrollToPosition(messageList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatActivity", "Failed to load messages", error.toException())
                }
            })

        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText, currentChatSenderUid, receiverChatUid)
            }
        }

        setupUserPresenceListener(receiverChatUid)
        setupTypingIndicator(currentChatSenderUid, receiverChatUid)
    }

    private fun updateCombinedUserStatus() {
        if (isOtherUserTyping) {
            userStatus.text = getString(R.string.typing_status_active)
            userStatus.setTextColor(getColor(R.color.colorPrimary))
        } else {
            if (otherUserIsOnline == true) {
                userStatus.text = getString(R.string.online)
                userStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            } else if (otherUserLastSeenTimestamp != null) {
                val timeAgo = getTimeAgo(otherUserLastSeenTimestamp!!)
                userStatus.text = getString(R.string.last_seen, timeAgo)
                userStatus.setTextColor(getColor(R.color.on_surface_variant))
            } else {
                userStatus.text = getString(R.string.offline)
                userStatus.setTextColor(getColor(R.color.on_surface_variant))
            }
        }
    }

    private fun sendMessage(message: String, senderUid: String, receiverUid: String) {
        val messageObject = message(message, senderUid, System.currentTimeMillis(), false)
        val messageKey = mDbref.child("chats").child(senderRoom).child("messages").push().key

        if (messageKey == null) {
            Toast.makeText(this, "Failed to send message. Try again.", Toast.LENGTH_SHORT).show()
            Log.e("ChatActivity", "Generated messageKey is null.")
            return
        }

        mDbref.child("chats").child(senderRoom).child("messages").child(messageKey)
            .setValue(messageObject).addOnSuccessListener {
                mDbref.child("chats").child(receiverRoom).child("messages").child(messageKey)
                    .setValue(messageObject)
                sendNotification(receiverUid, message)
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ChatActivity", "Failed to send message", e)
            }
        messageBox.setText("")
    }

    private fun sendNotification(receiverUid: String, messageContent: String) {
        createNotificationChannel()
        val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore('@') ?: "User"
        val chatName = userName.text?.toString() ?: getString(R.string.app_name)

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("uid", receiverUid)
            putExtra("name", chatName)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            receiverUid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "main_channel")
            .setSmallIcon(android.R.drawable.sym_action_chat) // Replaced R.drawable.ic_stat_message with a system icon
            .setContentTitle(getString(R.string.notification_title, senderName))
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(getColor(R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@ChatActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(receiverUid.hashCode(), builder.build())
            } else {
                Log.w("ChatActivity", "POST_NOTIFICATIONS permission not granted.")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.main_channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("main_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupUserPresenceListener(userId: String) {
        val userRef = mDbref.child("users").child(userId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(user::class.java)
                    user?.let {
                        otherUserIsOnline = it.isOnline
                        otherUserLastSeenTimestamp = it.lastSeen
                    } ?: run {
                        otherUserIsOnline = false
                        otherUserLastSeenTimestamp = null
                        Log.w("ChatActivity", "User object deserialization failed for UID: $userId")
                    }
                } else {
                    otherUserIsOnline = false
                    otherUserLastSeenTimestamp = null
                    Log.w("ChatActivity", "User node does not exist for UID: $userId")
                }
                updateCombinedUserStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                otherUserIsOnline = false
                otherUserLastSeenTimestamp = null
                updateCombinedUserStatus()
                Log.e("ChatActivity", "User presence listener cancelled for UID: $userId", error.toException())
            }
        })
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - timestamp

        val secondsAgo = timeDiff / 1000

        return when {
            secondsAgo < 5 -> getString(R.string.time_ago_just_now)
            secondsAgo < 60 -> getString(R.string.time_ago_seconds, secondsAgo)
            secondsAgo < 3600 -> getString(R.string.time_ago_minutes, secondsAgo / 60)
            secondsAgo < 86400 -> getString(R.string.time_ago_hours, secondsAgo / 3600)
            else -> getString(R.string.time_ago_days, secondsAgo / 86400)
        }
    }

    private fun updateUserPresence(isOnline: Boolean) {
        val userRef = mDbref.child("users").child(currentChatSenderUid)

        if (isOnline) {
            userRef.child("isOnline").setValue(true)
            userRef.child("lastSeen").removeValue()
        } else {
            val updateMap = mapOf(
                "isOnline" to false,
                "lastSeen" to System.currentTimeMillis()
            )
            userRef.updateChildren(updateMap)
        }
    }

    private fun setupTypingIndicator(currentUserId: String, otherUserId: String) {
        val typingRefCurrentUserWrites = mDbref.child("typing_status").child(otherUserId).child(currentUserId)
        val typingRefListenToOtherUser = mDbref.child("typing_status").child(currentUserId).child(otherUserId)

        typingRefListenToOtherUser.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isOtherUserTyping = snapshot.getValue(Boolean::class.java) == true
                updateCombinedUserStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                isOtherUserTyping = false
                updateCombinedUserStatus()
                Log.e("ChatActivity", "Typing indicator listener cancelled for other user: $otherUserId", error.toException())
            }
        })

        messageBox.addTextChangedListener(object : android.text.TextWatcher {
            private var timer = Timer()
            private val TYPING_DELAY = 1500L

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                timer.cancel()
                if (s.toString().isNotEmpty()) {
                    typingRefCurrentUserWrites.setValue(true)
                    timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            this@ChatActivity.runOnUiThread {
                                typingRefCurrentUserWrites.setValue(false)
                            }
                        }
                    }, TYPING_DELAY)
                } else {
                    typingRefCurrentUserWrites.setValue(false)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        if (::currentChatSenderUid.isInitialized) {
            updateUserPresence(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::currentChatSenderUid.isInitialized) {
            updateUserPresence(false)
            if (::receiverChatUid.isInitialized) {
                mDbref.child("typing_status").child(receiverChatUid).child(currentChatSenderUid).setValue(false)
            }
        }
    }
}