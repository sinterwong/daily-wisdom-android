package com.sinter.daily;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class QuoteWidget extends AppWidgetProvider {
    static final String NEXT = "com.sinter.daily.NEXT";
    static final String FAV = "com.sinter.daily.FAV";
    static final String TICK = "com.sinter.daily.TICK";

    static PendingIntent action(Context c, String action, int code) {
        return PendingIntent.getBroadcast(c, code,
                new Intent(c, QuoteWidget.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void refresh(Context c) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        int[] ids = manager.getAppWidgetIds(new ComponentName(c, QuoteWidget.class));
        if (ids.length == 0) return;
        int quoteId = Store.current(c);
        for (int widgetId : ids) {
            RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget);
            String text = Store.quote(c, quoteId).optString("text");
            Bundle size = manager.getAppWidgetOptions(widgetId);
            int height = size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140);
            int font = text.length() > 90 ? 16 : 19;
            float fontScale = c.getResources().getConfiguration().fontScale;
            int lines = Math.max(1, Math.min(16, (int)((height - 80) / (font * fontScale + 4))));
            views.setTextViewText(R.id.quote, text);
            views.setTextViewTextSize(R.id.quote, TypedValue.COMPLEX_UNIT_SP, font);
            views.setInt(R.id.quote, "setMaxLines", lines);
            views.setTextViewText(R.id.day_number, new SimpleDateFormat("dd", Locale.CHINA).format(new Date()));
            views.setTextViewText(R.id.month, new SimpleDateFormat("M月", Locale.CHINA).format(new Date()));
            views.setTextViewText(R.id.source, "《宝贵的人生建议》 · 凯文·凯利" + (Store.held(c) ? " · 停留中" : ""));
            views.setTextViewText(R.id.fav, Store.favorite(c, quoteId) ? "♥ 已收藏" : "♡ 收藏");
            PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.quote, open);
            views.setOnClickPendingIntent(R.id.open, open);
            views.setOnClickPendingIntent(R.id.date_box, open);
            views.setOnClickPendingIntent(R.id.next, action(c, NEXT, 1));
            // Capture the displayed quote: a tap around midnight must favorite what the user saw.
            PendingIntent favorite = PendingIntent.getBroadcast(c, 2,
                    new Intent(c, QuoteWidget.class).setAction(FAV).putExtra("quoteId", quoteId),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.fav, favorite);
            manager.updateAppWidget(widgetId, views);
        }
        schedule(c);
    }

    static void schedule(Context c) {
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 5);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        // Inexact, non-wakeup alarm plus periodic widget updates; no exact-alarm permission.
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).set(
                AlarmManager.RTC, next.getTimeInMillis(), action(c, TICK, 3));
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { refresh(c); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle b) { refresh(c); }

    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (NEXT.equals(action)) {
            Store.next(c);
            refresh(c);
        } else if (FAV.equals(action)) {
            int id = intent.getIntExtra("quoteId", -1);
            if (id >= 0 && id < Store.data(c).length()) Store.toggle(c, id);
            refresh(c);
        } else if (TICK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            refresh(c);
        } else {
            super.onReceive(c, intent);
        }
    }

    @Override public void onDisabled(Context c) {
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(action(c, TICK, 3));
    }
}
