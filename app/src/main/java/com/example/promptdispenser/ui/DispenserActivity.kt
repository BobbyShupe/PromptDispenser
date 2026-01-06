package com.example.promptdispenser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.promptdispenser.data.PromptDatabase
import com.example.promptdispenser.data.PromptListEntity
import com.example.promptdispenser.data.PromptRepository
import com.example.promptdispenser.databinding.ActivityDispenserBinding
import com.example.promptdispenser.util.Prefs

class DispenserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispenserBinding
    private val viewModel: PromptViewModel by viewModels {
        PromptViewModelFactory(PromptRepository(PromptDatabase.getDatabase(this).promptDao()))
    }
    private var currentList: PromptListEntity? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isDelayActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispenserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listId = intent.getLongExtra("list_id", -1L)

        viewModel.allLists.observe(this) { lists ->
            currentList = lists.find { it.id == listId } ?: return@observe
            updateUI()
        }

        binding.btnNext.setOnClickListener {
            if (!isDelayActive) {
                dispenseNext()
            }
        }
    }

    private fun updateUI() {
        val list = currentList ?: return
        binding.textListName.text = list.name
        val remaining = list.allPrompts.size - list.usedPrompts.size
        binding.textStatus.text = "$remaining prompts remaining"

        if (remaining == 0) {
            binding.textPreview.text = "All prompts used! Reset the list to start over."
            binding.btnNext.isEnabled = false
        } else {
            binding.btnNext.isEnabled = !isDelayActive
        }
    }

    private fun dispenseNext() {
        val list = currentList ?: return
        val available = list.allPrompts.filter { !list.usedPrompts.contains(it) }
        if (available.isEmpty()) {
            Toast.makeText(this, "No more prompts!", Toast.LENGTH_SHORT).show()
            return
        }

        val nextPrompt = available.random()  // Random selection

        // Copy to clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Prompt", nextPrompt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_LONG).show()

        binding.textPreview.text = nextPrompt

        // Mark as used
        val updated = list.copy(usedPrompts = list.usedPrompts + nextPrompt)
        viewModel.update(updated)
        currentList = updated

        updateUI()

        // Start cooldown delay
        startDelayCooldown()
    }

    private fun startDelayCooldown() {
        val delaySeconds = Prefs.getDelaySeconds(this)
        if (delaySeconds <= 0) return

        isDelayActive = true
        binding.btnNext.isEnabled = false

        handler.postDelayed({
            isDelayActive = false
            updateUI()  // Re-enable if prompts remain
        }, delaySeconds * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}