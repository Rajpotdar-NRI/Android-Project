package com.campus.survival;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.campus.survival.Models.Subject;

import java.util.List;
import java.util.Locale;


public class SyllabusActivity extends SubjectStatActivity {

    @Override protected String title() { return "📚 Syllabus"; }

    @Override
    protected String subtitle(List<Subject> subjects) {
        int t = 0, d = 0;
        for (Subject s : subjects) { t += s.unitTotal; d += s.unitDone; }
        return t == 0 ? "" : String.format(Locale.US, "Overall syllabus %.0f%% complete", d * 100.0 / t);
    }

    @Override
    protected void bind(Subject s, TextView pct, ProgressBar pb, TextView info, TextView hint, Button a, Button b) {
        double p = s.syllabusPct();
        pct.setText(String.format(Locale.US, "%.0f%%", p));
        pb.setProgress((int) Math.round(p));
        info.setText(s.unitDone + " of " + s.unitTotal + " units finished");

        StringBuilder units = new StringBuilder();
        for (int u = 1; u <= s.unitTotal; u++) {
            String mark = u <= s.unitDone ? "✅" : (u == s.unitDone + 1 ? "🔄" : "❌");
            units.append("U").append(u).append(' ').append(mark).append("   ");
        }
        hint.setText(units.toString().trim());
        hint.setTextColor(getResources().getColor(R.color.brown, null));

        boolean finished = s.unitDone >= s.unitTotal;
        a.setText("↺  Undo");
        a.setEnabled(s.unitDone > 0);
        a.setAlpha(s.unitDone > 0 ? 1f : 0.5f);
        b.setText(finished ? "🏆  Complete" : "✓  Finish Unit " + (s.unitDone + 1));
        b.setEnabled(!finished);
        b.setAlpha(finished ? 0.5f : 1f);

        a.setOnClickListener(v -> { db.changeUnits(s.id, -1); reload(); });
        b.setOnClickListener(v -> { db.changeUnits(s.id, +1); reload(); });
    }
}
