package com.example.voteapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "vote_prefs";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_YEAR = "year";

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_CANDIDATE = "candidate";

    public static String getRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROLE, ROLE_STUDENT);
    }

    public static void setRole(Context context, String role) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    public static boolean isAdmin(Context context) {
        return ROLE_ADMIN.equals(getRole(context));
    }

    public static boolean isCandidate(Context context) {
        return ROLE_CANDIDATE.equals(getRole(context));
    }

    public static void setLoggedInUser(Context context,
                                       long userId,
                                       String role,
                                       String name,
                                       String department,
                                       String year) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_DEPARTMENT, department)
                .putString(KEY_YEAR, year)
                .apply();
    }

    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static long getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public static boolean isLoggedIn(Context context) {
        return getUserId(context) != -1L;
    }

    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    public static String getDepartment(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DEPARTMENT, "");
    }

    public static String getYear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_YEAR, "");
    }
}
