package com.campus.survival;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.campus.survival.Models.Subject;

import java.util.List;
import java.util.Locale;

/** Add / edit / delete subjects. Every tracker is built on these. */
public class SubjectsActivity extends BaseActivity {

    private RowAdapter<Subject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader("📘 Subjects", "Tap to edit · long-press to delete");

        ListView list = findViewById(R.id.list);
        TextView empty = findViewById(R.id.empty);
        empty.setText("No subjects yet.\nTap ＋ Add Subject to create your first one.");
        list.setEmptyView(empty);

        adapter = new RowAdapter<>(this, R.layout.item_row, (row, s, pos) -> {
            ((TextView) row.findViewById(R.id.tvLeft)).setText(initials(s.name));
            ((TextView) row.findViewById(R.id.tvRowTitle)).setText(s.name);
            ((TextView) row.findViewById(R.id.tvRowSub)).setText(String.format(Locale.US,
                    "Lectures %d/%d · Practicals %d/%d · Units %d/%d",
                    s.lecAttended, s.lecTotal, s.pracDone, s.pracTotal, s.unitDone, s.unitTotal));
            ((TextView) row.findViewById(R.id.tvBadge)).setText(
                    s.lecTotal == 0 ? "—" : String.format(Locale.US, "%.0f%%", s.attendancePct()));
        });
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) -> showDialog(adapter.getItem(pos)));
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            Subject s = adapter.getItem(pos);
            confirm("Delete " + s.name + "?\nIts classes, assignments and exams will also be removed.",
                    () -> { db.deleteSubject(s.id); load(); });
            return true;
        });

        TextView add = findViewById(R.id.btnAdd);
        add.setText("＋  Add Subject");
        add.setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<Subject> subjects = db.getSubjects();
        adapter.setItems(subjects);
    }

    private void showDialog(final Subject existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_subject, null);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etLecTotal = v.findViewById(R.id.etLecTotal);
        final EditText etLecAtt = v.findViewById(R.id.etLecAtt);
        final EditText etPrac = v.findViewById(R.id.etPracTotal);
        final EditText etUnits = v.findViewById(R.id.etUnitTotal);

        if (existing != null) {
            etName.setText(existing.name);
            etLecTotal.setText(String.valueOf(existing.lecTotal));
            etLecAtt.setText(String.valueOf(existing.lecAttended));
            etPrac.setText(String.valueOf(existing.pracTotal));
            etUnits.setText(String.valueOf(existing.unitTotal));
        }

        showForm(existing == null ? "New Subject" : "Edit " + existing.name, v, () -> {
            String name = etName.getText().toString().trim();
            int lt = parseInt(etLecTotal, 0), la = parseInt(etLecAtt, 0);
            int pt = parseInt(etPrac, 0), ut = parseInt(etUnits, 5);
            if (name.isEmpty()) { etName.setError("Required"); return false; }
            if (la > lt) { etLecAtt.setError("Can't attend more than were held"); return false; }
            if (ut <= 0 || ut > 20) { etUnits.setError("1 to 20 units"); return false; }
            if (pt < 0 || pt > 50) { etPrac.setError("0 to 50"); return false; }

            Subject s = existing != null ? existing : new Subject();
            s.name = name; s.lecTotal = lt; s.lecAttended = la; s.pracTotal = pt; s.unitTotal = ut;
            db.saveSubject(s);
            load();
            return true;
        });
    }

    private static String initials(String name) {
        String n = name.trim().toUpperCase(Locale.US);
        return n.length() <= 4 ? n : n.substring(0, 3);
    }
}
