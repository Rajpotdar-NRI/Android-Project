package com.campus.survival;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.HapticFeedbackConstants;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;


public class FocusTimerActivity extends BaseActivity {

    private TextView tvTimer, tvState, tvToday;
    private TextView[] chips;
    private ProgressBar pb;
    private Button btnStart;

    private CountDownTimer timer;
    private int sessionMinutes = 25;
    private long remainingMs = 25 * 60_000L;
    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);
        setupHeader("⏱️ Focus Timer", "Phone down. One task. Go.");

        tvTimer = findViewById(R.id.tvTimer);
        tvState = findViewById(R.id.tvState);
        tvToday = findViewById(R.id.tvFocusToday);
        pb = findViewById(R.id.pbTimer);
        btnStart = findViewById(R.id.btnStart);

        chips = new TextView[]{findViewById(R.id.chip25), findViewById(R.id.chip45), findViewById(R.id.chip60)};
        final int[] mins = {25, 45, 60};
        for (int i = 0; i < chips.length; i++) {
            final int m = mins[i];
            chips[i].setOnClickListener(v -> {
                if (running) { toast("Pause or reset first"); return; }
                sessionMinutes = m;
                reset();
            });
        }

        btnStart.setOnClickListener(v -> { if (running) pause(); else start(); });
        findViewById(R.id.btnReset).setOnClickListener(v -> reset());
        reset();
    }

    private void start() {
        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStart.setText("❚❚  Pause");
        tvState.setText("FOCUSING");
        timer = new CountDownTimer(remainingMs, 250) {
            @Override public void onTick(long ms) { remainingMs = ms; render(); }
            @Override public void onFinish() { finishSession(); }
        }.start();
    }

    private void pause() {
        running = false;
        if (timer != null) timer.cancel();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStart.setText("▶  Resume");
        tvState.setText("PAUSED");
    }

    private void reset() {
        if (timer != null) timer.cancel();
        running = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        remainingMs = sessionMinutes * 60_000L;
        btnStart.setText("▶  Start");
        tvState.setText("READY");
        for (int i = 0; i < chips.length; i++) {
            boolean sel = new int[]{25, 45, 60}[i] == sessionMinutes;
            chips[i].setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            chips[i].setTextColor(getResources().getColor(sel ? R.color.white : R.color.brown, null));
        }
        render();
        showToday();
    }

    private void finishSession() {
        db.addFocus(sessionMinutes);
        remainingMs = 0;
        render();
        tvTimer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        try {
            Ringtone r = RingtoneManager.getRingtone(this,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            if (r != null) r.play();
        } catch (Exception ignored) { }
        toast("Session complete! +" + sessionMinutes + " min 🔥");
        reset();
    }

    private void render() {
        long total = sessionMinutes * 60_000L;
        long secs = (remainingMs + 999) / 1000;
        tvTimer.setText(String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60));
        pb.setProgress((int) (1000 * (total - remainingMs) / total));
    }

    private void showToday() {
        int m = db.focusMinutesToday();
        tvToday.setText(m == 0 ? "No focus sessions yet today.\nOne 25-minute session beats zero."
                : "🔥 Focused " + m + " min today");
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
