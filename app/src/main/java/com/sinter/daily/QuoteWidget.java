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
    static final String PINNED = "com.sinter.daily.PINNED";
    static final String NEXT = "com.sinter.daily.NEXT";
    static final String FAV = "com.sinter.daily.FAV";
    static final String TICK = "com.sinter.daily.TICK";
    static final String THEME = "com.sinter.daily.THEME";

    static int fontSize(Context c) {
        return Math.max(14,Math.min(24,c.getSharedPreferences("widget-style",0).getInt("fontSize",20)));
    }
    static void setFontSize(Context c,int value) {
        c.getSharedPreferences("widget-style",0).edit().putInt("fontSize",Math.max(14,Math.min(24,value))).commit();
        refresh(c);
    }

    static int theme(Context c) {
        return Math.max(0,Math.min(2,c.getSharedPreferences("widget-style",0).getInt("theme",0)));
    }
    static void setTheme(Context c,int value) {
        c.getSharedPreferences("widget-style",0).edit().putInt("theme",Math.max(0,Math.min(2,value))).commit();
        refresh(c);
    }
    static String themeName(Context c) {
        return new String[]{"黑色","浅色","透明"}[theme(c)];
    }
    private static void applyTheme(Context c,RemoteViews views) {
        int theme=theme(c);
        boolean darkText=theme==1 || theme==2;
        views.setInt(R.id.card,"setBackgroundResource",theme==0?R.drawable.card:theme==1?R.drawable.card_light:R.drawable.card_transparent);
        views.setInt(R.id.date_box,"setBackgroundResource",darkText?R.drawable.date_frame_light:R.drawable.date_frame);
        int primary=darkText?0xff25232a:0xfffff9f0;
        int secondary=darkText?0xff655c6e:0xffd5cddb;
        for (int id:new int[]{R.id.quote,R.id.day_number}) views.setTextColor(id,primary);
        for (int id:new int[]{R.id.month,R.id.source}) views.setTextColor(id,secondary);
        for (int id:new int[]{R.id.open,R.id.fav,R.id.next,R.id.theme}) views.setTextColor(id,darkText?0xff644375:0xffd6c2e6);
    }

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
            manager.updateAppWidget(widgetId, buildForOptions(c, quoteId, manager.getAppWidgetOptions(widgetId)));
        }
        schedule(c);
    }

    static RemoteViews buildForOptions(Context c,int quoteId,Bundle options) {
        if (android.os.Build.VERSION.SDK_INT>=31) {
            java.util.ArrayList<android.util.SizeF> sizes=options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (sizes!=null && !sizes.isEmpty()) {
                java.util.Map<android.util.SizeF,RemoteViews> layouts=new java.util.LinkedHashMap<>();
                for (android.util.SizeF size:sizes) {
                    if (size.getWidth()<=0 || size.getHeight()<=0) continue;
                    Bundle exact=new Bundle();
                    exact.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,(int)size.getWidth());
                    exact.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,(int)size.getHeight());
                    layouts.put(size,buildViews(c,quoteId,exact));
                    if (layouts.size()==16) break;
                }
                if (!layouts.isEmpty()) return new RemoteViews(layouts);
            }
        }
        // Legacy ranges describe two orientations, not one minimum-width/minimum-height rectangle.
        Bundle portrait=new Bundle(options),landscape=new Bundle(options);
        portrait.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,140)));
        landscape.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,180)));
        return new RemoteViews(buildViews(c,quoteId,landscape),buildViews(c,quoteId,portrait));
    }

    static RemoteViews buildViews(Context c, int quoteId, Bundle size) {
            RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget);
            String text = quoteId<0?"句库暂无内容，点开应用添加第一条。":Store.quote(c, quoteId).optString("text");
            int height = size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140);
            int width = size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
            float fontScale = c.getResources().getConfiguration().fontScale;
            boolean actions=width>=280 && height>=140;
            boolean source=height>=110 && fontScale<=1.5f;
            boolean date=width>=300 && height>=180 && fontScale<=1.5f;
            views.setViewVisibility(R.id.actions,actions?android.view.View.VISIBLE:android.view.View.GONE);
            views.setViewVisibility(R.id.source,source?android.view.View.VISIBLE:android.view.View.GONE);
            views.setViewVisibility(R.id.date_box,date?android.view.View.VISIBLE:android.view.View.GONE);
            views.setTextViewText(R.id.quote, text);
            views.setTextViewTextSize(R.id.quote, TypedValue.COMPLEX_UNIT_SP, fontSize(c));
            views.setTextViewText(R.id.day_number, new SimpleDateFormat("dd", Locale.CHINA).format(new Date()));
            views.setTextViewText(R.id.month, new SimpleDateFormat("M月", Locale.CHINA).format(new Date()));
            views.setTextViewText(R.id.source, Libraries.active(c).title + (Store.source(c, quoteId).isEmpty() ? "" : " · " + Store.source(c, quoteId)) + (Store.held(c) ? " · 停留中" : ""));
            views.setTextViewText(R.id.fav, Store.favorite(c, quoteId) ? "♥ 已收藏" : "♡ 收藏");
            applyTheme(c,views);
            fitText(c,views,width,height);
            PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.quote, open);
            views.setOnClickPendingIntent(R.id.open, open);
            views.setOnClickPendingIntent(R.id.date_box, open);
            views.setOnClickPendingIntent(R.id.next, action(c, NEXT, 1));
            views.setOnClickPendingIntent(R.id.theme, action(c, THEME, 5));
            // Capture the displayed quote: a tap around midnight must favorite what the user saw.
            PendingIntent favorite = PendingIntent.getBroadcast(c, 2,
                    new Intent(c, QuoteWidget.class).setAction(FAV).putExtra("libraryId", Libraries.active(c).id).putExtra("quoteKey", Store.quote(c, quoteId).optString("id")),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.fav, favorite);
            return views;
    }

    /** Measure Android's actual wrapped lines, including fallback fonts and system font scaling. */
    private static void fitText(Context c,RemoteViews views,int widthDp,int heightDp) {
        android.view.View preview=views.apply(c,new android.widget.FrameLayout(c));
        android.widget.TextView quote=preview.findViewById(R.id.quote);
        quote.setMaxLines(Integer.MAX_VALUE);
        quote.setEllipsize(null);
        float density=c.getResources().getDisplayMetrics().density;
        int width=Math.max(1,Math.round(widthDp*density)),height=Math.max(1,Math.round(heightDp*density));
        float font=fontSize(c);
        int lines=1;
        for (int attempt=0;attempt<6;attempt++) {
            quote.setMaxLines(Integer.MAX_VALUE);quote.setEllipsize(null);
            quote.setTextSize(TypedValue.COMPLEX_UNIT_SP,font);
            measurePreview(preview,width,height);
            android.text.Layout layout=quote.getLayout();
            int available=quote.getHeight()-quote.getCompoundPaddingTop()-quote.getCompoundPaddingBottom();
            lines=0;
            for (int i=0;i<layout.getLineCount();i++) {
                if (layout.getLineBottom(i)>available) break;
                lines++;
            }
            if (lines>0) {
                // Ellipsizing changes final-line font padding. Check the actual final layout too.
                quote.setEllipsize(android.text.TextUtils.TruncateAt.END);
                while (lines>0) {
                    quote.setMaxLines(lines);measurePreview(preview,width,height);
                    layout=quote.getLayout();
                    int last=Math.min(lines,layout.getLineCount())-1;
                    if (layout.getLineBottom(last)<=available) break;
                    if (lines==1) { lines=0;break; }
                    lines--;
                }
            }
            if (lines>0 || font<=1) break;
            font=Math.max(1,font*Math.max(1,available)/Math.max(1,layout.getLineBottom(0))*.95f);
        }
        views.setTextViewTextSize(R.id.quote,TypedValue.COMPLEX_UNIT_SP,font);
        views.setInt(R.id.quote,"setMaxLines",Math.max(1,lines));
    }

    private static void measurePreview(android.view.View preview,int width,int height) {
        preview.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));
        preview.layout(0,0,width,height);
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
        if (PINNED.equals(action)) {
            refresh(c);
            android.widget.Toast.makeText(c,"桌面小组件已添加",android.widget.Toast.LENGTH_LONG).show();
        } else if (NEXT.equals(action)) {
            Store.next(c);
            refresh(c);
        } else if (THEME.equals(action)) {
            setTheme(c,(theme(c)+1)%3);
            android.widget.Toast.makeText(c,"组件主题："+themeName(c),android.widget.Toast.LENGTH_SHORT).show();
        } else if (FAV.equals(action)) {
            if (Libraries.active(c).id.equals(intent.getStringExtra("libraryId"))) {
                String key = intent.getStringExtra("quoteKey");
                for (int i=0;i<Store.data(c).length();i++) {
                    if (Store.quote(c,i).optString("id").equals(key)) { Store.toggle(c,i); break; }
                }
            }
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
