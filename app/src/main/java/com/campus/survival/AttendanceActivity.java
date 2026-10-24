package com.campus.survival;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.campus.survival.Models.Subject;

import java.util.List;
import java.util.Locale;

/** Attendance tracker with the 75% rule + "how many can I miss" calculator. */
public class AttendanceActivity extends SubjectStatActivity {

    @Override protected String title() { return "📊 Attendance"; }

    @Override
    protected String subtitle(List<Subject> subjects) {
        int t = 0, a = 0;
        for (Subject s : subjects) { t += s.lecTotal; a += s.lecAttended; }
        return t == 0 ? "Mark each lecture as Present or Absent"
                : String.format(Locale.US, "Overall %.1f%% · %d of %d lectures", a * 100.0 / t, a, t);
    }

    @Override
    protected void bind(Subject s, TextView pct, ProgressBar pb, TextView info, TextView hint, Button a, Button b) {
        double p = s.attendancePct();
        pct.setText(s.lecTotal == 0 ? "—" : String.format(Locale.US, "%.1f%%", p));
        pb.setProgress((int) Math.round(p));
        info.setText("Attended " + s.lecAttended + " of " + s.lecTotal + " lectures");

        if (s.lecTotal == 0) {
            hint.setText("No lectures marked yet");
            hint.setTextColor(ORANGE);
        } else if (p < 75) {
            int n = s.lecturesNeededFor75();
            hint.setText("🔴 Below 75% — attend the next " + n + (n == 1 ? " lecture" : " lectures") + " to recover");
            hint.setTextColor(RED);
        } else {
            int k = s.lecturesYouCanMiss();
            hint.setText(k > 0
                    ? "🟢 Safe — you can miss " + k + (k == 1 ? " lecture" : " lectures") + " and stay ≥ 75%"
                    : "🟡 On the edge — don't miss the next one");
            hint.setTextColor(k > 0 ? GREEN : ORANGE);
        }

        a.setText("✗  Absent");
        b.setText("✓  Present");
        a.setOnClickListener(v -> { db.markLecture(s.id, false); reload(); undoToast(s, false); });
        b.setOnClickListener(v -> { db.markLecture(s.id, true); reload(); undoToast(s, true); });
    }

    private void undoToast(Subject s, boolean present) {
        toast((present ? "Present" : "Absent") + " marked for " + s.name + ". Mistake? Fix the numbers in Subjects.");
    }
}
