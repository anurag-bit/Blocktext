package com.anurag.slimchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {    
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<user>
    private lateinit var adapter: userAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbref: DatabaseReference
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabNewChat: FloatingActionButton    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()
        mDbref = FirebaseDatabase.getInstance().getReference()

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Initialize UI components
        userRecyclerView = findViewById(R.id.user_recyclerView)
        fabNewChat = findViewById(R.id.fab_new_chat)
          // Set up FAB click listener
        fabNewChat.setOnClickListener {
            Toast.makeText(this, "Create new chat", Toast.LENGTH_SHORT).show()
            // You can add implementation for creating a new chat
        }

        // Set up recycler view
        userList = ArrayList()
        adapter = userAdapter(this, userList)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter
        
        // Load user list
        mDbref.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(user::class.java)
                    if (mAuth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun updateUserPresence(isOnline: Boolean) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val userRef = mDbref.child("users").child(currentUserId)
        
        if (isOnline) {
            userRef.child("isOnline").setValue(true)
        } else {
            val updateMap = mapOf(
                "isOnline" to false,
                "lastSeen" to System.currentTimeMillis()
            )
            userRef.updateChildren(updateMap)
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUserPresence(true)
    }
    
    override fun onPause() {
        super.onPause()
        updateUserPresence(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }    
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_logout -> {
                // Logic for logout
                mAuth.signOut()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                finish()
                startActivity(intent)
                return true
            }
            R.id.menu_settings -> {
                // Handle settings action
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_edit_profile -> {
                // Handle edit profile action
                Toast.makeText(this, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_notifications -> {
                // Handle notifications action
                Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}