package com.sinter.daily;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Searchable, recycled rows keep large imported libraries usable. */
public class LibraryActivity extends Activity {
    private QuoteLibrary library;
    private final List<JSONObject> visible=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView count;
    private String query="";
    private boolean favoritesOnly;
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        favoritesOnly=getIntent().getBooleanExtra("favorites",false);
        if (state!=null) query=state.getString("query","");
        render();
    }
    @Override public void onSaveInstanceState(Bundle out) {
        out.putString("query",query);super.onSaveInstanceState(out);
    }
    private Button button(String title,Runnable action) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(0xff644375);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xffece3ef);bg.setCornerRadius(dp(14));b.setBackground(bg);
        b.setMinHeight(dp(44));b.setPadding(dp(14),dp(8),dp(14),dp(8));
        b.setOnClickListener(v->action.run());return b;
    }
    private void render() {
        library=Libraries.active(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(0xfff3eee6);
        setContentView(root);
        if (Build.VERSION.SDK_INT>=35) root.setOnApplyWindowInsetsListener((v,in)->{
            android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(dp(16)+bars.left,dp(12)+bars.top,dp(16)+bars.right,dp(12)+bars.bottom);return in;
        });
        LinearLayout toolbar=new LinearLayout(this);
        toolbar.addView(button("返回",this::finish));
        toolbar.setPadding(0,0,0,dp(12));
        Space gap=new Space(this);toolbar.addView(gap,new LinearLayout.LayoutParams(dp(8),1));
        toolbar.addView(button("切换句库",this::choose));
        root.addView(toolbar);
        TextView title=new TextView(this);title.setText(favoritesOnly?"我的收藏":library.title);title.setTextSize(23);title.setTextColor(0xff302b32);root.addView(title);
        TextView subtitle=new TextView(this);subtitle.setText(favoritesOnly?library.title+" · 留住触动你的内容":"点击条目查看全文与编辑");subtitle.setTextColor(0xff807584);root.addView(subtitle);
        if (!favoritesOnly) root.addView(button("＋ 新增条目",()->edit(null)));
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("搜索正文、作者或来源");
        root.addView(search);count=new TextView(this);root.addView(count);
        ListView list=new ListView(this);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>()) {
            @Override public View getView(int position,View convert,ViewGroup parent) {
                LinearLayout outer=new LinearLayout(LibraryActivity.this);outer.setPadding(0,dp(6),0,dp(6));
                LinearLayout card=new LinearLayout(LibraryActivity.this);card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(18),dp(16),dp(18),dp(12));
                android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
                bg.setColor(0xfffff8ec);bg.setCornerRadius(dp(18));card.setBackground(bg);
                outer.addView(card,new LinearLayout.LayoutParams(-1,-2));
                JSONObject q=visible.get(position);
                TextView quote=new TextView(LibraryActivity.this);quote.setText(q.optString("text"));quote.setTextSize(19);
                quote.setTextColor(0xff302b32);quote.setLineSpacing(dp(5),1);quote.setMaxLines(4);quote.setEllipsize(TextUtils.TruncateAt.END);card.addView(quote);
                LinearLayout foot=new LinearLayout(LibraryActivity.this);foot.setGravity(Gravity.CENTER_VERTICAL);card.addView(foot);
                TextView source=new TextView(LibraryActivity.this);
                source.setText(q.optString("author",library.json.optString("author",library.title))+"  ·  查看全文");
                source.setTextColor(0xff817187);source.setTextSize(12);source.setMaxLines(2);
                foot.addView(source,new LinearLayout.LayoutParams(0,-2,1));
                boolean saved=Store.favoriteIds(LibraryActivity.this).contains(q.optString("id"));
                Button favorite=button(saved?"♥ 已收藏":"♡ 收藏",()->{
                    for (int i=0;i<library.items.length();i++) if (library.items.optJSONObject(i).optString("id").equals(q.optString("id"))) {
                        Store.toggle(LibraryActivity.this,i);break;
                    }
                    QuoteWidget.refresh(LibraryActivity.this);filter();
                });
                favorite.setTextSize(12);foot.addView(favorite);
                card.setOnClickListener(v->details(q));
                return outer;
            }
        };
        list.setDivider(null);list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        list.setOnItemClickListener((p,v,position,id)->details(visible.get(position)));
        search.setText(query);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int n,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int n) { query=s.toString();filter(); }
            public void afterTextChanged(Editable e) { }
        });
        filter();
    }
    private void filter() {
        visible.clear();List<String> labels=new ArrayList<>();
        String needle=query.trim().toLowerCase(Locale.ROOT);
        Set<String> saved=Store.favoriteIds(this);
        for (int i=0;i<library.items.length();i++) {
            JSONObject q=library.items.optJSONObject(i);
            if (favoritesOnly && !saved.contains(q.optString("id"))) continue;
            String haystack=q.optString("text")+" "+q.optString("author",library.json.optString("author"))+" "+q.optString("source",library.json.optString("source"));
            if (haystack.toLowerCase(Locale.ROOT).contains(needle)) {
                visible.add(q);labels.add((i+1)+". "+q.optString("text"));
            }
        }
        adapter.clear();adapter.addAll(labels);
        count.setText(favoritesOnly?(saved.isEmpty()?"还没有收藏。遇到喜欢的内容，点一下 ♡。":"显示 "+visible.size()+" / "+saved.size()+" 条收藏"):(library.items.length()==0?"暂无内容，点击「新增条目」开始。":"显示 "+visible.size()+" / "+library.items.length()+" 条"));
    }
    private void choose() {
        List<QuoteLibrary> all=Libraries.all(this);String[] labels=new String[all.size()];
        for (int i=0;i<all.size();i++) labels[i]=all.get(i).title+" · "+all.get(i).items.length()+" 条";
        new AlertDialog.Builder(this).setTitle("选择句库（同时切换桌面内容）").setItems(labels,(d,i)->{
            Libraries.select(this,all.get(i).id);QuoteWidget.refresh(this);query="";render();
        }).setNegativeButton("取消",null).show();
    }
    private void details(JSONObject q) {
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(12),dp(20),dp(12));
        TextView body=new TextView(this);body.setText(q.optString("text"));body.setTextSize(19);body.setTextIsSelectable(true);content.addView(body);
        String meta="\n作者："+q.optString("author",library.json.optString("author","未填写"))
                +"\n来源："+q.optString("source",library.json.optString("source","未填写"));
        if (library.json.has("translator")) meta+="\n译者："+library.json.optString("translator");
        if (q.has("page")) meta+="\n页码："+q.optInt("page")+(q.has("endPage")?"–"+q.optInt("endPage"):"");
        meta+="\n条目 ID："+q.optString("id");
        TextView info=new TextView(this);info.setText(meta);info.setTextIsSelectable(true);content.addView(info);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("条目详情").setView(scroll).setNegativeButton("关闭",null);
        {
            dialog.setPositiveButton("编辑",(d,w)->edit(q));
            dialog.setNeutralButton("删除",(d,w)->new AlertDialog.Builder(this).setTitle("删除这条内容？")
                    .setMessage("该条目及其收藏将从当前句库删除。其他内容不受影响。")
                    .setNegativeButton("取消",null).setPositiveButton("删除",(confirm,which)->persist(q.optString("id"),null)).show());
        }
        dialog.show();
    }
    private EditText field(LinearLayout form,String label,String value,boolean number,int lines) {
        TextView caption=new TextView(this);caption.setText(label);form.addView(caption);
        EditText input=new EditText(this);input.setText(value);input.setMinLines(lines);
        input.setInputType(number?android.text.InputType.TYPE_CLASS_NUMBER:
                android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|
                (lines>1?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        form.addView(input);return input;
    }
    private void edit(JSONObject original) {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(8),dp(20),dp(8));
        JSONObject q=original==null?new JSONObject():original;
        EditText text=field(form,"正文（必填，最多 4000 字）",q.optString("text"),false,5);
        EditText author=field(form,"作者（留空沿用句库作者）",q.optString("author"),false,1);
        EditText source=field(form,"来源（留空沿用句库来源）",q.optString("source"),false,1);
        EditText page=field(form,"起始页码（可选）",q.has("page")?q.optString("page"):"",true,1);
        EditText end=field(form,"结束页码（可选）",q.has("endPage")?q.optString("endPage"):"",true,1);
        TextView error=new TextView(this);error.setTextColor(Color.RED);form.addView(error);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(original==null?"新增条目":"编辑条目").setView(scroll)
                .setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                JSONObject replacement=new JSONObject().put("text",text.getText().toString());
                if (!author.getText().toString().trim().isEmpty()) replacement.put("author",author.getText().toString());
                else if (original!=null && original.has("author") && original.optString("author").isEmpty()) replacement.put("author","");
                if (!source.getText().toString().trim().isEmpty()) replacement.put("source",source.getText().toString());
                else if (original!=null && original.has("source") && original.optString("source").isEmpty()) replacement.put("source","");
                if (!page.getText().toString().isEmpty()) replacement.put("page",Integer.parseInt(page.getText().toString()));
                if (!end.getText().toString().isEmpty()) replacement.put("endPage",Integer.parseInt(end.getText().toString()));
                QuoteLibrary changed=LibraryEdits.change(library,original==null?null:original.optString("id"),replacement);
                Libraries.saveEdited(this,library,changed);dialog.dismiss();render();
                Toast.makeText(this,"已保存，桌面内容已同步",Toast.LENGTH_SHORT).show();
            } catch (Exception e) { error.setText(e instanceof NumberFormatException?"页码应为 1～1000000 的整数。":e.getMessage()); }
        }));
        dialog.show();
    }
    private void persist(String id,JSONObject replacement) {
        try {
            Libraries.saveEdited(this,library,LibraryEdits.change(library,id,replacement));render();
            Toast.makeText(this,"已删除",Toast.LENGTH_SHORT).show();
        } catch (Exception e) { error(e); }
    }
    private void error(Exception e) {
        new AlertDialog.Builder(this).setTitle("未能保存").setMessage(e.getMessage()).setPositiveButton("关闭",null).show();
    }
}
