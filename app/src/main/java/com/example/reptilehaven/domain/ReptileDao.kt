package com.example.reptilehaven.domain

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ReptileDao {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var databaseReference: DatabaseReference = FirebaseDatabase
        .getInstance()
        .getReference(firebaseAuth.currentUser!!.uid)


    fun addReptile(reptile: Reptile) =
        databaseReference.child(Reptile::class.java.simpleName).push().setValue(reptile)

}