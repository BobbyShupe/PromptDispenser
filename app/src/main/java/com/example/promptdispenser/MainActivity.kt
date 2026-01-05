package com.example.promptdispenser.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.promptdispenser.data.PromptDatabase
import com.example.promptdispenser.data.PromptListEntity
import com.example.promptdispenser.data.PromptRepository
import com.example.promptdispenser.databinding.ActivityMainBinding
import com.example.promptdispenser.databinding.DialogEditListBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PromptViewModel by viewModels {
        PromptViewModelFactory(PromptRepository(PromptDatabase.getDatabase(this).promptDao()))
    }

    private lateinit var adapter: PromptListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PromptListAdapter(
            onDispense = { list -> startDispenser(list) },
            onReset = { list -> viewModel.update(list.copy(usedPrompts = emptyList())) },
            onDelete = { list ->
                AlertDialog.Builder(this)
                    .setTitle("Delete List")
                    .setMessage("Are you sure you want to delete \"${list.name}\"? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.delete(list)
                        Toast.makeText(this, "List deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allLists.observe(this) { rawLists ->
            if (rawLists.isEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            val sortedLists = rawLists.sortedByDescending { it.createdAt }

            val groupedList = mutableListOf<Any>()

            var currentDate: Date? = null
            for (list in sortedLists) {
                val listDate = Date(list.createdAt)
                val calendar = Calendar.getInstance().apply {
                    time = listDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val listDayStart = calendar.time

                if (currentDate == null || listDayStart != currentDate) {
                    groupedList.add(listDayStart)
                    currentDate = listDayStart
                }
                groupedList.add(list)
            }

            adapter.submitList(groupedList)
        }

        // Add new list
        binding.fabAdd.setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun showEditDialog(existingList: PromptListEntity?) {
        val dialogBinding = DialogEditListBinding.inflate(LayoutInflater.from(this))
        val isEdit = existingList != null

        if (isEdit) {
            dialogBinding.editName.setText(existingList!!.name)
            dialogBinding.editPrompts.setText(existingList.allPrompts.joinToString("\n\n"))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit List" else "New Prompt List")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Save" else "Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = !dialogBinding.editName.text.isNullOrBlank()

            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    positiveButton.isEnabled = !dialogBinding.editName.text.isNullOrBlank()
                }
            }
            dialogBinding.editName.addTextChangedListener(textWatcher)

            positiveButton.setOnClickListener {
                val name = dialogBinding.editName.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "List name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val promptsText = dialogBinding.editPrompts.text.toString()
                val prompts = promptsText.split(Regex("(\\n\\s*){2,}"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                if (prompts.isEmpty()) {
                    Toast.makeText(this, "Add at least one prompt", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val listEntity = if (isEdit) {
                    existingList!!.copy(
                        name = name,
                        allPrompts = prompts
                    )
                } else {
                    PromptListEntity(
                        name = name,
                        allPrompts = prompts
                    )
                }

                if (isEdit) {
                    viewModel.update(listEntity)
                } else {
                    viewModel.insert(listEntity)
                }

                dialog.dismiss()
                Toast.makeText(this, if (isEdit) "List updated" else "List created", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun startDispenser(list: PromptListEntity) {
        val intent = Intent(this, DispenserActivity::class.java)
        intent.putExtra("list_id", list.id)
        startActivity(intent)
    }
}