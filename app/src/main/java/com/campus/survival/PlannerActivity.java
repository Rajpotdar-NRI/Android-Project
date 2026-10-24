package com.campus.survival;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.campus.survival.SurvivalEngine.PlanItem;

import java.util.List;

/** "Plan My Day" — shows the schedule produced by SurvivalEngine.planToday(). */
public class PlannerActivity extends BaseActivity {

    private RowAdapter<PlanItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupHeader("🎯 Plan My Day", DateUtil.prettyToday());

        ListView list = findViewById(R.id.list);
        adapter = new RowAdapter<>(this, R.layout.item_row, (row, it, pos) -> {
            TextView left = row.findViewById(R.id.tvLeft);
            left.setText(it.start < 0 ? "NEXT" : DateUtil.time(it.start).replace(' ', '\n'));
            ((TextView) row.findViewById(R.id.tvRowTitle)).setText(it.emoji + "  " + it.title);
            TextView sub = row.findViewById(R.id.tvRowSub);
            String dur = it.start < 0 ? "" : (it.end - it.start) + " min";
            String text = dur + (it.note == null || it.note.isEmpty() ? "" : (dur.isEmpty() ? "" : " · ") + it.note);
            sub.setText(text);
            sub.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.tvBadge).setVisibility(View.GONE);
            boolean past = it.end >= 0 && it.end <= DateUtil.nowMinutes();
            row.setAlpha(past ? 0.5f : 1f);
        });
        list.setAdapter(adapter);

        Button regen = findViewById(R.id.btnAdd);
        regen.setText("🔄  Regenerate Plan");
        regen.setOnClickListener(v -> { load(); toast("Plan updated from your latest data"); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<PlanItem> plan = SurvivalEngine.planToday(db);
        adapter.setItems(plan);
    }
}
