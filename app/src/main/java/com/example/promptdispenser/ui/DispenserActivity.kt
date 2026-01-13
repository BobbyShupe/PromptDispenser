package com.example.promptdispenser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.promptdispenser.data.PromptDatabase
import com.example.promptdispenser.data.PromptListEntity
import com.example.promptdispenser.data.PromptRepository
import com.example.promptdispenser.databinding.ActivityDispenserBinding
import com.example.promptdispenser.util.Prefs
import java.util.Locale
import java.util.concurrent.TimeUnit

class DispenserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispenserBinding
    private val viewModel: PromptViewModel by viewModels {
        PromptViewModelFactory(PromptRepository(PromptDatabase.getDatabase(this).promptDao()))
    }
    private var currentList: PromptListEntity? = null
    private var countdownTimer: CountDownTimer? = null
    private var remainingMillis: Long = 0L

    companion object {
        private const val KEY_REMAINING_MILLIS = "remaining_cooldown_millis"
        private const val PREFS_NAME = "dispense_prefs"
        private const val KEY_COOLDOWN_END = "cooldown_end_time"
    }

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
            if (remainingMillis <= 0) dispenseNext()
        }

        binding.btnSkipForward.setOnClickListener {
            skipForward()
        }

        binding.btnSkipBack.setOnClickListener {
            skipBackward()
        }

        // Restore state
        savedInstanceState?.let {
            remainingMillis = it.getLong(KEY_REMAINING_MILLIS, 0L)
            if (remainingMillis > 0) startCountdown(remainingMillis)
        }

        restorePersistentCooldown()
    }

    private fun restorePersistentCooldown() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedEndTime = prefs.getLong(KEY_COOLDOWN_END, 0L)

        val now = System.currentTimeMillis()
        if (savedEndTime > now) {
            remainingMillis = savedEndTime - now
            startCountdown(remainingMillis)
        } else {
            prefs.edit().remove(KEY_COOLDOWN_END).apply()
            remainingMillis = 0L
        }
        updateUI()
    }

    private fun updateUI() {
        val list = currentList ?: return
        binding.textListName.text = list.name
        val remainingPrompts = list.allPrompts.size - list.usedPrompts.size
        binding.textStatus.text = "$remainingPrompts prompts remaining"

        if (remainingPrompts == 0) {
            binding.textPreview.text = "All prompts used! Reset the list to start over."
            binding.btnNext.isEnabled = false
            binding.btnSkipForward.isEnabled = false
            binding.btnSkipBack.isEnabled = list.usedPrompts.isNotEmpty()
            binding.tvCountdown.visibility = View.VISIBLE
        } else {
            val isReady = remainingMillis <= 0
            binding.btnNext.isEnabled = isReady
            binding.btnSkipForward.isEnabled = remainingPrompts > 0
            binding.btnSkipBack.isEnabled = list.usedPrompts.isNotEmpty()
            binding.tvCountdown.visibility = if (remainingMillis > 0) View.VISIBLE else View.GONE
        }
    }

    private fun dispenseNext() {
        performDispense(copyToClipboard = true, startDelay = true, undoUsed = false)
    }

    private fun skipForward() {
        performDispense(copyToClipboard = false, startDelay = false, undoUsed = false)
    }

    private fun skipBackward() {
        val list = currentList ?: return
        val used = list.usedPrompts
        if (used.isEmpty()) {
            Toast.makeText(this, "No previous prompt to go back to", Toast.LENGTH_SHORT).show()
            return
        }

        // Undo the last dispense: remove the most recent used prompt
        val newUsed = used.dropLast(1)
        val previousPrompt = used.last()

        val updated = list.copy(usedPrompts = newUsed)
        viewModel.update(updated)
        currentList = updated

        binding.textPreview.text = previousPrompt

        Toast.makeText(this, "Went back to previous prompt", Toast.LENGTH_SHORT).show()

        updateUI()
        // No delay affected
    }

    private fun performDispense(copyToClipboard: Boolean, startDelay: Boolean, undoUsed: Boolean) {
        val list = currentList ?: return
        val available = list.allPrompts.filter { !list.usedPrompts.contains(it) }
        if (available.isEmpty()) {
            Toast.makeText(this, "No more prompts!", Toast.LENGTH_SHORT).show()
            return
        }

        val nextPrompt = available.random()

        if (copyToClipboard) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AI Prompt", nextPrompt)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Skipped forward", Toast.LENGTH_SHORT).show()
        }

        binding.textPreview.text = nextPrompt

        val updatedUsed = if (undoUsed) {
            list.usedPrompts.dropLast(1)
        } else {
            list.usedPrompts + nextPrompt
        }

        val updated = list.copy(usedPrompts = updatedUsed)
        viewModel.update(updated)
        currentList = updated

        updateUI()

        if (startDelay) {
            startDelayCooldown()
        }
    }

    private fun startDelayCooldown() {
        val delaySeconds = Prefs.getDelaySeconds(this)
        if (delaySeconds <= 0) return

        remainingMillis = TimeUnit.SECONDS.toMillis(delaySeconds.toLong())

        val endTime = System.currentTimeMillis() + remainingMillis
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_COOLDOWN_END, endTime)
            .apply()

        startCountdown(remainingMillis)
        updateUI()
    }

    private fun startCountdown(millisInFuture: Long) {
        stopCountdown()

        countdownTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                binding.tvCountdown.text = String.format(Locale.US, "Next available in: %02d:%02d", minutes, seconds)
                binding.tvCountdown.visibility = View.VISIBLE
            }

            override fun onFinish() {
                remainingMillis = 0L
                binding.tvCountdown.visibility = View.GONE
                updateUI()
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_COOLDOWN_END)
                    .apply()
            }
        }.start()
    }

    private fun stopCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_REMAINING_MILLIS, remainingMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }
}