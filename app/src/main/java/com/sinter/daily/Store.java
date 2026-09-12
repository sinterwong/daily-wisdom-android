package com.sinter.daily;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

final class Store {
    static JSONArray data(Context c) { return Libraries.active(c).items; }
    static SharedPreferences prefsFor(Context c,String libraryId) {
        return c.getSharedPreferences("state-"+QuoteLibrary.digest(libraryId),0);
    }
    static SharedPreferences prefs(Context c) { return prefsFor(c,Libraries.active(c).id); }
    static String day() { return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(new Date()); }
    static synchronized int current(Context c) {
        if (data(c).length()==0) return -1;
        SharedPreferences p = prefs(c);
        int id = p.getInt("current",-1);
        if (id >= data(c).length() || Rotation.needsAdvance(id,p.getBoolean("hold",false),p.getString("day",""),day())) return next(c);
        return id;
    }
    static synchronized int next(Context c) {
        if (data(c).length()==0) return -1;
        SharedPreferences p = prefs(c);
        ArrayList<Integer> queue = new ArrayList<>();
        String raw = p.getString("queue","");
        if (!raw.isEmpty()) for (String s : raw.split(",")) queue.add(Integer.parseInt(s));
        int id = Rotation.draw(queue,data(c).length(),p.getInt("current",-1),new Random());
        StringBuilder remaining = new StringBuilder();
        for (int i : queue) { if (remaining.length()>0) remaining.append(','); remaining.append(i); }
        p.edit().putInt("current",id).putString("queue",remaining.toString()).putString("day",day()).putBoolean("hold",false).commit();
        return id;
    }
    static JSONObject quote(Context c,int id) { JSONObject q=data(c).optJSONObject(id); return q==null?new JSONObject():q; }
    static Set<String> favoriteIds(Context c) {
        return new HashSet<>(prefs(c).getStringSet("favoriteIds",Collections.<String>emptySet()));
    }
    static List<Integer> favorites(Context c) {
        Set<String> ids = favoriteIds(c); List<Integer> result = new ArrayList<>();
        for (int i=0;i<data(c).length();i++) if (ids.contains(quote(c,i).optString("id"))) result.add(i);
        return result;
    }
    static boolean favorite(Context c,int id) { return favoriteIds(c).contains(quote(c,id).optString("id")); }
    static void toggle(Context c,int id) {
        Set<String> ids = favoriteIds(c);String key=quote(c,id).optString("id");
        if (key.isEmpty()) return;
        if (!ids.add(key)) ids.remove(key);
        prefs(c).edit().putStringSet("favoriteIds",ids).commit();
    }
    static boolean held(Context c) { return prefs(c).getBoolean("hold",false); }
    static void hold(Context c) { if (data(c).length()==0) return; prefs(c).edit().putBoolean("hold",!held(c)).putString("day",day()).commit(); }
    static String source(Context c,int id) {
        QuoteLibrary library = Libraries.active(c); JSONObject q=quote(c,id);
        String author=q.optString("author",library.json.optString("author",""));
        String source=q.optString("source",library.json.optString("source",""));
        ArrayList<String> pieces=new ArrayList<>();
        if (!author.isEmpty()) pieces.add(author);
        String translator=library.json.optString("translator","");
        if (!translator.isEmpty()) pieces.add(translator+"译");
        if (!source.isEmpty()) pieces.add(source);
        if (q.has("page")) pieces.add("PDF 第 "+q.optInt("page")+(q.has("endPage")&&q.optInt("endPage")!=q.optInt("page")?"–"+q.optInt("endPage"):"")+" 页");
        return String.join(" · ",pieces);
    }
}
