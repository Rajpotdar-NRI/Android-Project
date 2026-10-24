package com.campus.survival;

import com.campus.survival.Models.Assignment;
import com.campus.survival.Models.ClassSlot;
import com.campus.survival.Models.Exam;
import com.campus.survival.Models.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The "brain" of the app:
 *  1) Survival Score-weighted average of attendance, practicals, assignments, syllabus.
 *  2) Plan My Day- a rule-based scheduler (no AI needed) that fills free time after classes.
 */
public final class SurvivalEngine {

    private SurvivalEngine() { }

    // =============================================================== SCORE

    public static class Score {
        public int total;
        public int attendance = -1, practicals = -1, assignments = -1, syllabus = -1; // -1 = no data
        public String rank;
        public boolean hasData;
    }

    // Weights (must add to 100). Attendance weighs most because < 75% can block exam eligibility.
    private static final int W_ATT = 35, W_PRAC = 25, W_ASG = 20, W_SYL = 20;

    public static Score score(DBHelper db) {
        Score s = new Score();
        int lecT = 0, lecA = 0, pracT = 0, pracD = 0, unitT = 0, unitD = 0;
        for (Subject sub : db.getSubjects()) {
            lecT += sub.lecTotal; lecA += sub.lecAttended;
            pracT += sub.pracTotal; pracD += sub.pracDone;
            unitT += sub.unitTotal; unitD += sub.unitDone;
        }
        int asgT = 0, asgD = 0;
        for (Assignment a : db.getAssignments()) {
            asgT++;
            if (a.done) asgD++;
        }

        // Attendance is scored against the 75% rule: 75% -> 75 points, 100% -> 100 points.
        if (lecT > 0) s.attendance = Math.round(lecA * 100f / lecT);
        if (pracT > 0) s.practicals = Math.round(pracD * 100f / pracT);
        if (asgT > 0) s.assignments = Math.round(asgD * 100f / asgT);
        if (unitT > 0) s.syllabus = Math.round(unitD * 100f / unitT);

        int weighted = 0, weights = 0;
        if (s.attendance >= 0) { weighted += s.attendance * W_ATT; weights += W_ATT; }
        if (s.practicals >= 0) { weighted += s.practicals * W_PRAC; weights += W_PRAC; }
        if (s.assignments >= 0) { weighted += s.assignments * W_ASG; weights += W_ASG; }
        if (s.syllabus >= 0) { weighted += s.syllabus * W_SYL; weights += W_SYL; }

        s.hasData = weights > 0;
        s.total = s.hasData ? Math.round(weighted / (float) weights) : 0;
        s.rank = rankFor(s.total, s.hasData);
        return s;
    }

    public static String rankFor(int score, boolean hasData) {
        if (!hasData) return "🌱 New Student";
        if (score >= 90) return "🏆 College Legend";
        if (score >= 75) return "😎 Surviving";
        if (score >= 50) return "😰 Danger Zone";
        return "💀 Academic Crisis";
    }

    // =============================================================== PLAN MY DAY

    public static class PlanItem {
        public int start = -1, end = -1;  // -1 = not time-boxed ("tomorrow" list)
        public String emoji, title, note;

        PlanItem(String emoji, String title, String note) {
            this.emoji = emoji; this.title = title; this.note = note;
        }
    }

    private static final int DAY_END = 22 * 60 + 30;  // stop planning at 10:30 PM
    private static final int BREAK = 15;

    public static List<PlanItem> planToday(DBHelper db) {
        List<PlanItem> plan = new ArrayList<>();
        List<Subject> subjects = db.getSubjects();
        List<ClassSlot> classes = db.getClasses(DateUtil.todayDow());
        int now = DateUtil.nowMinutes();

        // ---- 1. Fixed blocks: today's classes
        int lastClassEnd = -1;
        for (ClassSlot k : classes) {
            Subject s = find(subjects, k.subjectId);
            String note = (k.room == null || k.room.isEmpty() ? "" : k.room);
            if (s != null && s.lecTotal > 0 && s.attendancePct() < 75) {
                note += (note.isEmpty() ? "" : " · ") +
                        String.format(Locale.US, "Don't skip — attendance %.0f%%", s.attendancePct());
            }
            if (k.end <= now) note += (note.isEmpty() ? "" : " · ") + "done";
            PlanItem it = new PlanItem("🏫", "Attend " + k.subjectName, note);
            it.start = k.start; it.end = k.end;
            plan.add(it);
            lastClassEnd = Math.max(lastClassEnd, k.end);
        }

        // ---- 2. Build a priority queue of self-study tasks
        List<PlanItem> tasks = new ArrayList<>();
        List<Integer> lengths = new ArrayList<>();
        List<Long> usedSubjects = new ArrayList<>();

        List<Assignment> pending = new ArrayList<>();
        for (Assignment a : db.getAssignments()) if (!a.done) pending.add(a);

        // 2a. Urgent assignments: overdue or due within 2 days
        for (Assignment a : pending) {
            int d = DateUtil.daysUntil(a.due);
            if (d <= 2) {
                tasks.add(new PlanItem("📝", "Finish " + a.title,
                        a.subjectName + " · " + (d < 0 ? "OVERDUE" : "due " + DateUtil.daysLabel(d).toLowerCase(Locale.US))));
                lengths.add(60);
            }
        }

        // 2b. Nearest exam within 21 days -> revision of that subject
        for (Exam e : db.getExams()) {
            int d = DateUtil.daysUntil(e.date);
            if (d >= 0 && d <= 21) {
                Subject s = find(subjects, e.subjectId);
                String syl = s == null ? "" : String.format(Locale.US, " · syllabus %.0f%%", s.syllabusPct());
                tasks.add(new PlanItem("⏳", "Revise " + e.subjectName, e.name + " in " + d + " days" + syl));
                lengths.add(d <= 7 ? 60 : 45);
                usedSubjects.add(e.subjectId);
                break;
            }
        }

        // 2c. Practical that is furthest behind
        Subject behind = null;
        for (Subject s : subjects) {
            if (s.pracTotal > 0 && s.pracDone < s.pracTotal &&
                    (behind == null || s.practicalPct() < behind.practicalPct())) behind = s;
        }
        if (behind != null) {
            tasks.add(new PlanItem("🧪", "Write " + behind.name + " Practical #" + (behind.pracDone + 1),
                    String.format(Locale.US, "%d of %d done — lowest of all subjects", behind.pracDone, behind.pracTotal)));
            lengths.add(45);
        }

        // 2d. Other high-priority assignments due later
        for (Assignment a : pending) {
            if (DateUtil.daysUntil(a.due) > 2 && a.priority == 2) {
                tasks.add(new PlanItem("📝", "Start " + a.title,
                        a.subjectName + " · high priority · due " + DateUtil.pretty(a.due)));
                lengths.add(45);
            }
        }

        // 2e. Syllabus: weakest subject not already being revised
        Subject weak = null;
        for (Subject s : subjects) {
            if (usedSubjects.contains(s.id) || s.unitTotal == 0 || s.unitDone >= s.unitTotal) continue;
            if (weak == null || s.syllabusPct() < weak.syllabusPct()) weak = s;
        }
        if (weak != null) {
            tasks.add(new PlanItem("📚", "Study " + weak.name + " Unit " + (weak.unitDone + 1),
                    String.format(Locale.US, "Syllabus only %.0f%% done", weak.syllabusPct())));
            lengths.add(45);
        }

        // ---- 3. Place tasks into free time after classes
        int cursor;
        if (lastClassEnd > 0) cursor = Math.max(roundUp15(now), lastClassEnd + 60); // 1h to eat / travel / rest
        else cursor = Math.max(roundUp15(now), 10 * 60);                           // no classes today

        List<PlanItem> tomorrow = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            PlanItem t = tasks.get(i);
            int len = lengths.get(i);
            if (cursor + len <= DAY_END) {
                t.start = cursor; t.end = cursor + len;
                plan.add(t);
                cursor = t.end + BREAK;
            } else {
                t.note = "No time left today → first thing tomorrow · " + t.note;
                tomorrow.add(t);
            }
        }

        if (cursor <= DAY_END) {
            PlanItem wrap = new PlanItem("🎒", "Pack bag & check tomorrow's timetable", "5-minute habit that saves mornings");
            wrap.start = Math.max(cursor, 21 * 60); wrap.end = wrap.start + 10;
            plan.add(wrap);
        }

        Collections.sort(plan, (a, b) -> Integer.compare(a.start, b.start));
        plan.addAll(tomorrow);

        if (plan.isEmpty()) {
            plan.add(new PlanItem("🌴", "Nothing urgent today",
                    "Add your timetable, assignments and exams so the planner has something to work with"));
        }
        return plan;
    }

    private static int roundUp15(int m) { return ((m + 14) / 15) * 15; }

    private static Subject find(List<Subject> list, long id) {
        for (Subject s : list) if (s.id == id) return s;
        return null;
    }
}
