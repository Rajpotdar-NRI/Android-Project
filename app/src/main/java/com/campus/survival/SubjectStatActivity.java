package com.campus.survival;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.campus.survival.Models.Subject;

import java.util.List;

/**
 * Shared screen for Attendance, Practicals and Syllabus:
 * one card per subject with a %, progress bar, hint and two buttons.
 * Subclasses only describe WHAT to show; this class handles HOW.
 */
public abstract class SubjectStatActivity extends BaseActivity {

    protected RowAdapter<Subject> adapter;

    /** Colours for hints */
    protected static final int RED = Color.parseColor("#D64545");
    protected static final int GREEN = Color.parseColor("#5E9A4B");
    protected static final int ORANGE = Color.parseColor("#E9762B");

    protected abstract String title();
    protected abstract String subtitle(List<Subject> subjects);
    protected abstract void bind(Subject s, TextView pct, ProgressBar pb, TextView info, TextView hint, Button a, Button b);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader(title(), "");

        ListView list = findViewById(R.id.list);
        TextView empty = findViewById(R.id.empty);
        empty.setText("No subjects yet.\nAdd subjects first — every tracker uses them.");
        list.setEmptyView(empty);

        adapter = new RowAdapter<>(this, R.layout.item_stat, (row, s, pos) -> {
            ((TextView) row.findViewById(R.id.tvName)).setText(s.name);
            bind(s,
                    row.findViewById(R.id.tvPct),
                    row.findViewById(R.id.pb),
                    row.findViewById(R.id.tvInfo),
                    row.findViewById(R.id.tvHint),
                    row.findViewById(R.id.btnA),
                    row.findViewById(R.id.btnB));
        });
        list.setAdapter(adapter);

        Button add = findViewById(R.id.btnAdd);
        add.setText("📘  Manage Subjects");
        add.setOnClickListener(v -> startActivity(new Intent(this, SubjectsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    protected void reload() {
        List<Subject> subjects = db.getSubjects();
        adapter.setItems(subjects);
        setSubtitle(subtitle(subjects));
    }
}
