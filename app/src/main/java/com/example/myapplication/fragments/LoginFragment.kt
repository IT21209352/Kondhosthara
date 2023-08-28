package com.example.myapplication.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.LoginActivity
import com.example.myapplication.MapsActivity
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginButton = view.findViewById<Button>(R.id.loginBtn)
        val loginEmail = view.findViewById<EditText>(R.id.editTextTextEmailAddress)
        val loginPassword = view.findViewById<EditText>(R.id.editTextTextPassword)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        val signupButton = view.findViewById<Button>(R.id.regBtn)
        // val passrestButton = view.findViewById<TextView>(R.id.frogot_password_link)

        val user = Firebase.auth.currentUser

        if (user != null){
            activity?.let {
                Toast.makeText(activity, "You have already logged in...", Toast.LENGTH_SHORT).show()
                val intent = Intent(it, MapsActivity::class.java)
                it.startActivity(intent)
            }
        }

        loginButton.setOnClickListener {

            progressBar.visibility = View.VISIBLE
            val email = loginEmail.text.toString()
            val password = loginPassword.text.toString()

            if(email == "" || password =="" ){
                Toast.makeText(activity, "Please Enter Email and Password ", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }else{

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener{ task ->
                        if (task.isSuccessful) {


                            Firebase.auth.currentUser?.let { it1 -> getUserType(it1.uid) }

                            progressBar.visibility = View.GONE

                        } else {
                            Toast.makeText(activity, "Authentication failed.", Toast.LENGTH_LONG).show()
                            progressBar.visibility = View.GONE
                        }
                    }
            }
        }

        signupButton.setOnClickListener {
          //  val transaction = requireFragmentManager().beginTransaction()
            val transaction =  parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView, RegisterFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun getUserType(uid: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(uid)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    val userType = dataSnapshot.child("type").getValue(String::class.java)

                    activity?.let {
                        val intent = Intent(it, MapsActivity::class.java)
                        it.startActivity(intent)
                    }

                }else{
                    Toast.makeText(activity, "User Data Not Found...", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(activity, "Connection error. Please try again...", Toast.LENGTH_LONG).show()

            }
        })

    }



}