package com.sinter.daily;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
final class Store {
 static JSONArray data;
 static synchronized JSONArray data(Context c){
  if(data==null)try{ByteArrayOutputStream out=new ByteArrayOutputStream();InputStream in=c.getAssets().open("quotes.json");byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();data=new JSONArray(out.toString("UTF-8"));}catch(Exception e){throw new IllegalStateException("无法读取内置书摘",e);}return data;
 }
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("daily",0);}
 static String day(){return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(new Date());}
 static synchronized int current(Context c){android.content.SharedPreferences p=prefs(c);if(Rotation.needsAdvance(p.getInt("current",-1),p.getBoolean("hold",false),p.getString("day",""),day()))return next(c);return p.getInt("current",0);}
 static synchronized int next(Context c){
  android.content.SharedPreferences p=prefs(c);String raw=p.getString("queue","");ArrayList<Integer> queue=new ArrayList<>();
  if(!raw.isEmpty())for(String s:raw.split(","))queue.add(Integer.parseInt(s));
  int id=Rotation.draw(queue,data(c).length(),p.getInt("current",-1),new Random());StringBuilder rest=new StringBuilder();for(int i:queue){if(rest.length()>0)rest.append(',');rest.append(i);}
  p.edit().putInt("current",id).putString("queue",rest.toString()).putString("day",day()).putBoolean("hold",false).commit();return id;
 }
 static JSONObject quote(Context c,int id){return data(c).optJSONObject(id);}
 static Set<String> favorites(Context c){return new HashSet<String>(prefs(c).getStringSet("favorites",Collections.<String>emptySet()));}
 static boolean favorite(Context c,int id){return favorites(c).contains(""+id);}
 static void toggle(Context c,int id){Set<String>s=favorites(c);if(!s.add(""+id))s.remove(""+id);prefs(c).edit().putStringSet("favorites",s).commit();}
 static boolean held(Context c){return prefs(c).getBoolean("hold",false);}
 static void hold(Context c){prefs(c).edit().putBoolean("hold",!held(c)).putString("day",day()).commit();}
 static String source(Context c,int id){JSONObject q=quote(c,id);return "凯文·凯利 · 刘波译  /  PDF 第 "+q.optInt("page")+(q.optInt("endPage")!=q.optInt("page")?"–"+q.optInt("endPage"):"")+" 页";}
}
