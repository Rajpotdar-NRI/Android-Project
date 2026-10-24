package com.campus.survival;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.campus.survival.Models.Subject;

import java.util.List;

/**
 * Every screen extends this. It gives:
 *  - the game-style "+" tap effect on EVERY touch, on every screen
 *  - shared header, dialogs, and subject spinner helpers
 */
public abstract class BaseActivity extends Activity {

    protected DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DBHelper.get(this);
    }

    // ------------------------------------------------------------ "+" tap effect

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && Prefs.tapFx(this)) {
            int i = ev.getActionIndex();
            showPlus(ev.getX(i), ev.getY(i));
        }
        return super.dispatchTouchEvent(ev); // never consume: the tap still reaches buttons below
    }

    private void showPlus(float x, float y) {
        final ViewGroup root = (ViewGroup) getWindow().getDecorView();
        final TapPlusView plus = new TapPlusView(this);
        int size = dp(60);
        root.addView(plus, new FrameLayout.LayoutParams(size, size));
        plus.setX(x - size / 2f);
        plus.setY(y - size / 2f);
        plus.setScaleX(0.4f);
        plus.setScaleY(0.4f);
        plus.setRotation(-45f);
        plus.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .rotation(0f)
                .alpha(0f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> root.removeView(plus))
                .start();
        root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    // ------------------------------------------------------------ helpers

    protected int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    protected void setupHeader(String title, String subtitle) {
        ((TextView) findViewById(R.id.tvTitle)).setText(title);
        setSubtitle(subtitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    protected void setSubtitle(String subtitle) {
        TextView tv = findViewById(R.id.tvSubtitle);
        tv.setText(subtitle);
        tv.setVisibility(subtitle == null || subtitle.isEmpty() ? View.GONE : View.VISIBLE);
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public interface FormAction {
        /** Return true if the data was valid and saved (dialog closes), false to keep it open. */
        boolean save();
    }

    /** AlertDialog whose Save button does NOT close the dialog when validation fails. */
    protected void showForm(String title, View content, final FormAction action) {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            if (action.save()) d.dismiss();
        }));
        d.show();
    }

    protected void confirm(String message, Runnable onYes) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Yes", (d, w) -> onYes.run())
                .setNegativeButton("No", null)
                .show();
    }

    /** Fills a spinner with subject names. If there are no subjects, sends the user to add one. */
    protected boolean requireSubjects(List<Subject> subjects) {
        if (subjects.isEmpty()) {
            toast("Add a subject first");
            startActivity(new Intent(this, SubjectsActivity.class));
            return false;
        }
        return true;
    }

    protected void fillSpinner(Spinner sp, String[] values) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
    }

    protected void fillSubjectSpinner(Spinner sp, List<Subject> subjects) {
        String[] names = new String[subjects.size()];
        for (int i = 0; i < names.length; i++) names[i] = subjects.get(i).name;
        fillSpinner(sp, names);
    }

    protected static int parseInt(TextView et, int fallback) {
        try {
            return Integer.parseInt(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
