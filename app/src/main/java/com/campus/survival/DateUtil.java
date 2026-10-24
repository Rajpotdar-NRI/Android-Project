package com.campus.survival;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Date helpers Dates are stored as yyyy- MM-dd times as minutes from midnight */
public final class DateUtil {

    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    public static final String[] DAYS_SHORT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private static final SimpleDateFormat DB = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DateUtil() { }

    public static String today() { return DB.format(new Date()); }

    public static String plusDays(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        return DB.format(c.getTime());
    }

    public static String fromParts(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        return DB.format(c.getTime());
    }

    public static Calendar toCalendar(String date) {
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(DB.parse(date));
        } catch (ParseException ignored) { }
        return c;
    }

    /** Whole days from today until the date (negative = in the past). */
    public static int daysUntil(String date) {
        Calendar target = toCalendar(date);
        zeroTime(target);
        Calendar now = Calendar.getInstance();
        zeroTime(now);
        return (int) Math.round((target.getTimeInMillis() - now.getTimeInMillis()) / 86400000.0);
    }

    private static void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /** "28 Aug" */
    public static String pretty(String date) {
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(toCalendar(date).getTime());
    }

    /** "Thursday, 24 Sep" */
    public static String prettyToday() {
        return new SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(new Date());
    }

    /** 1 = Monday ... 7 = Sunday */
    public static int todayDow() {
        int cal = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // Sunday = 1
        return ((cal + 5) % 7) + 1;
    }

    public static int nowMinutes() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** 540 -> "9:00 AM" */
    public static String time(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        String ap = h >= 12 ? "PM" : "AM";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, m, ap);
    }

    public static String daysLabel(int d) {
        if (d == 0) return "Today";
        if (d == 1) return "Tomorrow";
        if (d < 0) return (-d) + "d late";
        return d + " days";
    }
}
