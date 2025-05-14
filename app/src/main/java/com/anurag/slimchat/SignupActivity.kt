package com.anurag.slimchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class SignupActivity : AppCompatActivity() {

    private lateinit var edtName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnLoginRedirect: MaterialButton
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbref: FirebaseDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()
        
        // Initialize UI components
        nameInputLayout = findViewById(R.id.name_input_layout)
        emailInputLayout = findViewById(R.id.email_input_layout)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        edtName = findViewById(R.id.editTextTextPersonName)
        edtEmail = findViewById(R.id.editTextTextEmailAddress)
        edtPassword = findViewById(R.id.editTextTextPassword)
        btnSignup = findViewById(R.id.signup_button)
        btnLoginRedirect = findViewById(R.id.login_redirect_button)
        
        // Set up click listeners
        btnSignup.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            
            if (validateForm(name, email, password)) {
                signup(name, email, password)
            }
        }
        
        btnLoginRedirect.setOnClickListener {
            // Navigate back to login screen
            finish()
        }
    }
    
    private fun validateForm(name: String, email: String, password: String): Boolean {
        var isValid = true
        
        // Validate name
        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            isValid = false
        } else {
            nameInputLayout.error = null
        }
        
        // Validate email
        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Enter a valid email address"
            isValid = false
        } else {
            emailInputLayout.error = null
        }
        
        // Validate password
        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordInputLayout.error = null
        }
        
        return isValid
    }    private fun signup(name: String, email: String, password: String) {
        // Show loading state
        btnSignup.isEnabled = false
        btnSignup.text = "Creating Account..."
        
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Reset button state
                btnSignup.isEnabled = true
                btnSignup.text = getString(R.string.sign_up_primary)
                
                if (task.isSuccessful) {
                    // Sign up success, add user to database
                    adduserToDatabase(name, email, mAuth.currentUser?.uid!!)
                    
                    // Navigate to main activity
                    val intent = Intent(this@SignupActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // If sign up fails, display the error message
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    Toast.makeText(this@SignupActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun adduserToDatabase(name: String, email: String, uid: String) {
        val mDbref = FirebaseDatabase.getInstance().reference
        
        // Create user with enhanced data
        val newUser = user(
            name = name,
            email = email,
            uid = uid,
            profileImageUrl = null,
            status = "Hey there! I'm using BlockText",
            lastSeen = System.currentTimeMillis(),
            isOnline = true
        )
        
        // Save to "users" node in the database
        mDbref.child("users").child(uid).setValue(newUser).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // User data saved successfully
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // If saving user data fails, display the error message
                val errorMessage = task.exception?.message ?: "Failed to save user data"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}




