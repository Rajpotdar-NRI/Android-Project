package com.campus.survival;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.campus.survival.Models.ClassSlot;
import com.campus.survival.Models.Subject;

import java.util.List;

/** Weekly timetable with day chips (Mon–Sun). */
public class TimetableActivity extends BaseActivity {

    private RowAdapter<ClassSlot> adapter;
    private int selectedDay;
    private final TextView[] chips = new TextView[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader("📅 Timetable", "Long-press a class to delete it");

        selectedDay = DateUtil.todayDow();
        buildDayChips();

        ListView list = findViewById(R.id.list);
        TextView empty = findViewById(R.id.empty);
        empty.setText("No classes on this day.\nTap ＋ Add Class.");
        list.setEmptyView(empty);

        adapter = new RowAdapter<>(this, R.layout.item_row, (row, k, pos) -> {
            ((TextView) row.findViewById(R.id.tvLeft)).setText(DateUtil.time(k.start).replace(' ', '\n'));
            ((TextView) row.findViewById(R.id.tvRowTitle)).setText(k.subjectName);
            String sub = DateUtil.time(k.start) + " – " + DateUtil.time(k.end);
            if (k.room != null && !k.room.isEmpty()) sub += " · " + k.room;
            ((TextView) row.findViewById(R.id.tvRowSub)).setText(sub);

            TextView badge = row.findViewById(R.id.tvBadge);
            int now = DateUtil.nowMinutes();
            boolean today = k.day == DateUtil.todayDow();
            if (today && now >= k.start && now < k.end) { badge.setText("NOW"); badge.setVisibility(View.VISIBLE); }
            else if (today && now >= k.end) { badge.setText("Done"); badge.setVisibility(View.VISIBLE); }
            else badge.setVisibility(View.GONE);
        });
        list.setAdapter(adapter);

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            ClassSlot k = adapter.getItem(pos);
            confirm("Delete " + k.subjectName + " at " + DateUtil.time(k.start) + "?",
                    () -> { db.deleteClass(k.id); load(); });
            return true;
        });

        Button add = findViewById(R.id.btnAdd);
        add.setText("＋  Add Class");
        add.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void buildDayChips() {
        findViewById(R.id.dayScroll).setVisibility(View.VISIBLE);
        LinearLayout bar = findViewById(R.id.dayBar);
        for (int i = 0; i < 7; i++) {
            final int day = i + 1;
            TextView chip = new TextView(this);
            chip.setText(DateUtil.DAYS_SHORT[i]);
            chip.setTextSize(14);
            chip.setPadding(dp(18), dp(8), dp(18), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            chip.setOnClickListener(v -> { selectedDay = day; load(); });
            bar.addView(chip, lp);
            chips[i] = chip;
        }
    }

    private void load() {
        for (int i = 0; i < 7; i++) {
            boolean sel = (i + 1) == selectedDay;
            chips[i].setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            chips[i].setTextColor(getResources().getColor(sel ? R.color.white : R.color.brown, null));
        }
        List<ClassSlot> classes = db.getClasses(selectedDay);
        adapter.setItems(classes);
        setSubtitle(DateUtil.DAYS[selectedDay - 1] + " · " + classes.size() + (classes.size() == 1 ? " class" : " classes"));
    }

    private void showAddDialog() {
        final List<Subject> subjects = db.getSubjects();
        if (!requireSubjects(subjects)) return;

        View v = getLayoutInflater().inflate(R.layout.dialog_class, null);
        final Spinner spDay = v.findViewById(R.id.spDay);
        final Spinner spSubject = v.findViewById(R.id.spSubject);
        final Button btnStart = v.findViewById(R.id.btnStartTime);
        final Button btnEnd = v.findViewById(R.id.btnEndTime);
        final EditText etRoom = v.findViewById(R.id.etRoom);

        fillSpinner(spDay, DateUtil.DAYS);
        spDay.setSelection(selectedDay - 1);
        fillSubjectSpinner(spSubject, subjects);

        final int[] start = {9 * 60};
        final int[] end = {10 * 60};
        btnStart.setText("Start  " + DateUtil.time(start[0]));
        btnEnd.setText("End  " + DateUtil.time(end[0]));

        btnStart.setOnClickListener(b -> new TimePickerDialog(this, (tp, h, m) -> {
            start[0] = h * 60 + m;
            if (end[0] <= start[0]) end[0] = start[0] + 60;
            btnStart.setText("Start  " + DateUtil.time(start[0]));
            btnEnd.setText("End  " + DateUtil.time(end[0]));
        }, start[0] / 60, start[0] % 60, false).show());

        btnEnd.setOnClickListener(b -> new TimePickerDialog(this, (tp, h, m) -> {
            end[0] = h * 60 + m;
            btnEnd.setText("End  " + DateUtil.time(end[0]));
        }, end[0] / 60, end[0] % 60, false).show());

        showForm("New Class", v, () -> {
            if (end[0] <= start[0]) { toast("End time must be after start time"); return false; }
            int day = spDay.getSelectedItemPosition() + 1;
            for (ClassSlot other : db.getClasses(day)) {
                if (start[0] < other.end && end[0] > other.start) {
                    toast("Clashes with " + other.subjectName + " at " + DateUtil.time(other.start));
                    return false;
                }
            }
            ClassSlot k = new ClassSlot();
            k.day = day;
            k.subjectId = subjects.get(spSubject.getSelectedItemPosition()).id;
            k.start = start[0];
            k.end = end[0];
            k.room = etRoom.getText().toString().trim();
            db.addClass(k);
            selectedDay = k.day;
            load();
            return true;
        });
    }
}
