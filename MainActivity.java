package com.example.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 101;
    private TextView status, transcript, reply;
    private Button talkButton;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(4, 12, 24));
        getWindow().setNavigationBarColor(Color.rgb(4, 12, 24));
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(Locale.UK);
        });
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(4, 12, 24));

        TextView brand = new TextView(this);
        brand.setText("J.A.R.V.I.S.");
        brand.setTextColor(Color.rgb(79, 220, 255));
        brand.setTextSize(30);
        brand.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView sub = new TextView(this);
        sub.setText("PERSONAL AI • SAFE PROTOTYPE");
        sub.setTextColor(Color.rgb(145, 180, 205));
        sub.setTextSize(11);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(30)));

        View orb = new View(this) {
            @Override protected void onDraw(android.graphics.Canvas c) {
                super.onDraw(c);
                android.graphics.Paint p = new android.graphics.Paint(3);
                p.setColor(Color.rgb(26, 177, 220));
                p.setStyle(android.graphics.Paint.Style.STROKE);
                p.setStrokeWidth(dp(3));
                float cx = getWidth()/2f, cy = getHeight()/2f;
                c.drawCircle(cx, cy, Math.min(cx, cy)*0.78f, p);
                p.setStrokeWidth(dp(1));
                c.drawCircle(cx, cy, Math.min(cx, cy)*0.60f, p);
                p.setStyle(android.graphics.Paint.Style.FILL);
                p.setColor(Color.argb(30, 30, 200, 255));
                c.drawCircle(cx, cy, Math.min(cx, cy)*0.52f, p);
            }
        };
        LinearLayout.LayoutParams orbParams = new LinearLayout.LayoutParams(dp(210), dp(210));
        orbParams.setMargins(0, dp(22), 0, dp(22));
        root.addView(orb, orbParams);

        status = text("SYSTEM STANDBY", 13, Color.rgb(79,220,255), true);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(32)));

        transcript = text("Tap the button and speak.", 16, Color.WHITE, false);
        transcript.setGravity(Gravity.CENTER);
        root.addView(transcript, new LinearLayout.LayoutParams(-1, dp(58)));

        talkButton = new Button(this);
        talkButton.setText("TALK TO JARVIS");
        talkButton.setTextSize(15);
        talkButton.setAllCaps(false);
        talkButton.setOnClickListener(v -> startListening());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(56));
        bp.setMargins(0, dp(14), 0, dp(16));
        root.addView(talkButton, bp);

        ScrollView scroll = new ScrollView(this);
        reply = text("Awaiting your command, sir.", 16, Color.rgb(190,220,238), false);
        reply.setPadding(dp(14), dp(14), dp(14), dp(14));
        scroll.addView(reply);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView safety = text("BANKING DISABLED • NO PAYMENTS • NO TRANSFERS", 10, Color.rgb(255,170,130), true);
        safety.setGravity(Gravity.CENTER);
        root.addView(safety, new LinearLayout.LayoutParams(-1, dp(32)));

        setContentView(root);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition isn't available on this phone.", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { status.setText("LISTENING…"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { status.setText("PROCESSING…"); }
            @Override public void onError(int error) {
                status.setText("SYSTEM STANDBY");
                Toast.makeText(MainActivity.this, "Didn't catch that. Please try again.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String said = (matches != null && !matches.isEmpty()) ? matches.get(0) : "";
                transcript.setText("You: " + said);
                respond(said);
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS");
        recognizer.startListening(intent);
    }

    private void respond(String input) {
        String s = input.toLowerCase(Locale.ROOT);
        String answer;
        if (s.contains("transfer") || s.contains("payment") || s.contains("bank") || s.contains("money")) {
            answer = "For your security, financial actions are not available in this JARVIS prototype.";
        } else if (s.contains("hello") || s.contains("jarvis")) {
            answer = "Online, sir. JARVIS safe prototype is ready. How may I assist you?";
        } else if (s.contains("time")) {
            answer = "I can help with that in a future version. This prototype currently supports basic voice interaction.";
        } else {
            answer = "I heard: " + input + ". My AI brain is not connected yet. This is the safe first prototype.";
        }
        reply.setText(answer);
        status.setText("RESPONSE READY");
        if (tts != null) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply");
    }

    @Override protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
