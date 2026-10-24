package com.campus.survival;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.campus.survival.Models.Subject;

import java.util.List;
import java.util.Locale;

/** Practical tracker: total vs completed per subject. */
public class PracticalActivity extends SubjectStatActivity {

    @Override protected String title() { return "🧪 Practicals"; }

    @Override
    protected String subtitle(List<Subject> subjects) {
        int t = 0, d = 0;
        for (Subject s : subjects) { t += s.pracTotal; d += s.pracDone; }
        return t == 0 ? "Set total practicals for each subject in Subjects"
                : String.format(Locale.US, "%d of %d done · %d remaining", d, t, t - d);
    }

    @Override
    protected void bind(Subject s, TextView pct, ProgressBar pb, TextView info, TextView hint, Button a, Button b) {
        double p = s.practicalPct();
        pct.setText(s.pracTotal == 0 ? "—" : String.format(Locale.US, "%.0f%%", p));
        pb.setProgress((int) Math.round(p));
        info.setText(s.pracDone + " of " + s.pracTotal + " completed · " + (s.pracTotal - s.pracDone) + " remaining");

        boolean finished = s.pracTotal > 0 && s.pracDone >= s.pracTotal;
        if (s.pracTotal == 0) {
            hint.setText("Set total practicals in Subjects");
            hint.setTextColor(ORANGE);
        } else if (finished) {
            hint.setText("🏆 All practicals complete");
            hint.setTextColor(GREEN);
        } else {
            hint.setText("Next up: Practical #" + (s.pracDone + 1));
            hint.setTextColor(p < 40 ? RED : ORANGE);
        }

        a.setText("↺  Undo");
        a.setEnabled(s.pracDone > 0);
        a.setAlpha(s.pracDone > 0 ? 1f : 0.5f);
        b.setText(finished || s.pracTotal == 0 ? "✓  Done" : "✓  Complete #" + (s.pracDone + 1));
        b.setEnabled(!finished && s.pracTotal > 0);
        b.setAlpha(b.isEnabled() ? 1f : 0.5f);

        a.setOnClickListener(v -> { db.changePracticals(s.id, -1); reload(); });
        b.setOnClickListener(v -> { db.changePracticals(s.id, +1); reload(); });
    }
}
