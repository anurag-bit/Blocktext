package com.anurag.slimchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {


    private lateinit var edtName: EditText
    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnsignup: Button
    private lateinit var mDbref: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        mAuth = FirebaseAuth.getInstance()
        edtName = findViewById(R.id.editTextTextPersonName)
        btnsignup = findViewById(R.id.signup_button)
        edtPassword = findViewById(R.id.editTextTextPassword)
        edtEmail = findViewById(R.id.editTextTextEmailAddress)
        mAuth = FirebaseAuth.getInstance()




        btnsignup.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            val name = edtName.text.toString()

            signup(name, email,password)

        }

    }

    private  fun  signup(name:String, email: String, password: String){
        //signup functionality
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                   //jump to home_activity
                    //add user to db
                    adduserToDatabase(name, email, mAuth.currentUser?.uid!!)
                    val intent = Intent(this@SignupActivity, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                    //add use to data base

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this@SignupActivity, "Some Error Occurred", Toast.LENGTH_SHORT).show()

                }
            }
       // fun adduserToDatabase(name: String, email:String, uid: String ){

        }

    }

    private fun adduserToDatabase(name: String, email: String, uid: String) {

        var mDbref = FirebaseDatabase.getInstance().reference

        mDbref.child("user").child(uid).setValue(user(name, email, uid))
    }


