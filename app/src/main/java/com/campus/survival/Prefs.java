package com.campus.survival;

import android.content.Context;
import android.content.SharedPreferences;

/** Small settings stored in SharedPreferences. */
public final class Prefs {
    private static final String FILE = "campus_prefs";
    private static final String KEY_TAP_FX = "tap_fx";
    private static final String KEY_USER = "user_id";

    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean tapFx(Context c) { return sp(c).getBoolean(KEY_TAP_FX, true); }

    public static void setTapFx(Context c, boolean on) { sp(c).edit().putBoolean(KEY_TAP_FX, on).apply(); }

    // ---- Login session: stays logged in until the user taps Logout
    public static long userId(Context c) { return sp(c).getLong(KEY_USER, -1); }

    public static boolean isLoggedIn(Context c) { return userId(c) != -1; }

    public static void login(Context c, long id) { sp(c).edit().putLong(KEY_USER, id).apply(); }

    public static void logout(Context c) { sp(c).edit().remove(KEY_USER).apply(); }
}
