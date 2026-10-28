package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarWidgetManager {

    private final Activity activity;
    private final View rootWidget;
    private final TextView tvMonthYear;
    private final GridLayout gridDays;
    private final TextView tvTodayDate;
    private final TextView tvSyncStatus;

    // Event 1
    private final TextView tvEv1Time;
    private final TextView tvEv1Title;
    // Event 2
    private final TextView tvEv2Time;
    private final TextView tvEv2Title;
    // Event 3
    private final TextView tvEv3Time;
    private final TextView tvEv3Title;

    public CalendarWidgetManager(Activity activity) {
        this.activity = activity;
        this.rootWidget = activity.findViewById(R.id.calendarWidget);
        this.tvMonthYear = activity.findViewById(R.id.calMonthYear);
        this.gridDays = activity.findViewById(R.id.calGridDays);
        this.tvTodayDate = activity.findViewById(R.id.calTodayDate);
        this.tvSyncStatus = activity.findViewById(R.id.calSyncStatus);

        this.tvEv1Time = activity.findViewById(R.id.calEv1Time);
        this.tvEv1Title = activity.findViewById(R.id.calEv1Title);
        this.tvEv2Time = activity.findViewById(R.id.calEv2Time);
        this.tvEv2Title = activity.findViewById(R.id.calEv2Title);
        this.tvEv3Time = activity.findViewById(R.id.calEv3Time);
        this.tvEv3Title = activity.findViewById(R.id.calEv3Title);

        init();
    }

    private void init() {
        if (rootWidget == null) return;

        // Render current month mini calendar
        renderMonthGrid();

        // Render live date
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        if (tvTodayDate != null) {
            tvTodayDate.setText(dateFmt.format(new Date()));
        }

        // Fetch & sync Google Calendar events from TV account
        refreshEvents();

        // Register broadcast receiver for custom calendar URL overrides if needed
        IntentFilter filter = new IntentFilter("com.gathin.tvhome.SET_CALENDAR_URL");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            activity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String url = intent.getStringExtra("url");
                    if (url != null && !url.trim().isEmpty()) {
                        CalendarFetcher.setCalendarUrl(context, url.trim());
                        Toast.makeText(context, "Calendar URL updated! Syncing...", Toast.LENGTH_SHORT).show();
                        refreshEvents();
                    }
                }
            }, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String url = intent.getStringExtra("url");
                    if (url != null && !url.trim().isEmpty()) {
                        CalendarFetcher.setCalendarUrl(context, url.trim());
                        Toast.makeText(context, "Calendar URL updated! Syncing...", Toast.LENGTH_SHORT).show();
                        refreshEvents();
                    }
                }
            }, filter);
        }

        // On TV Remote click on calendar widget
        rootWidget.setOnClickListener(v -> showSyncDialog());
    }

    public void refreshEvents() {
        CalendarFetcher.fetch(activity, (events, accountLabel, isLiveSync) -> {
            if (activity.isFinishing()) return;

            if (tvSyncStatus != null) {
                if (isLiveSync) {
                    tvSyncStatus.setText("● " + accountLabel.toUpperCase());
                    tvSyncStatus.setTextColor(Color.parseColor("#10B981"));
                } else {
                    tvSyncStatus.setText("● LIVE");
                    tvSyncStatus.setTextColor(Color.parseColor("#38BDF8"));
                }
            }

            populateEvents(events);
        });
    }

    private void populateEvents(List<CalendarFetcher.Event> events) {
        if (events == null || events.isEmpty()) {
            setEventRow(tvEv1Time, tvEv1Title, "ALL DAY", "No Upcoming Events");
            setEventRow(tvEv2Time, tvEv2Title, "", "");
            setEventRow(tvEv3Time, tvEv3Title, "", "");
            return;
        }

        // Row 1
        if (events.size() > 0) {
            CalendarFetcher.Event e1 = events.get(0);
            setEventRow(tvEv1Time, tvEv1Title, e1.time, e1.title);
        } else {
            setEventRow(tvEv1Time, tvEv1Title, "", "");
        }

        // Row 2
        if (events.size() > 1) {
            CalendarFetcher.Event e2 = events.get(1);
            setEventRow(tvEv2Time, tvEv2Title, e2.time, e2.title);
        } else {
            setEventRow(tvEv2Time, tvEv2Title, "", "");
        }

        // Row 3
        if (events.size() > 2) {
            CalendarFetcher.Event e3 = events.get(2);
            setEventRow(tvEv3Time, tvEv3Title, e3.time, e3.title);
        } else {
            setEventRow(tvEv3Time, tvEv3Title, "", "");
        }
    }

    private void setEventRow(TextView tvTime, TextView tvTitle, String time, String title) {
        if (tvTime == null || tvTitle == null) return;
        if (title.isEmpty()) {
            tvTime.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);
        } else {
            tvTime.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.VISIBLE);
            tvTime.setText(time);
            tvTitle.setText(title);
        }
    }

    private void renderMonthGrid() {
        if (gridDays == null) return;
        gridDays.removeAllViews();

        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);

        // Header: e.g. "OCTOBER 2026"
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (tvMonthYear != null) {
            tvMonthYear.setText(monthFmt.format(cal.getTime()).toUpperCase());
        }

        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sun, 7 = Sat
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        gridDays.setColumnCount(7);

        // Leading blank cells
        int blankDays = firstDayOfWeek - 1;
        for (int i = 0; i < blankDays; i++) {
            TextView blank = createDayCell("", false);
            gridDays.addView(blank);
        }

        // Days of month
        for (int day = 1; day <= maxDays; day++) {
            boolean isToday = (day == today);
            TextView cell = createDayCell(String.valueOf(day), isToday);
            gridDays.addView(cell);
        }
    }

    private TextView createDayCell(String text, boolean isToday) {
        TextView tv = new TextView(activity);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = dpToPx(17);
        lp.height = dpToPx(16);
        lp.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setText(text);
        tv.setTextSize(7.8f);
        tv.setIncludeFontPadding(false);

        if (isToday) {
            tv.setBackgroundResource(R.drawable.calendar_today_circle);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            tv.setTextColor(Color.parseColor("#94A3B8"));
        }
        return tv;
    }

    private int dpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showSyncDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("📅 Google Calendar Widget")
                .setMessage("Synced directly from your TV's Google Account (Google Play / System Calendar)!\n\nEvents update automatically in the background.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Sync Now", (d, w) -> refreshEvents())
                .show();
    }
}
