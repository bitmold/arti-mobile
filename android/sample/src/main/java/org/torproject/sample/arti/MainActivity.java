// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT

package org.torproject.sample.arti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private FloatingActionButton fab;
    private ViewPropertyAnimator fabSpin;
    private Spinner spinner;
    private ScrollView inputScrollView;
    private LinearLayout inputLayout;
    private EditText bridgeLineInput;
    private EditText obfs4Port;
    private EditText stunServerInput;
    private EditText targetInput;
    private EditText frontInput;
    private TextView noSelection;
    private Button addBridgeLine;
    private List<EditText> bridgeLineList;
    private TextView connectionStatus;
    private SelectedPluggableTransport selectedOption;
    private TextView logOutput;
    private ScrollView logScrollView;
    private SwitchMaterial logLabel;
    private BroadcastReceiver logReceiver;
    private ConstraintLayout constraintLayout;
    private Button startButton;
    private Button stopButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.constraint_layout);

        textView = findViewById(R.id.text);
        textView.setText(getString(R.string.intro_text));

        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(view -> {
            checkConnection();
        });
        fabSpin = fab.animate();

        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(view -> startArti());
        stopButton = findViewById(R.id.stopButton);

        stopButton.setOnClickListener(v -> {
            ((App)getApplication()).stopArti();
        });

        spinner = findViewById(R.id.spinner);
        inputScrollView = findViewById(R.id.inputScrollView);
        inputLayout = findViewById(R.id.inputFieldsContainer);
        bridgeLineInput = findViewById(R.id.bridgeLineInput);
        stunServerInput = findViewById(R.id.stunServerInput);
        targetInput = findViewById(R.id.targetInput);
        frontInput = findViewById(R.id.frontInput);
        noSelection = findViewById(R.id.no_option_selected);
        connectionStatus = findViewById(R.id.status);
        obfs4Port = findViewById(R.id.obfs4Port);
        addBridgeLine = findViewById(R.id.buttonAdd);
        Button removeBridgeLine = findViewById(R.id.buttonRemove);
        bridgeLineList = new ArrayList<>();
        bridgeLineList.add(bridgeLineInput);

        logOutput = findViewById(R.id.logTextView);
        logScrollView = findViewById(R.id.logScrollView);
        logLabel = findViewById(R.id.logLabel);

        logLabel.setOnCheckedChangeListener((view, isChecked) -> {
            logScrollView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String logMessage = intent.getStringExtra("logMessage");
                appendLog(logMessage);
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver,
                new IntentFilter("LOG_MESSAGE"));

        ArrayAdapter<CharSequence> adapter= ArrayAdapter.createFromResource(this, R.array.connection_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOption = SelectedPluggableTransport.values()[position];
                onSelectionChanged(selectedOption); // get options
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        addBridgeLine.setOnClickListener(v -> addNewEditText());
        removeBridgeLine.setOnClickListener(v -> removeEditText());
        inputScrollView.getViewTreeObserver().addOnGlobalLayoutListener(()
                -> inputScrollView.post(() -> inputScrollView.fullScroll(View.FOCUS_DOWN)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver);
    }
    private void appendLog(String text) {
        logOutput.append(text);
        ScrollView scrollView = findViewById(R.id.logScrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void startArti() {
//        stopButton.setEnabled(true);
        textView.setText(R.string.intro_text);
        switch (selectedOption){
            case NO_PT:
                ((App)getApplication()).connectTorDirect();
                break;
            case OBFS4:
                List<String> lyreBirdBridgeLines = collectInputs();
                if (lyreBirdBridgeLines.isEmpty()) break;
                ((App) getApplication()).connectWithLyrebird(Integer.parseInt(obfs4Port.getText().toString()), lyreBirdBridgeLines);
                break;
            case SNOWFLAKE:
                String stunServers = stunServerInput.getText().toString();
                String target = targetInput.getText().toString();
                String front = frontInput.getText().toString();
                List<String> snowflakeBridgesLines = collectInputs();
                if (snowflakeBridgesLines.isEmpty()) break;
                ((App) getApplication()).connectWithSnowflake(stunServers, target, front, snowflakeBridgesLines);
            default:
                break;
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                checkConnection();
            }
        }, 2000);
    }

    private void checkConnection() {
        fab.setEnabled(false);
        fabSpin.setDuration(1000*60).rotationBy((float) (1000 * 60) /4).setInterpolator(new LinearInterpolator()).start();
        connectionStatus.setVisibility(View.VISIBLE);
        connectionStatus.setText(R.string.performing_request);

        new CheckTorConnectionTask(this).execute();
    }

    private static class CheckTorConnectionTask extends AsyncTask<Void, Void, TorConnectionStatus> {
        private final WeakReference<MainActivity> mainActivityWeakReference;

        CheckTorConnectionTask(MainActivity mainActivity) {
            mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        protected TorConnectionStatus doInBackground(Void... voids) {
            MainActivity mainActivity = mainActivityWeakReference.get();
            if (mainActivity == null) {
                return TorConnectionStatus.ERROR;
            }
            return Helpers.checkTorProxyConnectivity("localhost", 9150);
        }

        @Override
        protected void onPostExecute(TorConnectionStatus s) {
            MainActivity mainActivity = mainActivityWeakReference.get();
            if (mainActivity == null) {
                return;
            }
            mainActivity.onCheckTorConnectionTaskCompleted(s);
        }
    }

    private void onCheckTorConnectionTaskCompleted(TorConnectionStatus s) {
        fabSpin.cancel();
        fab.setEnabled(true);
        switch (s) {
            case DIRECT:
                connectionStatus.setText(R.string.tor_no_arti);
                break;
            case TOR:
                connectionStatus.setText(R.string.tor_with_arti);
                break;
            case ERROR:
                connectionStatus.setText(R.string.connection_failed);
                break;
        }
        fab.setActivated(true);
    }

    private void addNewEditText() {
        EditText newEditText = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // convert to dp for correct margins
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (15 * (metrics.densityDpi / 160f));
        params.setMarginStart(px);
        params.setMarginEnd(px);
        newEditText.setLayoutParams(params);

        newEditText.setHint(R.string.enter_bridge_line);
        newEditText.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03DAC5"))); // teal hex
        newEditText.setSingleLine(true);

        // Add the new EditText above the add/remove buttons
        inputLayout.addView(newEditText, inputLayout.getChildCount() - 1);

        // Add the new EditText to the list
        bridgeLineList.add(newEditText);
    }

    private void removeEditText() {
        int childCount = inputLayout.getChildCount();
        if (childCount > 6) {
            inputLayout.removeViewAt(childCount - 2);
        }
    }

    private List<String> collectInputs() {
        List<String> inputs = new ArrayList<>();
        boolean matchFound;
        for (EditText editText : bridgeLineList) {
            String text = editText.getText().toString();
            if(selectedOption == SelectedPluggableTransport.OBFS4) {
                // guidance needed: how detailed should I go with this regex? not sure how
                // flexible port names can be
                Pattern pattern = Pattern.compile("^obfs4 ", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                matchFound = matcher.find();
            } else {
                matchFound = true;
            }
            if(matchFound) {
                inputs.add(editText.getText().toString());
            } else {
                textView.setText(R.string.syntax_err);
                return new ArrayList<>();
            }
        }
        return inputs;
    }

    private void onSelectionChanged(SelectedPluggableTransport s) {
        ConstraintSet constraintSet = new ConstraintSet();

        switch (s) {
            case NO_SELECTION:
                setDefaultVisibilities();
                constraintSet.clone(constraintLayout);
                break;
            case NO_PT:
                setDirectVisibilities();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(logLabel.getId(), ConstraintSet.TOP, spinner.getId(), ConstraintSet.BOTTOM);
                break;
            case OBFS4:
                setLyrebirdVisibilities();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(logLabel.getId(), ConstraintSet.TOP, inputScrollView.getId(), ConstraintSet.BOTTOM);
                break;
            case SNOWFLAKE:
                setSnowflakeVisibilities();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(logLabel.getId(), ConstraintSet.TOP, inputScrollView.getId(), ConstraintSet.BOTTOM);
                break;
        }
        constraintSet.applyTo(constraintLayout);
    }

    private void setDefaultVisibilities() {
        noSelection.setVisibility(View.VISIBLE);
        logScrollView.setVisibility(View.GONE);
        logLabel.setVisibility(View.GONE);
        inputScrollView.setVisibility(View.GONE);
      //  startButton.setEnabled(false);
        fab.setEnabled(false);
    }

    private void setDirectVisibilities() {
        stunServerInput.setVisibility(View.GONE);
        obfs4Port.setVisibility(View.GONE);
        targetInput.setVisibility(View.GONE);
        frontInput.setVisibility(View.GONE);
        bridgeLineInput.setVisibility(View.GONE);
        noSelection.setVisibility(View.GONE);
        logLabel.setVisibility(View.VISIBLE);
        inputScrollView.setVisibility(View.GONE);
        addBridgeLine.setVisibility(View.GONE);
    //    startButton.setEnabled(true);
        fab.setEnabled(true);
    }

    private void setLyrebirdVisibilities() {
        bridgeLineInput.setVisibility(View.VISIBLE);
        obfs4Port.setVisibility(View.VISIBLE);
        stunServerInput.setVisibility(View.GONE);
        targetInput.setVisibility(View.GONE);
        frontInput.setVisibility(View.GONE);
        noSelection.setVisibility(View.GONE);
        logLabel.setVisibility(View.VISIBLE);
        inputScrollView.setVisibility(View.VISIBLE);
        addBridgeLine.setVisibility(View.VISIBLE);
    //    startButton.setEnabled(true);
        fab.setEnabled(true);
    }

    private void setSnowflakeVisibilities() {
        bridgeLineInput.setVisibility(View.VISIBLE);
        stunServerInput.setVisibility(View.VISIBLE);
        targetInput.setVisibility(View.VISIBLE);
        frontInput.setVisibility(View.VISIBLE);
        noSelection.setVisibility(View.GONE);
        obfs4Port.setVisibility(View.GONE);
        logLabel.setVisibility(View.VISIBLE);
        inputScrollView.setVisibility(View.VISIBLE);
        addBridgeLine.setVisibility(View.VISIBLE);
    //    startButton.setEnabled(true);
        fab.setEnabled(true);
    }
}