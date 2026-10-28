package com.gathin.tvhome;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fetches Google Calendar events directly from the TV's logged-in Google Account
 * (via Android Calendar Provider CalendarContract.Instances), with fallback to iCal/ICS.
 */
public class CalendarFetcher {
    private static final String TAG = "CalendarFetcher";
    private static final String PREF_NAME = "tvhome_calendar";
    public static final String KEY_GCAL_URL = "gcal_ics_url";

    public static class Event {
        public String title;
        public String time;
        public Date startDate;
        public boolean isAllDay;
        public String accountName;

        public Event(String title, String time, Date startDate, boolean isAllDay, String accountName) {
            this.title = title;
            this.time = time;
            this.startDate = startDate;
            this.isAllDay = isAllDay;
            this.accountName = accountName;
        }
    }

    public interface Callback {
        void onLoaded(List<Event> events, String accountLabel, boolean isLiveSync);
    }

    public static void setCalendarUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GCAL_URL, url).apply();
    }

    public static String getCalendarUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_GCAL_URL, "");
    }

    public static void fetch(Context context, Callback callback) {
        new Thread(() -> {
            // 1. Primary: Query the TV's logged-in Google account via CalendarContract
            List<Event> deviceEvents = fetchFromDeviceCalendar(context);
            if (!deviceEvents.isEmpty()) {
                String account = deviceEvents.get(0).accountName;
                String label = "GOOGLE";
                if (account != null && account.contains("@")) {
                    label = account.substring(0, account.indexOf("@"));
                }
                final String finalLabel = label;
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onLoaded(deviceEvents, finalLabel, true);
                });
                return;
            }

            // 2. Secondary: Check if a custom ICS URL was configured
            String icsUrl = getCalendarUrl(context);
            if (!icsUrl.trim().isEmpty()) {
                List<Event> icsEvents = fetchFromIcs(icsUrl);
                if (!icsEvents.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onLoaded(icsEvents, "GOOGLE", true);
                    });
                    return;
                }
            }

            // 3. Fallback: Clean default agenda
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onLoaded(getDefaultEvents(), "GOOGLE", false);
            });
        }).start();
    }

    /**
     * Reads upcoming calendar events from the Android device Calendar Provider
     * for the TV's logged-in Google account(s).
     */
    public static List<Event> fetchFromDeviceCalendar(Context context) {
        List<Event> events = new ArrayList<>();
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            long startMs = startCal.getTimeInMillis();
            long endMs = startMs + (180L * 24 * 60 * 60 * 1000L); // Next 6 months

            Uri uri = Uri.parse("content://com.android.calendar/instances/when/" + startMs + "/" + endMs);
            String[] projection = new String[] {
                    "title",
                    "begin",
                    "end",
                    "allDay",
                    "ownerAccount"
            };

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    "begin ASC"
            );

            if (cursor != null) {
                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
                SimpleDateFormat dayFmt = new SimpleDateFormat("d MMM", Locale.getDefault());
                Calendar todayCal = Calendar.getInstance();
                int todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR);
                int todayYear = todayCal.get(Calendar.YEAR);

                while (cursor.moveToNext() && events.size() < 12) {
                    String title = cursor.getString(0);
                    long beginMs = cursor.getLong(1);
                    int allDayInt = cursor.getInt(3);
                    String owner = cursor.getString(4);
                    boolean isAllDay = (allDayInt == 1);

                    if (title != null && !title.trim().isEmpty()) {
                        Calendar evCal = Calendar.getInstance();
                        evCal.setTimeInMillis(beginMs);

                        String timeStr;
                        if (evCal.get(Calendar.YEAR) == todayYear && evCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                            timeStr = isAllDay ? "TODAY" : timeFmt.format(new Date(beginMs));
                        } else if (evCal.get(Calendar.YEAR) == todayYear && evCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear + 1) {
                            timeStr = isAllDay ? "TOMORROW" : "TMRW " + timeFmt.format(new Date(beginMs));
                        } else {
                            timeStr = dayFmt.format(new Date(beginMs));
                        }

                        events.add(new Event(title.trim(), timeStr, new Date(beginMs), isAllDay, owner));
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying device calendar: " + e.getMessage());
        }
        return events;
    }

    private static List<Event> fetchFromIcs(String icsUrl) {
        List<Event> events = new ArrayList<>();
        try {
            URL url = new URL(icsUrl.trim());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 AndroidTV Zamax");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                events = parseIcsString(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching ICS: " + e.getMessage());
        }
        return events;
    }

    private static List<Event> parseIcsString(String ics) {
        List<Event> events = new ArrayList<>();
        try {
            String[] lines = ics.split("\r?\n");
            String summary = null;
            String dtStart = null;
            boolean inEvent = false;

            Calendar today = Calendar.getInstance();
            int todayYear = today.get(Calendar.YEAR);
            int todayDayOfYear = today.get(Calendar.DAY_OF_YEAR);

            SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
            SimpleDateFormat dayFmt = new SimpleDateFormat("d MMM", Locale.getDefault());
            SimpleDateFormat icsFmtUtc = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            icsFmtUtc.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            SimpleDateFormat icsFmtDate = new SimpleDateFormat("yyyyMMdd", Locale.US);

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.equals("BEGIN:VEVENT")) {
                    inEvent = true;
                    summary = null;
                    dtStart = null;
                } else if (line.equals("END:VEVENT")) {
                    if (summary != null && dtStart != null) {
                        Date d = null;
                        boolean allDay = false;
                        if (dtStart.contains("T")) {
                            try {
                                d = icsFmtUtc.parse(dtStart.replaceAll("^.*:", ""));
                            } catch (Exception ignored) {}
                        } else {
                            try {
                                d = icsFmtDate.parse(dtStart.replaceAll("^.*:", ""));
                                allDay = true;
                            } catch (Exception ignored) {}
                        }

                        if (d != null) {
                            Calendar evCal = Calendar.getInstance();
                            evCal.setTime(d);
                            if (evCal.get(Calendar.YEAR) == todayYear && evCal.get(Calendar.DAY_OF_YEAR) >= todayDayOfYear) {
                                String timeStr;
                                if (evCal.get(Calendar.YEAR) == todayYear && evCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                                    timeStr = allDay ? "TODAY" : timeFmt.format(d);
                                } else if (evCal.get(Calendar.YEAR) == todayYear && evCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear + 1) {
                                    timeStr = allDay ? "TOMORROW" : "TMRW " + timeFmt.format(d);
                                } else {
                                    timeStr = dayFmt.format(d);
                                }
                                events.add(new Event(summary, timeStr, d, allDay, "Google"));
                            }
                        }
                    }
                    inEvent = false;
                } else if (inEvent) {
                    if (line.startsWith("SUMMARY:")) {
                        summary = line.substring(8);
                    } else if (line.startsWith("DTSTART")) {
                        dtStart = line;
                    }
                }
            }
            Collections.sort(events, (a, b) -> a.startDate.compareTo(b.startDate));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ICS: " + e.getMessage());
        }
        return events;
    }

    public static List<Event> getDefaultEvents() {
        List<Event> list = new ArrayList<>();
        list.add(new Event("Strategy & Planning", "TODAY", new Date(), true, "Google"));
        list.add(new Event("Google Calendar Sync", "02:00 PM", new Date(), false, "Google"));
        list.add(new Event("Team Sync & Review", "05:00 PM", new Date(), false, "Google"));
        return list;
    }
}
