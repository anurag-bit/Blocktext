package com.anurag.slimchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: messageAdapter
    private lateinit var messageList: ArrayList<message>
    private lateinit var mDbref: DatabaseReference

    var recieverRoom: String? = null
    var senderRoom: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val recieverUid = intent.getStringExtra("uid")
        val name = intent.getStringExtra("name")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbref = FirebaseDatabase.getInstance().reference
        senderRoom = recieverUid + senderUid
        recieverRoom = senderUid + recieverUid

        supportActionBar?.title = name

        messageRecyclerView = findViewById(R.id.chatrecyclervview)
        messageBox = findViewById(R.id.messagebox)
        sendButton = findViewById(R.id.sendbutton)
        messageList = ArrayList()
        messageAdapter = messageAdapter(this, messageList)

        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name0Channel = getString(R.string.main_channel)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val CHANNEL_ID = "MNC_0"
            val mChannel = NotificationChannel(CHANNEL_ID, name0Channel, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = this@ChatActivity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)


        }

        //logic for adding data to RecyclerView
        mDbref.child("chat").child(senderRoom!!).child("message")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    with(NotificationManagerCompat.from(this@ChatActivity)) {
                        // notificationId is a unique int for each notification that you must define
                        if (ActivityCompat.checkSelfPermission(
                                this@ChatActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        val CHANNEL_ID = "MNC_0"
                        val builder = NotificationCompat.Builder(this@ChatActivity, CHANNEL_ID)
                            .setSmallIcon(R.drawable.baseline_notifications_none_24)
                            .setContentTitle("check for messages")
                            .setContentText("You may have new messages...")
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText("You may have new messages...please check app"))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        val notificationId = 230307
                        notify(notificationId, builder.build())
                    }




                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(message::class.java)
                        messageList.add(message!!)
                    }


                    messageAdapter.notifyDataSetChanged()

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })


        //adding the message to database!

        sendButton.setOnClickListener {

            val message = messageBox.text.toString()
            val messageObject = message(message, senderUid)

            mDbref.child("chat").child(senderRoom!!).child("message").push()
                .setValue(messageObject).addOnSuccessListener {
                    mDbref.child("chat").child(recieverRoom!!).child("message").push()
                        .setValue(messageObject)

                }
            messageBox.setText("")

        }

    }
}