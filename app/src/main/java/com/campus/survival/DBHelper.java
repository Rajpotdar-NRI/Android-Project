package com.campus.survival;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.campus.survival.Models.Assignment;
import com.campus.survival.Models.ClassSlot;
import com.campus.survival.Models.Exam;
import com.campus.survival.Models.Subject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** All SQLite access for the app. Single shared instance. */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_survival.db";
    private static final int DB_VERSION = 2; // v2: added users + per-user data

    private static DBHelper instance;

    public static synchronized DBHelper get(Context c) {
        if (instance == null) instance = new DBHelper(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;

    private DBHelper(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
        ctx = c;
    }

    /** Id of the logged-in user. Every query is filtered by this, so each account sees only its own data. */
    private String uid() {
        return String.valueOf(Prefs.userId(ctx));
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true); // deleting a subject deletes its classes/assignments/exams
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, pass_hash TEXT NOT NULL, salt TEXT NOT NULL)");
        db.execSQL("CREATE TABLE subjects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER REFERENCES users(id) ON DELETE CASCADE, name TEXT NOT NULL, " +
                "lec_total INTEGER DEFAULT 0, lec_attended INTEGER DEFAULT 0, " +
                "prac_total INTEGER DEFAULT 0, prac_done INTEGER DEFAULT 0, " +
                "unit_total INTEGER DEFAULT 5, unit_done INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE timetable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER, " +
                "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE, " +
                "start_min INTEGER, end_min INTEGER, room TEXT)");
        db.execSQL("CREATE TABLE assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE, " +
                "title TEXT, due TEXT, priority INTEGER, done INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE exams (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE, " +
                "name TEXT, date TEXT)");
        db.execSQL("CREATE TABLE focus (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, date TEXT, minutes INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS focus");
        db.execSQL("DROP TABLE IF EXISTS exams");
        db.execSQL("DROP TABLE IF EXISTS assignments");
        db.execSQL("DROP TABLE IF EXISTS timetable");
        db.execSQL("DROP TABLE IF EXISTS subjects");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // ---------------------------------------------------------------- Users / login

    /** Creates an account. Returns the new user id, or -1 if the email is already registered. */
    public long registerUser(String name, String email, String password) {
        String salt = newSalt();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("email", email.toLowerCase());
        v.put("salt", salt);
        v.put("pass_hash", hash(password, salt));
        return getWritableDatabase().insert("users", null, v); // UNIQUE(email) makes insert return -1
    }

    /** Returns the user id if email + password match, otherwise -1. */
    public long login(String email, String password) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, pass_hash, salt FROM users WHERE email = ?", new String[]{email.toLowerCase()});
        long id = -1;
        if (c.moveToFirst() && hash(password, c.getString(2)).equals(c.getString(1))) id = c.getLong(0);
        c.close();
        return id;
    }

    public boolean emailExists(String email) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM users WHERE email = ?", new String[]{email.toLowerCase()});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public String userName(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name FROM users WHERE id = ?", new String[]{String.valueOf(id)});
        String name = c.moveToFirst() ? c.getString(0) : "";
        c.close();
        return name;
    }

    /** Passwords are never stored. We store SHA-256(salt + password) and a random salt per user. */
    private static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder hex = new StringBuilder();
        for (byte x : b) hex.append(String.format("%02x", x));
        return hex.toString();
    }

    // ---------------------------------------------------------------- Subjects

    public long saveSubject(Subject s) {
        ContentValues v = new ContentValues();
        v.put("name", s.name);
        v.put("lec_total", s.lecTotal);
        v.put("lec_attended", s.lecAttended);
        v.put("prac_total", s.pracTotal);
        v.put("prac_done", Math.min(s.pracDone, s.pracTotal));
        v.put("unit_total", s.unitTotal);
        v.put("unit_done", Math.min(s.unitDone, s.unitTotal));
        SQLiteDatabase db = getWritableDatabase();
        if (s.id == 0) {
            v.put("user_id", Prefs.userId(ctx));
            return db.insert("subjects", null, v);
        }
        db.update("subjects", v, "id=?", new String[]{String.valueOf(s.id)});
        return s.id;
    }

    public List<Subject> getSubjects() {
        List<Subject> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM subjects WHERE user_id = ? ORDER BY name COLLATE NOCASE", new String[]{uid()});
        while (c.moveToNext()) {
            Subject s = new Subject();
            s.id = c.getLong(c.getColumnIndexOrThrow("id"));
            s.name = c.getString(c.getColumnIndexOrThrow("name"));
            s.lecTotal = c.getInt(c.getColumnIndexOrThrow("lec_total"));
            s.lecAttended = c.getInt(c.getColumnIndexOrThrow("lec_attended"));
            s.pracTotal = c.getInt(c.getColumnIndexOrThrow("prac_total"));
            s.pracDone = c.getInt(c.getColumnIndexOrThrow("prac_done"));
            s.unitTotal = c.getInt(c.getColumnIndexOrThrow("unit_total"));
            s.unitDone = c.getInt(c.getColumnIndexOrThrow("unit_done"));
            list.add(s);
        }
        c.close();
        return list;
    }

    public void deleteSubject(long id) {
        getWritableDatabase().delete("subjects", "id=?", new String[]{String.valueOf(id)});
    }

    /** One lecture happened: present = attended it, otherwise absent. */
    public void markLecture(long subjectId, boolean present) {
        getWritableDatabase().execSQL(
                "UPDATE subjects SET lec_total = lec_total + 1, lec_attended = lec_attended + ? WHERE id = ?",
                new Object[]{present ? 1 : 0, subjectId});
    }

    public void changePracticals(long subjectId, int delta) {
        getWritableDatabase().execSQL(
                "UPDATE subjects SET prac_done = MAX(0, MIN(prac_total, prac_done + ?)) WHERE id = ?",
                new Object[]{delta, subjectId});
    }

    public void changeUnits(long subjectId, int delta) {
        getWritableDatabase().execSQL(
                "UPDATE subjects SET unit_done = MAX(0, MIN(unit_total, unit_done + ?)) WHERE id = ?",
                new Object[]{delta, subjectId});
    }

    // ---------------------------------------------------------------- Timetable

    public void addClass(ClassSlot k) {
        ContentValues v = new ContentValues();
        v.put("day", k.day);
        v.put("subject_id", k.subjectId);
        v.put("start_min", k.start);
        v.put("end_min", k.end);
        v.put("room", k.room);
        getWritableDatabase().insert("timetable", null, v);
    }

    public List<ClassSlot> getClasses(int day) {
        List<ClassSlot> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT t.id, t.day, t.subject_id, s.name, t.start_min, t.end_min, t.room " +
                        "FROM timetable t JOIN subjects s ON s.id = t.subject_id " +
                        "WHERE t.day = ? AND s.user_id = ? ORDER BY t.start_min", new String[]{String.valueOf(day), uid()});
        while (c.moveToNext()) {
            ClassSlot k = new ClassSlot();
            k.id = c.getLong(0);
            k.day = c.getInt(1);
            k.subjectId = c.getLong(2);
            k.subjectName = c.getString(3);
            k.start = c.getInt(4);
            k.end = c.getInt(5);
            k.room = c.getString(6);
            list.add(k);
        }
        c.close();
        return list;
    }

    public void deleteClass(long id) {
        getWritableDatabase().delete("timetable", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------------------------------------------------------- Assignments

    public void addAssignment(Assignment a) {
        ContentValues v = new ContentValues();
        v.put("subject_id", a.subjectId);
        v.put("title", a.title);
        v.put("due", a.due);
        v.put("priority", a.priority);
        v.put("done", a.done ? 1 : 0);
        getWritableDatabase().insert("assignments", null, v);
    }

    /** Pending first (soonest due, highest priority), then completed. */
    public List<Assignment> getAssignments() {
        List<Assignment> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT a.id, a.subject_id, s.name, a.title, a.due, a.priority, a.done " +
                        "FROM assignments a JOIN subjects s ON s.id = a.subject_id WHERE s.user_id = ? " +
                        "ORDER BY a.done, a.due, a.priority DESC", new String[]{uid()});
        while (c.moveToNext()) {
            Assignment a = new Assignment();
            a.id = c.getLong(0);
            a.subjectId = c.getLong(1);
            a.subjectName = c.getString(2);
            a.title = c.getString(3);
            a.due = c.getString(4);
            a.priority = c.getInt(5);
            a.done = c.getInt(6) == 1;
            list.add(a);
        }
        c.close();
        return list;
    }

    public void setAssignmentDone(long id, boolean done) {
        ContentValues v = new ContentValues();
        v.put("done", done ? 1 : 0);
        getWritableDatabase().update("assignments", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAssignment(long id) {
        getWritableDatabase().delete("assignments", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------------------------------------------------------- Exams

    public void addExam(Exam e) {
        ContentValues v = new ContentValues();
        v.put("subject_id", e.subjectId);
        v.put("name", e.name);
        v.put("date", e.date);
        getWritableDatabase().insert("exams", null, v);
    }

    /** Upcoming exams first (nearest date), past exams at the end. */
    public List<Exam> getExams() {
        List<Exam> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT e.id, e.subject_id, s.name, e.name, e.date " +
                        "FROM exams e JOIN subjects s ON s.id = e.subject_id WHERE s.user_id = ? " +
                        "ORDER BY (e.date < ?), e.date", new String[]{uid(), DateUtil.today()});
        while (c.moveToNext()) {
            Exam e = new Exam();
            e.id = c.getLong(0);
            e.subjectId = c.getLong(1);
            e.subjectName = c.getString(2);
            e.name = c.getString(3);
            e.date = c.getString(4);
            list.add(e);
        }
        c.close();
        return list;
    }

    public void deleteExam(long id) {
        getWritableDatabase().delete("exams", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------------------------------------------------------- Focus timer

    public void addFocus(int minutes) {
        ContentValues v = new ContentValues();
        v.put("user_id", Prefs.userId(ctx));
        v.put("date", DateUtil.today());
        v.put("minutes", minutes);
        getWritableDatabase().insert("focus", null, v);
    }

    public int focusMinutesToday() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(minutes), 0) FROM focus WHERE date = ? AND user_id = ?",
                new String[]{DateUtil.today(), uid()});
        int m = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return m;
    }

    // ---------------------------------------------------------------- Reset / demo

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Only the logged-in user's data. Classes, assignments and exams go via ON DELETE CASCADE.
            db.delete("focus", "user_id = ?", new String[]{uid()});
            db.delete("subjects", "user_id = ?", new String[]{uid()});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Realistic sample data so the app can be demonstrated in 5 seconds. */
    public void loadDemoData() {
        clearAll();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long java = demoSubject("Java", 22, 16, 12, 4, 5, 2);
            long mad = demoSubject("MAD", 20, 18, 10, 6, 5, 3);
            long cloud = demoSubject("Cloud Computing", 18, 12, 8, 3, 5, 1);
            long py = demoSubject("Python", 15, 13, 10, 7, 5, 4);

            long[] order = {java, mad, cloud, py};
            for (int day = 1; day <= 6; day++) {
                int slots = day == 6 ? 2 : 3;
                for (int i = 0; i < slots; i++) {
                    ClassSlot k = new ClassSlot();
                    k.day = day;
                    k.subjectId = order[(day + i) % order.length];
                    k.start = 9 * 60 + i * 60 + (i == 2 ? 60 : 0); // 9, 10, then 12 after break
                    k.end = k.start + 60;
                    k.room = i == 2 ? "Lab " + (day + 1) : "Room " + (200 + day);
                    addClass(k);
                }
            }

            demoAssignment(java, "Java Assignment 3", 1, 2);
            demoAssignment(cloud, "Cloud Case Study", 3, 1);
            demoAssignment(mad, "MAD Micro-project Report", 6, 2);
            demoAssignment(py, "Python Assignment 2", -2, 1);

            Exam e1 = new Exam();
            e1.subjectId = java; e1.name = "Java Unit Test 2"; e1.date = DateUtil.plusDays(9);
            addExam(e1);
            Exam e2 = new Exam();
            e2.subjectId = mad; e2.name = "MAD End-Sem"; e2.date = DateUtil.plusDays(32);
            addExam(e2);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long demoSubject(String name, int lt, int la, int pt, int pd, int ut, int ud) {
        Subject s = new Subject();
        s.name = name; s.lecTotal = lt; s.lecAttended = la;
        s.pracTotal = pt; s.pracDone = pd; s.unitTotal = ut; s.unitDone = ud;
        return saveSubject(s);
    }

    private void demoAssignment(long subjectId, String title, int dueInDays, int priority) {
        Assignment a = new Assignment();
        a.subjectId = subjectId; a.title = title;
        a.due = DateUtil.plusDays(dueInDays); a.priority = priority;
        addAssignment(a);
    }
}
