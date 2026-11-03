 
package com.example.jarvis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {

    private static final String KEY_ALIAS = "JARVIS_API_KEY";
    private static final String PREFS = "jarvis_secure";
    private static final String KEY_DATA = "encrypted_key";
    private static final String KEY_IV = "key_iv";

    private final int BG = Color.rgb(8, 10, 18);
    private final int PANEL = Color.rgb(19, 23, 35);
    private final int RED = Color.rgb(255, 65, 55);
    private final int GOLD = Color.rgb(255, 190, 75);
    private final int WHITE = Color.rgb(240, 243, 250);
    private final int MUTED = Color.rgb(150, 160, 180);

    private LinearLayout root;
    private LinearLayout chat;
    private EditText input;
    private TextView status;
    private TextView reactor;
    private ScrollView scroll;

    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private ExecutorService executor;

    private String apiKey = "";
    private boolean ttsReady = false;

    // ENCRYPT AND SAVE THE API KEY
    private void saveApiKey(String value) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);

        if (!store.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore");

            generator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT
                            | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());

            generator.generateKey();
        }

        SecretKey secret =
                (SecretKey) store.getKey(KEY_ALIAS, null);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);

        byte[] encrypted =
                cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_DATA,
                        Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV,
                        Base64.encodeToString(
                                cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    // LOAD AND DECRYPT THE SAVED API KEY
    private String loadApiKey() {
        try {
            SharedPreferences prefs =
                    getSharedPreferences(PREFS, MODE_PRIVATE);

            String data = prefs.getString(KEY_DATA, null);
            String iv = prefs.getString(KEY_IV, null);

            if (data == null || iv == null) {
                return "";
            }

            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);

            SecretKey secret =
                    (SecretKey) store.getKey(KEY_ALIAS, null);

            if (secret == null) {
                return "";
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(Cipher.DECRYPT_MODE, secret,
                    new GCMParameterSpec(
                            128, Base64.decode(iv, Base64.NO_WRAP)));

            byte[] decrypted =
                    cipher.doFinal(
                            Base64.decode(data, Base64.NO_WRAP));

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor = Executors.newSingleThreadExecutor();

tts = new TextToSpeech(this, result -> {
    if (result == TextToSpeech.SUCCESS) {
        int languageResult = tts.setLanguage(Locale.UK);

        ttsReady =
                languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult
                        != TextToSpeech.LANG_NOT_SUPPORTED;

        if (ttsReady) {
            tts.setSpeechRate(1.0f);
            tts.setPitch(0.95f);
        }
    }
});
                    
                
            
              

        buildInterface();

        addMessage("JARVIS",
                "Systems online. Good evening, Sir. How may I assist you?");

        apiKey = loadApiKey();

        if (apiKey.isEmpty()) {
            showKeyDialog();
        } else {
            status.setText("● READY • SAVED AI KEY");
            status.setTextColor(GOLD);
        }
    }

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private Button makeButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(shape(PANEL, color));
        return button;
    }

    // MAIN JARVIS INTERFACE
    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(12));
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        
         header.setOrientation(LinearLayout.VERTICAL);
header.setGravity(Gravity.CENTER);
header.setPadding(0, dp(8), 0, dp(8));

ImageView logo = new ImageView(this);
logo.setImageResource(R.drawable.jarvis_logo);
logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

LinearLayout.LayoutParams logoParams =
        new LinearLayout.LayoutParams(dp(110), dp(110));

header.addView(logo, logoParams);

TextView eyebrow = text(
        "STARK INDUSTRIES  //  PERSONAL AI", 10, GOLD);
header.addView(eyebrow);
        TextView title = text("J.A.R.V.I.S.", 32, WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.12f);
        header.addView(title);

        TextView subtitle = text(
                "JUST A RATHER VERY INTELLIGENT SYSTEM", 9, MUTED);
        header.addView(subtitle);

        root.addView(header);

        // REACTOR PANEL
        LinearLayout corePanel = new LinearLayout(this);
        corePanel.setOrientation(LinearLayout.VERTICAL);
        corePanel.setGravity(Gravity.CENTER);
        corePanel.setPadding(dp(12), dp(14), dp(12), dp(14));
        corePanel.setBackground(shape(PANEL, RED));

        reactor = text("◉", 64, RED);
        reactor.setVisibility(View.GONE);

        JarvisHudView hud = new JarvisHudView(this);
        corePanel.addView(hud,
                new LinearLayout.LayoutParams(-1, dp(210)));

        TextView coreLabel = text(
                "ARC REACTOR  //  CORE SYSTEM", 10, GOLD);
        corePanel.addView(coreLabel);

        status = text(
                "●  STANDBY  •  AI NOT CONNECTED", 11, MUTED);
        status.setPadding(0, dp(8), 0, 0);
        corePanel.addView(status);

        LinearLayout.LayoutParams coreParams =
                new LinearLayout.LayoutParams(-1, dp(300));
        coreParams.bottomMargin = dp(12);
        root.addView(corePanel, coreParams);

        // COMMUNICATION LOG
        TextView logTitle = text(
                "COMMUNICATION LOG", 11, GOLD);
        logTitle.setGravity(
                Gravity.START | Gravity.CENTER_VERTICAL);
        logTitle.setPadding(dp(4), dp(4), 0, dp(8));
        root.addView(logTitle);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        chat.setPadding(dp(2), dp(4), dp(2), dp(8));

        scroll.addView(chat);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(scroll, scrollParams);

        // MESSAGE INPUT
        input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        input.setHint("Ask JARVIS anything...");
        input.setTextSize(15);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(
                shape(PANEL, Color.rgb(65, 75, 95)));

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(-1, -2);
        inputParams.topMargin = dp(10);
        root.addView(input, inputParams);

        // CONTROL BUTTONS
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button send = makeButton("SEND", RED);
        Button talk = makeButton("🎙 TALK", GOLD);
        Button connect = makeButton("AI KEY", GOLD);

        row.addView(send,
                new LinearLayout.LayoutParams(0, dp(52), 1f));

        LinearLayout.LayoutParams talkParams =
                new LinearLayout.LayoutParams(0, dp(52), 1f);
        talkParams.leftMargin = dp(6);
        row.addView(talk, talkParams);

        LinearLayout.LayoutParams connectParams =
                new LinearLayout.LayoutParams(0, dp(52), 1f);
        connectParams.leftMargin = dp(6);
        row.addView(connect, connectParams);

        root.addView(row);

        TextView safety = text(
                "SECURE MODE  •  BANKING AND PAYMENTS DISABLED",
                9, MUTED);
        safety.setPadding(0, dp(10), 0, dp(2));
        root.addView(safety);

        setContentView(root);

        send.setOnClickListener(v -> {
            String question = input.getText().toString().trim();

            if (!question.isEmpty()) {
                input.setText("");
                hideKeyboard();
                sendMessage(question);
            }
        });

        talk.setOnClickListener(v -> startListening());
        connect.setOnClickListener(v -> showKeyDialog());
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);

            if (manager != null && input != null) {
                manager.hideSoftInputFromWindow(
                        input.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private void addMessage(String who, String message) {
        TextView bubble = new TextView(this);
        bubble.setText(who + "\n" + message);
        bubble.setTextSize(14);
        bubble.setTextColor(WHITE);
        bubble.setGravity(Gravity.START);
        bubble.setPadding(dp(13), dp(10), dp(13), dp(10));

        bubble.setBackground(shape(
                who.equals("YOU")
                        ? Color.rgb(35, 24, 29)
                        : Color.rgb(18, 27, 39),
                who.equals("YOU") ? RED : GOLD));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(9);

        chat.addView(bubble, params);

        scroll.post(() ->
                scroll.fullScroll(View.FOCUS_DOWN));
    }

    // GROQ API KEY DIALOG
    private void showKeyDialog() {
        EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint("Paste your Groq API key");
        keyInput.setInputType(129);

        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), dp(4));
        box.addView(keyInput,
                new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("Connect JARVIS AI")
                .setMessage(
                        "Enter your Groq API key. " +
                        "It will be encrypted and saved on this device.")
                .setView(box)
                .setCancelable(true)
                .setPositiveButton("CONNECT", (dialog, which) -> {
                    String entered =
                            keyInput.getText().toString().trim();

                    if (entered.isEmpty()) {
                        Toast.makeText(this,
                                "No API key entered.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        saveApiKey(entered);
                        apiKey = entered;

                        status.setText(
                                "● READY • AI KEY SAVED");
                        status.setTextColor(GOLD);

                        addMessage("JARVIS",
                                "Your AI key has been securely saved. " +
                                "I will test the connection when you send a message.");

                    } catch (Exception e) {
                        apiKey = "";

                        Toast.makeText(this,
                                "Could not securely save API key.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("LATER", (dialog, which) -> {
                    status.setText(
                            "● OFFLINE MODE • AI NOT CONNECTED");
                })
                .show();
    }

    // PROCESS USER MESSAGE
    private void sendMessage(String question) {
        addMessage("YOU", question);

        String lower =
                question.toLowerCase(Locale.ROOT);

        // PERMANENT BANKING AND PAYMENT RESTRICTION
        if (lower.matches(
                ".*\\b(bank|banking|payment|payments|money transfer|transfer money|upi|pay someone)\\b.*")) {

            reply("That capability is disabled, as requested. " +
                    "I cannot help perform banking, payments, " +
                    "or money transfers.");
            return;
        }

        if (lower.contains("what time") || lower.equals("time")) {
            reply("The current time is " +
                    java.text.DateFormat.getTimeInstance(
                            java.text.DateFormat.SHORT)
                            .format(new java.util.Date()));
            return;
        }

        if (lower.contains("what date")
                || lower.contains("today's date")) {
            reply("Today's date is " +
                    java.text.DateFormat.getDateInstance(
                            java.text.DateFormat.LONG)
                            .format(new java.util.Date()));
            return;
        }

        if (apiKey.isEmpty()) {
            reply("My AI connection is not configured yet. " +
                    "Tap AI KEY and enter your Groq API key.");
            return;
        }

        status.setText("● THINKING • CONNECTING TO GROQ");
        status.setTextColor(GOLD);
        reactor.setTextColor(GOLD);

        executor.execute(() -> askGroq(question));
    }

    // CONNECT TO GROQ AI
    private void askGroq(String question) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(
                    "https://api.groq.com/openai/v1/chat/completions");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            conn.setRequestProperty(
                    "Content-Type", "application/json");
            conn.setRequestProperty(
                    "Authorization", "Bearer " + apiKey);

            JSONObject body = new JSONObject();
            body.put("model", "openai/gpt-oss-120b");
            body.put("temperature", 0.7);
            body.put("max_tokens", 500);

            JSONArray messages = new JSONArray();

            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    "You are JARVIS, a helpful, calm, intelligent " +
                    "personal voice assistant. Address the user as Sir " +
                    "when natural. Reply in the same language as the user. " +
                    "Use clear, natural, concise language. " +
                    "Never perform or facilitate banking, payments, " +
                    "UPI transactions, or money transfers.");
            messages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", question);
            messages.put(user);

            body.put("messages", messages);

            byte[] bytes =
                    body.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }

            int code = conn.getResponseCode();

            InputStream stream =
                    (code >= 200 && code < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            StringBuilder result = new StringBuilder();

            if (stream != null) {
                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             stream,
                                             StandardCharsets.UTF_8))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                }
            }

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "Groq error " + code + ": " + result);
            }

            JSONObject response =
                    new JSONObject(result.toString());

            String answer =
                
           response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

            runOnUiThread(() -> {
                status.setText("● ONLINE • JARVIS READY");
                status.setTextColor(GOLD);
                reactor.setTextColor(RED);
                reply(answer);
            });

        } catch (Exception e) {
            String error = e.getMessage();

            if (error == null || error.isEmpty()) {
                error = e.getClass().getSimpleName();
            }

            final String message = error;

            runOnUiThread(() -> {
                status.setText("● CONNECTION ERROR");
                status.setTextColor(RED);
                reactor.setTextColor(RED);
                reply("AI connection failed: " + message);
            });

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // DISPLAY AND SPEAK RESPONSE
    private void reply(String answer) {
        addMessage("JARVIS", answer);

        if (tts != null && ttsReady) {
            tts.speak(
                    answer,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_REPLY");
        }
    }

    // VOICE INPUT
    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this,
                    "Speech recognition is not available on this device.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (recognizer != null) {
                recognizer.destroy();
            }

            recognizer =
                    SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(
                    new RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle params) {
                    status.setText("● LISTENING • SPEAK NOW");
                    status.setTextColor(GOLD);
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    status.setText("● PROCESSING VOICE");
                }

                @Override
                public void onError(int error) {
                    status.setText("● STANDBY");

                    Toast.makeText(MainActivity.this,
                            "Couldn't recognize speech. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches =
                            results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION);

                    status.setText("● STANDBY");

                    if (matches != null && !matches.isEmpty()) {
                        String spoken = matches.get(0);
                        input.setText(spoken);
                        input.setText("");
                        sendMessage(spoken);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(
                        int eventType, Bundle params) {
                }
            });

            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            intent.putExtra(
                   RecognizerIntent.EXTRA_LANGUAGE,
                   Locale.getDefault().toString());

            intent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false);

            recognizer.startListening(intent);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Voice input failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // CLEAN UP WHEN APP CLOSES
    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        super.onDestroy();
    }
}
   
