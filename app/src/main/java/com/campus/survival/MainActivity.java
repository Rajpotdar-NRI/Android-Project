package com.campus.survival;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.campus.survival.Models.Assignment;
import com.campus.survival.Models.ClassSlot;
import com.campus.survival.Models.Exam;
import com.campus.survival.Models.Subject;

import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView tvGreeting, tvDate, tvScore, tvRank, tvBreakdown, tvToday, tvAlerts;
    private ProgressBar pbScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvDate = findViewById(R.id.tvDate);
        tvScore = findViewById(R.id.tvScore);
        tvRank = findViewById(R.id.tvRank);
        tvBreakdown = findViewById(R.id.tvBreakdown);
        tvToday = findViewById(R.id.tvToday);
        tvAlerts = findViewById(R.id.tvAlerts);
        pbScore = findViewById(R.id.pbScore);

        open(R.id.btnPlan, PlannerActivity.class);
        open(R.id.tileTimetable, TimetableActivity.class);
        open(R.id.tileAttendance, AttendanceActivity.class);
        open(R.id.tilePracticals, PracticalActivity.class);
        open(R.id.tileAssignments, AssignmentsActivity.class);
        open(R.id.tileExams, ExamsActivity.class);
        open(R.id.tileSyllabus, SyllabusActivity.class);
        open(R.id.tileFocus, FocusTimerActivity.class);
        open(R.id.tileSubjects, SubjectsActivity.class);

        findViewById(R.id.btnMenu).setOnClickListener(v -> showMenu());
    }

    private void open(int viewId, Class<?> screen) {
        findViewById(viewId).setOnClickListener(v -> startActivity(new Intent(this, screen)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh(); // data may have changed in another screen
    }

    private void refresh() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String first = db.userName(Prefs.userId(this)).trim().split("\\s+")[0];
        tvGreeting.setText((h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening") +
                (first.isEmpty() ? "" : ", " + first) + " 👋");
        tvDate.setText(DateUtil.prettyToday());

        // ---- Score
        SurvivalEngine.Score s = SurvivalEngine.score(db);
        tvScore.setText(String.valueOf(s.total));
        tvRank.setText(s.rank);
        pbScore.setProgress(s.total);
        tvBreakdown.setText(s.hasData
                ? "Attendance " + pct(s.attendance) + "  ·  Practicals " + pct(s.practicals) +
                  "\nAssignments " + pct(s.assignments) + "  ·  Syllabus " + pct(s.syllabus)
                : "Add subjects to start tracking — or ⚙ → Load demo data");

        // ---- Today's classes
        List<ClassSlot> classes = db.getClasses(DateUtil.todayDow());
        if (classes.isEmpty()) {
            tvToday.setText("No classes today 🎉\nAdd your timetable from the Timetable module.");
        } else {
            StringBuilder sb = new StringBuilder();
            int now = DateUtil.nowMinutes();
            for (ClassSlot k : classes) {
                if (sb.length() > 0) sb.append('\n');
                String mark = k.end <= now ? "✅" : (k.start <= now ? "🟠" : "🕘");
                sb.append(mark).append("  ").append(DateUtil.time(k.start)).append("  —  ").append(k.subjectName);
                if (k.room != null && !k.room.isEmpty()) sb.append("  (").append(k.room).append(')');
            }
            tvToday.setText(sb);
        }

        // ---- Alerts
        StringBuilder al = new StringBuilder();
        List<Subject> subjects = db.getSubjects();
        for (Subject sub : subjects) {
            if (sub.lecTotal > 0 && sub.attendancePct() < 75) {
                line(al, String.format(Locale.US, "🔴 %s attendance %.1f%% — attend next %d",
                        sub.name, sub.attendancePct(), sub.lecturesNeededFor75()));
            }
        }
        for (Assignment a : db.getAssignments()) {
            if (a.done) continue;
            int d = DateUtil.daysUntil(a.due);
            if (d < 0) line(al, "⛔ " + a.title + " is OVERDUE");
            else if (d <= 2) line(al, "📝 " + a.title + " due " + DateUtil.daysLabel(d).toLowerCase(Locale.US));
        }
        for (Exam e : db.getExams()) {
            int d = DateUtil.daysUntil(e.date);
            if (d >= 0) {
                line(al, "🔥 " + e.name + " in " + d + (d == 1 ? " day" : " days"));
                break; // only the next exam
            }
        }
        for (Subject sub : subjects) {
            if (sub.pracTotal > 0 && sub.practicalPct() < 40) {
                line(al, String.format(Locale.US, "🧪 %s practicals only %d/%d", sub.name, sub.pracDone, sub.pracTotal));
            }
        }
        int focus = db.focusMinutesToday();
        if (focus > 0) line(al, "⏱️ Focused " + focus + " min today — nice");
        tvAlerts.setText(al.length() == 0 ? "All clear. You're surviving 😎" : al);
    }

    private static void line(StringBuilder sb, String text) {
        if (sb.length() > 0) sb.append('\n');
        sb.append(text);
    }

    private static String pct(int v) { return v < 0 ? "—" : v + "%"; }

    private void showMenu() {
        PopupMenu pm = new PopupMenu(this, findViewById(R.id.btnMenu));
        Menu m = pm.getMenu();
        m.add(0, 1, 0, Prefs.tapFx(this) ? "Tap “+” effect: ON" : "Tap “+” effect: OFF");
        m.add(0, 2, 1, "Load demo data");
        m.add(0, 3, 2, "Reset all data");
        m.add(0, 4, 3, "Logout");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    Prefs.setTapFx(this, !Prefs.tapFx(this));
                    toast(Prefs.tapFx(this) ? "Tap effect on" : "Tap effect off");
                    return true;
                case 2:
                    confirm("Replace your data with demo data?", () -> { db.loadDemoData(); refresh(); });
                    return true;
                case 3:
                    confirm("Delete ALL subjects, timetable, assignments and exams?", () -> { db.clearAll(); refresh(); });
                    return true;
                case 4:
                    confirm("Log out?", () -> {
                        Prefs.logout(this);
                        Intent i = new Intent(this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    });
                    return true;
            }
            return false;
        });
        pm.show();
    }
}
