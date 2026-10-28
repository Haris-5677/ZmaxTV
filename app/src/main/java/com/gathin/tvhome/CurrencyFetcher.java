package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

public final class CurrencyFetcher {

    private static final String PREFS_NAME = "currency_rates";
    private static final String KEY_USD = "rate_usd_inr";
    private static final String KEY_AED = "rate_aed_inr";
    private static final String KEY_SAR = "rate_sar_inr";
    private static final String KEY_TIME = "rate_last_fetched";

    private static final String DEFAULT_USD = "₹95.17";
    private static final String DEFAULT_AED = "₹25.90";
    private static final String DEFAULT_SAR = "₹25.34";

    // 4 hours minimum between background network fetches to avoid spamming Google Finance
    private static final long FETCH_INTERVAL_MS = 4 * 60 * 60 * 1000L;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static boolean isFetching = false;

    public interface OnRatesUpdatedListener {
        void onRatesUpdated(String usdInr, String aedInr, String sarInr);
    }

    public static String getCachedUsd(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USD, DEFAULT_USD);
    }

    public static String getCachedAed(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AED, DEFAULT_AED);
    }

    public static String getCachedSar(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAR, DEFAULT_SAR);
    }

    public static synchronized void fetch(Context context, OnRatesUpdatedListener listener) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastTime = prefs.getLong(KEY_TIME, 0);
        long now = System.currentTimeMillis();

        // Always notify listener with current cached rates first
        if (listener != null) {
            listener.onRatesUpdated(
                    prefs.getString(KEY_USD, DEFAULT_USD),
                    prefs.getString(KEY_AED, DEFAULT_AED),
                    prefs.getString(KEY_SAR, DEFAULT_SAR)
            );
        }

        // If fetched recently, don't re-fetch immediately
        if (now - lastTime < FETCH_INTERVAL_MS || isFetching) {
            return;
        }

        isFetching = true;
        EXECUTOR.execute(() -> {
            try {
                String usd = fetchQuote("USD-INR");
                String aed = fetchQuote("AED-INR");
                String sar = fetchQuote("SAR-INR");

                SharedPreferences.Editor editor = prefs.edit();
                boolean updated = false;

                if (usd != null) {
                    editor.putString(KEY_USD, usd);
                    updated = true;
                }
                if (aed != null) {
                    editor.putString(KEY_AED, aed);
                    updated = true;
                }
                if (sar != null) {
                    editor.putString(KEY_SAR, sar);
                    updated = true;
                }

                if (updated) {
                    editor.putLong(KEY_TIME, System.currentTimeMillis());
                    editor.apply();

                    String finalUsd = prefs.getString(KEY_USD, DEFAULT_USD);
                    String finalAed = prefs.getString(KEY_AED, DEFAULT_AED);
                    String finalSar = prefs.getString(KEY_SAR, DEFAULT_SAR);

                    if (listener != null) {
                        MAIN_HANDLER.post(() -> listener.onRatesUpdated(finalUsd, finalAed, finalSar));
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                isFetching = false;
            }
        });
    }

    private static String fetchQuote(String pair) {
        HttpsURLConnection conn = null;
        try {
            URL url = new URL("https://www.google.com/finance/quote/" + pair);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            int code = conn.getResponseCode();
            if (code != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }

            String html = sb.toString();
            // Primary extraction: Indian Rupee followed by jsname="Pdsbrc"
            Pattern p1 = Pattern.compile("Indian Rupee.*?<span jsname=\"Pdsbrc\"[^>]*><span>([0-9.,]+)</span>", Pattern.DOTALL);
            Matcher m1 = p1.matcher(html);
            if (m1.find()) {
                return formatRate(m1.group(1));
            }

            // Fallback: N6SYTe container
            Pattern p2 = Pattern.compile("<div class=\"N6SYTe\"><span jsname=\"Pdsbrc\"[^>]*><span>([0-9.,]+)</span>");
            Matcher m2 = p2.matcher(html);
            if (m2.find()) {
                return formatRate(m2.group(1));
            }

            // Fallback: YMlKec fxKbKc
            Pattern p3 = Pattern.compile("class=\"YMlKec fxKbKc\">([0-9.,]+)<");
            Matcher m3 = p3.matcher(html);
            if (m3.find()) {
                return formatRate(m3.group(1));
            }

            // Fallback: data-last-price
            Pattern p4 = Pattern.compile("data-last-price=\"([0-9.,]+)\"");
            Matcher m4 = p4.matcher(html);
            if (m4.find()) {
                return formatRate(m4.group(1));
            }

            return null;
        } catch (Throwable e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String formatRate(String raw) {
        if (raw == null) return null;
        try {
            double val = Double.parseDouble(raw.replace(",", "").trim());
            return String.format(Locale.US, "₹%.2f", val);
        } catch (Throwable e) {
            return "₹" + raw.trim();
        }
    }
}
