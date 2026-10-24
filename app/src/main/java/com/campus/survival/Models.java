package com.campus.survival;

/** Plain data classes for every table in the database. */
public final class Models {

    private Models() { }

    public static class Subject {
        public long id;
        public String name;
        public int lecTotal, lecAttended;
        public int pracTotal, pracDone;
        public int unitTotal, unitDone;

        public double attendancePct() { return lecTotal == 0 ? 0 : lecAttended * 100.0 / lecTotal; }
        public double practicalPct()  { return pracTotal == 0 ? 0 : pracDone * 100.0 / pracTotal; }
        public double syllabusPct()   { return unitTotal == 0 ? 0 : unitDone * 100.0 / unitTotal; }

        /** Lectures you must attend in a row to reach 75%. From (a+n)/(t+n) >= 0.75  =>  n >= 3t - 4a */
        public int lecturesNeededFor75() { return Math.max(0, 3 * lecTotal - 4 * lecAttended); }

        /** Lectures you can miss and still stay >= 75%. From a/(t+k) >= 0.75  =>  k <= (4a - 3t) / 3 */
        public int lecturesYouCanMiss() { return Math.max(0, (4 * lecAttended - 3 * lecTotal) / 3); }
    }

    public static class ClassSlot {
        public long id;
        public int day;          // 1 = Monday ... 7 = Sunday
        public long subjectId;
        public String subjectName;
        public int start, end;   // minutes from midnight
        public String room;
    }

    public static class Assignment {
        public long id;
        public long subjectId;
        public String subjectName;
        public String title;
        public String due;       // yyyy-MM-dd
        public int priority;     // 0 Low, 1 Medium, 2 High
        public boolean done;
    }

    public static class Exam {
        public long id;
        public long subjectId;
        public String subjectName;
        public String name;
        public String date;      // yyyy-MM-dd
    }

    public static final String[] PRIORITIES = {"Low", "Medium", "High"};
}
