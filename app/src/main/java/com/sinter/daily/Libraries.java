package com.sinter.daily;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Internal JSON files; every library has its own reading state. */
final class Libraries {
    static final String BUILTIN = "builtin-advice";
    private static QuoteLibrary cached;

    static String read(InputStream input) throws IOException {
        if (input == null) throw new IOException("无法打开所选文件");
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > QuoteLibrary.MAX_BYTES) throw new IOException("文件不能超过 5 MB");
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private static File directory(Context c) {
        File dir = new File(c.getFilesDir(), "libraries");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("无法创建句库目录");
        return dir;
    }
    private static File file(Context c, String id) {
        // File name never comes directly from imported content.
        return new File(directory(c), QuoteLibrary.digest(id) + ".json");
    }
    static QuoteLibrary builtin(Context c) {
        try {
            JSONObject root = new JSONObject().put("schemaVersion",1).put("id",BUILTIN)
                    .put("title","宝贵的人生建议").put("author","凯文·凯利").put("translator","刘波")
                    .put("items",new JSONArray(read(c.getAssets().open("quotes.json"))));
            return QuoteLibrary.parse(root.toString());
        } catch (Exception e) { throw new IllegalStateException("无法读取内置示例",e); }
    }
    static synchronized QuoteLibrary active(Context c) {
        String id = c.getSharedPreferences("libraries",0).getString("active",BUILTIN);
        if (cached != null && cached.id.equals(id)) return cached;
        try {
            cached = BUILTIN.equals(id) && !file(c,id).exists() ? builtin(c) : QuoteLibrary.parse(read(new AtomicFile(file(c,id)).openRead()));
        } catch (Exception e) {
            c.getSharedPreferences("libraries",0).edit().putString("active",BUILTIN).commit();
            cached = builtin(c);
        }
        return cached;
    }
    static synchronized void select(Context c, String id) {
        c.getSharedPreferences("libraries",0).edit().putString("active",id).commit();
        cached = null;
    }
    static boolean exists(Context c, String id) { return BUILTIN.equals(id) || file(c,id).exists(); }
    static synchronized void save(Context c, QuoteLibrary library) throws IOException {
        AtomicFile file = new AtomicFile(file(c,library.id));
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(library.json.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(output);
        } catch (IOException e) { if (output != null) file.failWrite(output); throw e; }
        // IDs retain favorites on a replacement; sequence positions are rebuilt after content changes.
        android.content.SharedPreferences p = Store.prefsFor(c,library.id);
        Set<String> kept = new HashSet<>(p.getStringSet("favoriteIds",Collections.<String>emptySet()));
        Set<String> valid = new HashSet<>();
        for (int i=0;i<library.items.length();i++) valid.add(library.items.optJSONObject(i).optString("id"));
        kept.retainAll(valid);
        p.edit().clear().putStringSet("favoriteIds",kept).commit();
        select(c,library.id);
    }
    static synchronized void saveEdited(Context c, QuoteLibrary previous, QuoteLibrary changed) throws IOException {
        if (!previous.id.equals(changed.id)) throw new IOException("句库 id 不可更改");
        android.content.SharedPreferences p=Store.prefsFor(c,previous.id);
        int current=p.getInt("current",-1);
        String currentKey=current>=0&&current<previous.items.length()?previous.items.optJSONObject(current).optString("id"):"";
        Map<String,Integer> positions=new HashMap<>();
        Set<String> oldKeys=new HashSet<>();
        for (int i=0;i<changed.items.length();i++) positions.put(changed.items.optJSONObject(i).optString("id"),i);
        for (int i=0;i<previous.items.length();i++) oldKeys.add(previous.items.optJSONObject(i).optString("id"));
        List<Integer> queue=new ArrayList<>();
        for (String value:p.getString("queue","").split(",")) try {
            int old=Integer.parseInt(value);
            if (old>=0 && old<previous.items.length()) {
                Integer mapped=positions.get(previous.items.optJSONObject(old).optString("id"));
                if (mapped!=null && !queue.contains(mapped)) queue.add(mapped);
            }
        } catch (NumberFormatException ignored) { }
        for (int i=0;i<changed.items.length();i++)
            if (!oldKeys.contains(changed.items.optJSONObject(i).optString("id"))) queue.add(i);
        String day=p.getString("day","");
        boolean hold=p.getBoolean("hold",false);
        Integer mapped=positions.get(currentKey);
        StringBuilder remaining=new StringBuilder();
        for (int index:queue) { if (remaining.length()>0) remaining.append(','); remaining.append(index); }
        save(c,changed);
        p.edit().putInt("current",mapped==null?-1:mapped).putString("queue",remaining.toString())
                .putString("day",day).putBoolean("hold",mapped!=null&&hold).commit();
        QuoteWidget.refresh(c);
    }
    static String exportJson(Context c) {
        try {
            JSONObject copy = new JSONObject(active(c).json.toString());
            if (BUILTIN.equals(copy.optString("id"))) copy.put("id","advice-copy");
            return copy.toString(2);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static List<QuoteLibrary> all(Context c) {
        List<QuoteLibrary> result = new ArrayList<>();
        try { result.add(file(c,BUILTIN).exists()?QuoteLibrary.parse(read(new AtomicFile(file(c,BUILTIN)).openRead())):builtin(c)); }
        catch (Exception e) { result.add(builtin(c)); }
        File[] files = directory(c).listFiles((dir,name)->name.endsWith(".json"));
        if (files != null) {
            Arrays.sort(files,Comparator.comparing(File::getName));
            for (File file : files) try {
                QuoteLibrary entry=QuoteLibrary.parse(read(new AtomicFile(file).openRead()));
                if (!BUILTIN.equals(entry.id)) result.add(entry);
            } catch (Exception ignored) { /* Ignore damaged entries without overwriting them. */ }
        }
        return result;
    }
}
