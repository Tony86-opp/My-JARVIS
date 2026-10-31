package com.example.jarvis;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    TextView status, conversation;
    EditText message;
    TextToSpeech tts;
    SpeechRecognizer recognizer;
    String apiKey = "";
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(4,12,24));
        getWindow().setNavigationBarColor(Color.rgb(4,12,24));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(22,25,22,20);
        root.setBackgroundColor(Color.rgb(4,12,24));

        TextView title = new TextView(this);
        title.setText("J.A.R.V.I.S.");
        title.setTextSize(32);
        title.setTextColor(Color.CYAN);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("SYSTEM READY");
        status.setTextColor(Color.CYAN);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        conversation = new TextView(this);
        conversation.setTextColor(Color.WHITE);
        conversation.setTextSize(17);
        conversation.setPadding(12,20,12,20);
        conversation.setText("Hello! I am JARVIS.");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(conversation);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        message = new EditText(this);
        message.setHint("Type your message");
        message.setTextColor(Color.WHITE);
        message.setHintTextColor(Color.GRAY);
        root.addView(message);

        Button send = new Button(this);
        send.setText("SEND");
        send.setOnClickListener(v -> sendMessage());
        root.addView(send);

        Button talk = new Button(this);
        talk.setText("TALK TO JARVIS");
        talk.setOnClickListener(v -> startListening());
        root.addView(talk);

        Button key = new Button(this);
        key.setText("CONNECT GROQ AI");
        key.setOnClickListener(v -> showKeyDialog());
        root.addView(key);

        TextView safety = new TextView(this);
        safety.setText("NO BANKING • NO PAYMENTS • NO TRANSFERS");
        safety.setTextColor(Color.rgb(255,160,130));
        safety.setGravity(Gravity.CENTER);
        root.addView(safety);

        setContentView(root);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.getDefault());
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},101);
        }    void showKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("Paste your Groq API key");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle("Connect Groq AI")
            .setMessage("Your key stays in memory until the app closes.")
            .setView(input)
            .setPositiveButton("SAVE", (d,w) -> {
                apiKey = input.getText().toString().trim();
                status.setText(apiKey.isEmpty()
                    ? "API KEY NOT SET" : "AI KEY ADDED");
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    void sendMessage() {
        String question = message.getText().toString().trim();
        if (question.isEmpty()) return;

        conversation.append("\n\nYOU: " + question);
        message.setText("");

        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("bank") || q.contains("payment")
                || q.contains("transfer")) {
            reply("I cannot perform banking, payments or transfers.");
            return;
        }

        if (q.contains("time")) {
            reply("The time is " +
                new java.text.SimpleDateFormat("hh:mm a",
                Locale.getDefault()).format(new Date()));
            return;
        }

        if (q.contains("date")) {
            reply("Today's date is " +
                new java.text.SimpleDateFormat("dd MMMM yyyy",
                Locale.getDefault()).format(new Date()));
            return;
        }

        if (apiKey.isEmpty()) {
            reply("Please connect Groq AI first. Offline mode is limited.");
            return;
        }

        status.setText("THINKING...");
        executor.execute(() -> askGroq(question));
    }

    void askGroq(String question) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(
                "https://api.groq.com/openai/v1/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                "application/json");
            conn.setRequestProperty("Authorization",
                "Bearer " + apiKey);

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();

            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                "You are JARVIS, a helpful voice assistant. " +
                "Reply in the same language as the user. " +
                "Use simple, clear language. " +
                "Never perform banking, payments or money transfers.");
            messages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", question);
            messages.put(user);

            body.put("messages", messages);

            OutputStream out = conn.getOutputStream();
            out.write(body.toString().getBytes("UTF-8"));
            out.close();

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                result.append(line);
            reader.close();

            if (code < 200 || code >= 300)
                throw new Exception("Groq error " + code);

            JSONObject response = new JSONObject(result.toString());
            String answer = response.getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message")
                .getString("content");

            runOnUiThread(() -> reply(answer));

        } catch (Exception e) {
            runOnUiThread(() ->
                reply("I could not connect to AI. Check internet and API key."));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    }
    void reply(String answer) {
        conversation.append("\n\nJARVIS: " + answer);
        status.setText("SYSTEM READY");
        if (tts != null)
            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis");
    }

    void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            reply("Speech recognition is not available on this phone.");
            return;
        }

        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("LISTENING...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("PROCESSING...");
            }

            @Override
            public void onError(int error) {
                status.setText("SYSTEM READY");
                reply("I could not hear you. Please try again.");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> words = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (words != null && !words.isEmpty()) {
                    message.setText(words.get(0));
                    sendMessage();
                }
                status.setText("SYSTEM READY");
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault().toLanguageTag());

        recognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) tts.shutdown();
        executor.shutdownNow();
        super.onDestroy();
    }
}
