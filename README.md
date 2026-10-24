# Campus Survival — Android (Java) Micro-project

A personal college assistant that answers: **"What do I need to do today?"**

## Open & run
1. Android Studio → **File → Open** → select the `CampusSurvival` folder.
2. Let Gradle sync (needs internet the first time). Accept any "upgrade AGP" prompt only if sync fails.
3. Run on an emulator or phone (Android 7.0 / API 24+).
4. Create an account on the login screen, then tap **⚙ → Load demo data** to see everything filled in instantly.

## Tech
- Java, XML layouts, **SQLite** (`SQLiteOpenHelper`), `SharedPreferences`
- No third-party libraries — only the Android SDK (ListView + BaseAdapter, AlertDialog, Date/Time pickers, CountDownTimer, custom `View` with `Canvas`)

## Modules
| Screen | What it does |
|---|---|
| Login / Register | SQLite `users` table, salted SHA-256 password hash, stays logged in via SharedPreferences, Logout in ⚙ |
| Dashboard | Greeting, Survival Score, today's classes, alerts, module tiles |
| Subjects | Add/edit/delete subjects (lectures, practical count, unit count) |
| Timetable | Mon–Sun chips, add class with time pickers, clash detection |
| Attendance | Present/Absent buttons, 75% rule, "attend next N" / "can miss K" |
| Practicals | Complete / undo practicals, progress per subject |
| Assignments | Due date, priority, tap to mark done, overdue highlighting |
| Exams | Countdown sorted by date, readiness badge from syllabus % |
| Syllabus | Unit-by-unit ✅ 🔄 ❌ progress |
| Plan My Day | Rule-based daily schedule (see below) |
| Focus Timer | 25/45/60 min Pomodoro, sessions saved to DB |
| Tap "+" effect | Game-style crosshair on every touch (toggle in ⚙) |

## Key logic (be ready to explain in viva)
**Attendance to 75%:** `(a+n)/(t+n) ≥ 0.75  ⇒  n = 3t − 4a`
**Lectures you can miss:** `a/(t+k) ≥ 0.75  ⇒  k = ⌊(4a − 3t)/3⌋`

**Survival Score** = weighted average of available parts:
Attendance 35% · Practicals 25% · Assignments 20% · Syllabus 20%
90+ 🏆 College Legend · 75–89 😎 Surviving · 50–74 😰 Danger Zone · <50 💀 Academic Crisis

**Plan My Day** (`SurvivalEngine.planToday`):
1. Today's classes are fixed blocks (warns if that subject's attendance < 75%).
2. Self-study queue in priority order: overdue / due-in-2-days assignments → revision for nearest exam (≤21 days) → practical furthest behind → other high-priority assignments → weakest syllabus subject.
3. Blocks placed from 1 h after the last class (or now), with 15-min breaks, until 10:30 PM. Whatever doesn't fit goes to a "NEXT" list.

**Login:** `LoginActivity` is the launcher. Register → validate (name, email pattern, password ≥ 6, confirm match, email not taken) → store `SHA-256(salt + password)` with a random 16-byte salt. Login → re-hash the typed password with that user's salt and compare. The logged-in user id lives in SharedPreferences; every query filters by it (`subjects.user_id`, and other tables join through subjects), so two accounts on one phone never see each other's data.

**Tap effect:** `BaseActivity.dispatchTouchEvent()` sees every touch before any view, adds a `TapPlusView` (custom `onDraw` with 4 `drawLine` arms) to the window's DecorView at the touch point, animates scale + rotation + fade, then removes it. It returns `super.dispatchTouchEvent()`, so buttons still work normally.

## Files
```
java/com/campus/survival/
  LoginActivity.java       login + create account (launcher)
  BaseActivity.java        tap effect + shared helpers (every screen extends it)
  TapPlusView.java         the "+" crosshair drawn with Canvas
  DBHelper.java            SQLite: subjects, timetable, assignments, exams, focus
  Models.java              data classes
  SurvivalEngine.java      score + Plan My Day algorithm
  DateUtil.java, Prefs.java, RowAdapter.java
  MainActivity.java        dashboard
  SubjectsActivity, TimetableActivity, AssignmentsActivity, ExamsActivity,
  SubjectStatActivity (base) → AttendanceActivity, PracticalActivity, SyllabusActivity,
  PlannerActivity, FocusTimerActivity
```

## Future scope (mention in report, don't build now)
Timetable image scan (OCR), daily reminder notifications, cloud backup, home-screen widget.
