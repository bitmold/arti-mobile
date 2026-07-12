// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT
package org.torproject.sample.arti

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.lang.ref.WeakReference
import java.util.regex.Pattern
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import org.torproject.sample.arti.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var logReceiver: BroadcastReceiver

    private lateinit var bridgeLineList: ArrayList<EditText>
    private lateinit var fabSpin: ViewPropertyAnimator
    private lateinit var selectedOption: SelectedPluggableTransport
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as App

        // edge-to-edge to make app render correctly on modern versions of android
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }


        binding.floatingActionButton.setOnClickListener { checkConnection() }
        fabSpin = binding.floatingActionButton.animate()

        binding.startButton.setOnClickListener { startArti() }
        binding.stopButton.setOnClickListener { app.stopArti() }

        bridgeLineList = ArrayList()
        bridgeLineList.add(binding.bridgeLineInput)


        binding.logLabel.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding.logScrollView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        logReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val logMessage = intent.getStringExtra(App.EXTRA_LOG_MESSAGE_KEY)
                appendLog(logMessage)
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            logReceiver,
            IntentFilter(App.ACTION_LOG_MESSAGE)
        )

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.connection_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        binding.spinner.adapter = adapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedOption = SelectedPluggableTransport.entries[position]
                onSelectionChanged(selectedOption) // get options
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        binding.buttonAdd.setOnClickListener { addNewEditText() }
        binding.buttonRemove.setOnClickListener { removeEditText() }
        binding.inputScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.inputScrollView.post { binding.inputScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }

    private fun appendLog(text: String?) {
        binding.logTextView.append(text)
        binding.logScrollView.post { binding.logScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startArti() {
//        stopButton.setEnabled(true);
        binding.text.setText(R.string.intro_text)
        when (selectedOption) {
            SelectedPluggableTransport.NO_PT -> app.connectTorDirect()
            SelectedPluggableTransport.OBFS4 -> {
                val lyreBirdBridgeLines = collectInputs()
                if (lyreBirdBridgeLines.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.no_lyrebird_bridgelines), Toast.LENGTH_LONG
                    ).show()
                    return
                }
                app.connectWithLyrebird(
                    binding.obfs4Port.text.toString().toInt(),
                    lyreBirdBridgeLines
                )
            }

            SelectedPluggableTransport.SNOWFLAKE -> {
                val stunServers = binding.stunServerInput.text.toString()
                val target = binding.targetInput.text.toString()
                val front = binding.frontInput.text.toString()
                val snowflakeBridgesLines = collectInputs()
                if (snowflakeBridgesLines.isEmpty()) {
                    Toast.makeText(
                        this, R.string.no_snowflake_bridge_lines, Toast.LENGTH_LONG
                    ).show()
                    return
                }
                app.connectWithSnowflake(
                    stunServers,
                    target,
                    front,
                    snowflakeBridgesLines
                )
            }

            else -> {}
        }

        Handler(mainLooper).postDelayed({ checkConnection() }, 2000)
    }

    private fun checkConnection() {
        binding.floatingActionButton.isEnabled = false
        fabSpin.setDuration((1000 * 60).toLong()).rotationBy((1000 * 60).toFloat() / 4)
            .setInterpolator(LinearInterpolator()).start()
        binding.status.visibility = View.VISIBLE
        binding.status.setText(R.string.performing_request)

        CheckTorConnectionTask(this).execute()
    }

    private class CheckTorConnectionTask(mainActivity: MainActivity?) :
        AsyncTask<Void?, Void?, TorConnectionStatus>() {
        private val mainActivityWeakReference: WeakReference<MainActivity?> =
            WeakReference<MainActivity?>(mainActivity)

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): TorConnectionStatus {
            mainActivityWeakReference.get() ?: return TorConnectionStatus.ERROR
            return Helpers.checkTorProxyConnectivity("localhost", 9150)
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: TorConnectionStatus) {
            val mainActivity = mainActivityWeakReference.get() ?: return
            mainActivity.onCheckTorConnectionTaskCompleted(result)
        }
    }

    private fun onCheckTorConnectionTaskCompleted(s: TorConnectionStatus) {
        fabSpin.cancel()
        binding.floatingActionButton.isEnabled = true
        when (s) {
            TorConnectionStatus.DIRECT -> binding.status.setText(R.string.tor_no_arti)
            TorConnectionStatus.TOR -> binding.status.setText(R.string.tor_with_arti)
            TorConnectionStatus.ERROR -> binding.status.setText(R.string.connection_failed)
        }
        binding.floatingActionButton.isActivated = true
    }

    private fun addNewEditText() {
        val newEditText = EditText(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        // convert to dp for correct margins
        val resources = this.getResources()
        val metrics = resources.displayMetrics
        val px = (15 * (metrics.densityDpi / 160f)).toInt()
        params.marginStart = px
        params.marginEnd = px
        newEditText.layoutParams = params

        newEditText.setHint(R.string.enter_bridge_line)
        newEditText.backgroundTintList = ColorStateList.valueOf("#03DAC5".toColorInt()) // teal hex
        newEditText.isSingleLine = true

        // Add the new EditText above the add/remove buttons
        binding.inputFieldsContainer.addView(newEditText, binding.inputFieldsContainer.size - 1)

        // Add the new EditText to the list
        bridgeLineList.add(newEditText)
    }

    private fun removeEditText() {
        if (binding.inputFieldsContainer.size > 6) {
            binding.inputFieldsContainer.removeViewAt(binding.inputFieldsContainer.size - 2)
        }
    }

    private fun collectInputs(): MutableList<String?> {
        val inputs: MutableList<String?> = ArrayList()
        var matchFound: Boolean
        for (editText in bridgeLineList) {
            val text = editText.text.toString()
            if (selectedOption == SelectedPluggableTransport.OBFS4) {
                // guidance needed: how detailed should I go with this regex? not sure how
                // flexible port names can be
                val pattern = Pattern.compile("^obfs4 ", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(text)
                matchFound = matcher.find()
            } else {
                matchFound = true
            }
            if (matchFound) {
                inputs.add(editText.text.toString())
            } else {
                binding.text.setText(R.string.syntax_err)
                return ArrayList()
            }
        }
        return inputs
    }

    private fun onSelectionChanged(s: SelectedPluggableTransport) {
        val constraintSet = ConstraintSet()
        when (s) {
            SelectedPluggableTransport.NO_SELECTION -> {
                setDefaultVisibilities()
                constraintSet.clone(binding.constraintLayout)
            }

            SelectedPluggableTransport.NO_PT -> {
                setDirectVisibilities()
                constraintSet.clone(binding.constraintLayout)
                constraintSet.connect(
                    binding.logLabel.id,
                    ConstraintSet.TOP,
                    binding.spinner.id,
                    ConstraintSet.BOTTOM
                )
            }

            SelectedPluggableTransport.OBFS4 -> {
                setLyrebirdVisibilities()
                constraintSet.clone(binding.constraintLayout)
                constraintSet.connect(
                    binding.logLabel.id,
                    ConstraintSet.TOP,
                    binding.inputScrollView.id,
                    ConstraintSet.BOTTOM
                )
            }

            SelectedPluggableTransport.SNOWFLAKE -> {
                setSnowflakeVisibilities()
                constraintSet.clone(binding.constraintLayout)
                constraintSet.connect(
                    binding.logLabel.id,
                    ConstraintSet.TOP,
                    binding.inputScrollView.id,
                    ConstraintSet.BOTTOM
                )
            }
        }
        constraintSet.applyTo(binding.constraintLayout)
    }

    private fun setDefaultVisibilities() {
        binding.noOptionSelected.visibility = View.VISIBLE
        binding.logScrollView.visibility = View.GONE
        binding.logLabel.visibility = View.GONE
        binding.inputScrollView.visibility = View.GONE
        //  startButton.setEnabled(false);
        binding.floatingActionButton.isEnabled = false
    }

    private fun setDirectVisibilities() {
        binding.stunServerInput.visibility = View.GONE
        binding.obfs4Port.visibility = View.GONE
        binding.targetInput.visibility = View.GONE
        binding.frontInput.visibility = View.GONE
        binding.bridgeLineInput.visibility = View.GONE
        binding.noOptionSelected.visibility = View.GONE
        binding.logLabel.visibility = View.VISIBLE
        binding.inputScrollView.visibility = View.GONE
        binding.buttonAdd.visibility = View.GONE
        //    startButton.setEnabled(true);
        binding.floatingActionButton.isEnabled = true
    }

    private fun setLyrebirdVisibilities() {
        binding.bridgeLineInput.visibility = View.VISIBLE
        binding.obfs4Port.visibility = View.VISIBLE
        binding.stunServerInput.visibility = View.GONE
        binding.targetInput.visibility = View.GONE
        binding.frontInput.visibility = View.GONE
        binding.noOptionSelected.visibility = View.GONE
        binding.logLabel.visibility = View.VISIBLE
        binding.inputScrollView.visibility = View.VISIBLE
        binding.buttonAdd.visibility = View.VISIBLE
        //    startButton.setEnabled(true);
        binding.floatingActionButton.isEnabled = true

    }

    private fun setSnowflakeVisibilities() {
        binding.bridgeLineInput.visibility = View.VISIBLE
        binding.stunServerInput.visibility = View.VISIBLE
        binding.targetInput.visibility = View.VISIBLE
        binding.frontInput.visibility = View.VISIBLE
        binding.noOptionSelected.visibility = View.GONE
        binding.obfs4Port.visibility = View.GONE
        binding.logLabel.visibility = View.VISIBLE
        binding.inputScrollView.visibility = View.VISIBLE
        binding.buttonAdd.visibility = View.VISIBLE
        //    startButton.setEnabled(true);
        binding.floatingActionButton.isEnabled = true
    }
}