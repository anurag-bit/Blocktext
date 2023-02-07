package com.anurag.slimchat

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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

        //logic for adding data to RecyclerView
        mDbref.child("chat").child(senderRoom!!).child("message")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

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