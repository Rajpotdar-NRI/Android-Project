package com.campus.survival;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.campus.survival.Models.Exam;
import com.campus.survival.Models.Subject;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExamsActivity extends BaseActivity {

    private RowAdapter<Exam> adapter;
    private List<Subject> subjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader("⏳ Exams", "Long-press an exam to delete it");

        ListView list = findViewById(R.id.list);
        TextView empty = findViewById(R.id.empty);
        empty.setText("No exams added.\nAdd them early — the planner schedules revision for you.");
        list.setEmptyView(empty);

        adapter = new RowAdapter<>(this, R.layout.item_row, (row, e, pos) -> {
            int d = DateUtil.daysUntil(e.date);
            TextView left = row.findViewById(R.id.tvLeft);
            left.setText(d < 0 ? "OVER" : d == 0 ? "TODAY" : d + "\nday" + (d == 1 ? "" : "s"));
            left.setTextColor(getResources().getColor(d >= 0 && d <= 7 ? R.color.red : R.color.orange_deep, null));

            ((TextView) row.findViewById(R.id.tvRowTitle)).setText(e.name);

            Subject s = find(e.subjectId);
            double syl = s == null ? 0 : s.syllabusPct();
            ((TextView) row.findViewById(R.id.tvRowSub)).setText(String.format(Locale.US,
                    "%s · %s · Syllabus %.0f%%", e.subjectName, DateUtil.pretty(e.date), syl));

            TextView badge = row.findViewById(R.id.tvBadge);
            badge.setVisibility(View.VISIBLE);
            if (d < 0) badge.setText("Done");
            else if (syl >= 80) badge.setText("✅ Ready");
            else if (d <= 7) badge.setText("🔥 Urgent");
            else badge.setText("📖 Prepare");
            row.setAlpha(d < 0 ? 0.55f : 1f);
        });
        list.setAdapter(adapter);

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            Exam e = adapter.getItem(pos);
            confirm("Delete " + e.name + "?", () -> { db.deleteExam(e.id); load(); });
            return true;
        });

        Button add = findViewById(R.id.btnAdd);
        add.setText("＋  Add Exam");
        add.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        subjects = db.getSubjects();
        adapter.setItems(db.getExams());
    }

    private Subject find(long id) {
        if (subjects != null) for (Subject s : subjects) if (s.id == id) return s;
        return null;
    }

    private void showAddDialog() {
        final List<Subject> subs = db.getSubjects();
        if (!requireSubjects(subs)) return;

        View v = getLayoutInflater().inflate(R.layout.dialog_exam, null);
        final Spinner spSubject = v.findViewById(R.id.spSubject);
        final EditText etTitle = v.findViewById(R.id.etTitle);
        final Button btnDate = v.findViewById(R.id.btnDate);
        fillSubjectSpinner(spSubject, subs);

        final String[] date = {DateUtil.plusDays(14)};
        btnDate.setText("Exam date  " + DateUtil.pretty(date[0]));
        btnDate.setOnClickListener(b -> {
            Calendar c = DateUtil.toCalendar(date[0]);
            DatePickerDialog dlg = new DatePickerDialog(this, (dp, y, m, d) -> {
                date[0] = DateUtil.fromParts(y, m, d);
                btnDate.setText("Exam date  " + DateUtil.pretty(date[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dlg.show();
        });

        showForm("New Exam", v, () -> {
            Subject s = subs.get(spSubject.getSelectedItemPosition());
            String name = etTitle.getText().toString().trim();
            Exam e = new Exam();
            e.subjectId = s.id;
            e.name = name.isEmpty() ? s.name + " Exam" : name;
            e.date = date[0];
            db.addExam(e);
            load();
            return true;
        });
    }
}
