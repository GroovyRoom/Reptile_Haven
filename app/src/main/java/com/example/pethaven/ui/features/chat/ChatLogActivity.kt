package com.example.pethaven.ui.features.chat

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pethaven.R
import com.example.pethaven.domain.ChatMessage
import com.example.pethaven.domain.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_receiving.view.*
import kotlinx.android.synthetic.main.chat_sending.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 *  Activity for messaging with other user
 */
class ChatLogActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatLog"
        var currentUser: User? = null
    }

    var toUser : User? = null
    val adapter = GroupAdapter<ViewHolder>()

    private lateinit var speechLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        fetchCurrentUser()
        setUpSpeechLauncher()

        recyclerview_chat_log.adapter = adapter

        toUser = intent.getParcelableExtra<User>(ChatFragment.USER_KEY)
        println("debug: " + toUser?.uid)

        supportActionBar?.title = toUser?.username

        listenForMessages()

        send_button_chat_log.setOnClickListener {
            Log.d(TAG, "Attempt to send message....")
            performSendMessage()
        }

        chatMicButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking")
            }
            speechLauncher.launch(intent)
        }
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                currentUser = p0.getValue(User::class.java)
            }
            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    private fun setUpSpeechLauncher() {
        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                val data = it.data
                edittext_chat_log.setText(
                    edittext_chat_log.text.toString() +
                    (data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: "")
                )
            }
        }
    }

    private fun performSendMessage() {
        // how do we actually send a message to firebase...
        val text = edittext_chat_log.text.toString()
        if (text.compareTo("") == 0) {
            return
        }

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(ChatFragment.USER_KEY)
        val toId = user?.uid

        if (fromId == null) return

        //val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val df: DateFormat = SimpleDateFormat("HH:mm")
        val theTime: String = df.format(Calendar.getInstance().getTime())
        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId.toString(),
            System.currentTimeMillis() / 1000, theTime)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object: ChildEventListener {

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.fromId.compareTo(FirebaseAuth.getInstance().uid.toString()) == 0) {
                        val currentUser = currentUser
                        adapter.add(ChatToItem(chatMessage.text, chatMessage.timeString, currentUser!!))
                    } else {
                        adapter.add(ChatFromItem(chatMessage.text, chatMessage.timeString, toUser!!))
                        recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)
                    }
                }

            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }

        })

    }
}

class ChatToItem(val text: String, val timeString: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_sending.text = text
        viewHolder.itemView.textView_sending_time.text = timeString
        viewHolder.itemView.textView_sender.text = user.username

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_sending
        Picasso.get().load(uri).into(targetImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_sending
    }
}

class ChatFromItem(val text: String, val timeString: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_receiving.text = text
        viewHolder.itemView.textView_receiving_time.text = timeString
        viewHolder.itemView.textView_receiver.text = user.username

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_receiving
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_receiving
    }
}