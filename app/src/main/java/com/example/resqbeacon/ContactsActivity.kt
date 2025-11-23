package com.example.resqbeacon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resqbeacon.database.AppDatabase
import com.example.resqbeacon.database.Contact
import com.example.resqbeacon.databinding.ActivityContactsBinding
import com.example.resqbeacon.databinding.ItemContactBinding
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private val adapter = ContactAdapter { contactToDelete ->
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@ContactsActivity).contactDao().delete(contactToDelete)
            Toast.makeText(this@ContactsActivity, "Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                lifecycleScope.launch {
                    AppDatabase.getDatabase(applicationContext).contactDao().insert(Contact(name = name, phone = phone))
                    binding.etName.text?.clear()
                    binding.etPhone.text?.clear()
                    Toast.makeText(this@ContactsActivity, "Contact Saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext).contactDao().getAllContacts().collect { contacts ->
                adapter.submitList(contacts)
            }
        }
    }
}

class ContactAdapter(private val onDeleteClick: (Contact) -> Unit) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {
    private var list = listOf<Contact>()

    fun submitList(newList: List<Contact>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = list[position]
        holder.binding.tvContactName.text = contact.name
        holder.binding.tvContactPhone.text = contact.phone
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(contact) }
    }

    override fun getItemCount() = list.size
    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)
}