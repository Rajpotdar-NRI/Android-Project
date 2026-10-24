package com.campus.survival;

import android.app.DatePickerDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.campus.survival.Models.Assignment;
import com.campus.survival.Models.Subject;

import java.util.Calendar;
import java.util.List;

/** Assignment tracker: tap = toggle done, long-press = delete. */
public class AssignmentsActivity extends BaseActivity {

    private RowAdapter<Assignment> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader("📝 Assignments", "");

        ListView list = findViewById(R.id.list);
        TextView empty = findViewById(R.id.empty);
        empty.setText("No assignments. Enjoy it while it lasts 😌");
        list.setEmptyView(empty);

        adapter = new RowAdapter<>(this, R.layout.item_row, (row, a, pos) -> {
            int d = DateUtil.daysUntil(a.due);
            TextView left = row.findViewById(R.id.tvLeft);
            TextView title = row.findViewById(R.id.tvRowTitle);
            TextView sub = row.findViewById(R.id.tvRowSub);
            TextView badge = row.findViewById(R.id.tvBadge);

            left.setText(a.done ? "✓" : d < 0 ? "LATE" : d == 0 ? "TODAY" : d + "\nday" + (d == 1 ? "" : "s"));
            left.setTextColor(getResources().getColor(
                    a.done ? R.color.green : (d < 0 ? R.color.red : R.color.orange_deep), null));

            title.setText(a.title);
            title.setPaintFlags(a.done ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            String pr = a.priority == 2 ? "🔴 High" : a.priority == 1 ? "🟠 Medium" : "🟢 Low";
            sub.setText(a.subjectName + " · Due " + DateUtil.pretty(a.due) + " · " + pr);

            badge.setVisibility(View.VISIBLE);
            badge.setText(a.done ? "Done" : "Pending");
            row.setAlpha(a.done ? 0.6f : 1f);
        });
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) -> {
            Assignment a = adapter.getItem(pos);
            db.setAssignmentDone(a.id, !a.done);
            if (!a.done) toast("Nice! " + a.title + " done ✅");
            load();
        });
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            Assignment a = adapter.getItem(pos);
            confirm("Delete \"" + a.title + "\"?", () -> { db.deleteAssignment(a.id); load(); });
            return true;
        });

        Button add = findViewById(R.id.btnAdd);
        add.setText("＋  Add Assignment");
        add.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<Assignment> items = db.getAssignments();
        int pending = 0, late = 0;
        for (Assignment a : items) {
            if (a.done) continue;
            pending++;
            if (DateUtil.daysUntil(a.due) < 0) late++;
        }
        adapter.setItems(items);
        setSubtitle(pending + " pending" + (late > 0 ? " · " + late + " overdue" : "") + " · tap to mark done");
    }

    private void showAddDialog() {
        final List<Subject> subjects = db.getSubjects();
        if (!requireSubjects(subjects)) return;

        View v = getLayoutInflater().inflate(R.layout.dialog_assignment, null);
        final Spinner spSubject = v.findViewById(R.id.spSubject);
        final EditText etTitle = v.findViewById(R.id.etTitle);
        final Button btnDate = v.findViewById(R.id.btnDate);
        final Spinner spPriority = v.findViewById(R.id.spPriority);

        fillSubjectSpinner(spSubject, subjects);
        fillSpinner(spPriority, new String[]{"🟢 Low priority", "🟠 Medium priority", "🔴 High priority"});
        spPriority.setSelection(1);

        final String[] due = {DateUtil.plusDays(1)};
        btnDate.setText("Due  " + DateUtil.pretty(due[0]));
        btnDate.setOnClickListener(b -> {
            Calendar c = DateUtil.toCalendar(due[0]);
            new DatePickerDialog(this, (dp, y, m, d) -> {
                due[0] = DateUtil.fromParts(y, m, d);
                btnDate.setText("Due  " + DateUtil.pretty(due[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        showForm("New Assignment", v, () -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) { etTitle.setError("Required"); return false; }
            Assignment a = new Assignment();
            a.subjectId = subjects.get(spSubject.getSelectedItemPosition()).id;
            a.title = title;
            a.due = due[0];
            a.priority = spPriority.getSelectedItemPosition();
            db.addAssignment(a);
            load();
            return true;
        });
    }
}
