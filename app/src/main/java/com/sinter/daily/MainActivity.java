package com.sinter.daily;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int IMPORT = 10, EXPORT = 11;
    private final int ink=Color.rgb(48,43,50), purple=Color.rgb(100,67,117), muted=Color.rgb(138,128,119), cream=Color.rgb(255,248,236);
    private int current;
    private String currentLibrary="";
    private LinearLayout body;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable() {
        public void run() {
            if (!Libraries.active(MainActivity.this).id.equals(currentLibrary) || Store.current(MainActivity.this)!=current) render();
            handler.postDelayed(this,30000);
        }
    };

    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(5),1);return t;
    }
    private GradientDrawable background(int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private void gap(LinearLayout parent,int height) { parent.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(height))); }
    private Button button(String label,Runnable action) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(purple);b.setTextSize(14);
        b.setBackground(background(0xffece3ef,14));b.setMinHeight(dp(48));b.setPadding(dp(8),dp(8),dp(8),dp(8));
        b.setOnClickListener(v->action.run());return b;
    }
    private void row(LinearLayout parent,Button... buttons) {
        LinearLayout row=new LinearLayout(this);
        for (Button b:buttons) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);
        }
        parent.addView(row);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(0xfff3eee6);getWindow().setNavigationBarColor(0xfff3eee6);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    @Override public void onResume() { super.onResume();render();handler.postDelayed(tick,30000); }
    @Override public void onPause() { handler.removeCallbacks(tick);super.onPause(); }

    private void render() {
        QuoteLibrary library=Libraries.active(this);currentLibrary=library.id;current=Store.current(this);QuoteWidget.refresh(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff3eee6);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(24),dp(24),dp(32));
        scroll.addView(body);setContentView(scroll);
        if (Build.VERSION.SDK_INT>=35) scroll.setOnApplyWindowInsetsListener((v,in)->{
            android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return in;
        });
        TextView label=text("DAILY WISDOM  /  一日一句",12,purple);label.setLetterSpacing(.12f);body.addView(label);gap(body,12);
        TextView title=text("让一句话，陪你过一天。",26,ink);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);body.addView(title);gap(body,8);
        body.addView(text(library.title+" · "+library.items.length()+" 条",13,muted));gap(body,24);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24),dp(24),dp(24),dp(24));card.setBackground(background(cream,24));body.addView(card);
        card.addView(text(Store.day().replace('-','.')+"  /  "+(Store.held(this)?"这一句，继续陪你":"今天的偶遇"),12,purple));gap(card,20);
        String content=Store.quote(this,current).optString("text");
        TextView quote=text(content,content.length()>300?19:24,ink);quote.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        quote.setTextIsSelectable(true);quote.setLineSpacing(dp(10),1.15f);card.addView(quote);gap(card,24);
        card.addView(text(library.title,13,purple));
        String source=Store.source(this,current);if (!source.isEmpty()) card.addView(text(source,11,muted));
        gap(card,12);card.addView(text(String.format(Locale.CHINA,"%03d / %d",current+1,library.items.length()),11,muted));gap(body,20);
        row(body,button("换一句",()->{Store.next(this);render();}),
                button(Store.favorite(this,current)?"♥ 已收藏":"♡ 收藏",()->{Store.toggle(this,current);render();}),
                button(Store.held(this)?"恢复更新":"让它停留",()->{Store.hold(this);render();}));gap(body,12);
        body.addView(button("＋ 放到手机桌面",this::pin));gap(body,8);
        body.addView(button("从系统小组件添加",this::widgetHelp));gap(body,12);
        row(body,button("我的收藏 · "+Store.favorites(this).size(),this::favorites),button("切换句库",this::chooseLibrary));gap(body,12);
        row(body,button("导入 JSON",this::importFile),button("导出当前句库",this::exportFile));gap(body,22);
        body.addView(text(Store.held(this)?"已暂停每日换句。点击「恢复更新」继续；「换一句」也会结束停留。":"每天换一条，同一天保持不变。读完当前句库一轮再重复；每个句库分别保存收藏和阅读进度。",13,muted));gap(body,16);
        TextView help=text("使用说明 ↗",13,purple);help.setPadding(0,dp(10),0,dp(10));help.setOnClickListener(v->help());body.addView(help);
    }
    private void pin() {
        // Keep guidance visible underneath the launcher confirmation, including silent rejection.
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("添加桌面小组件")
                .setMessage("点击「请求添加」，然后在桌面弹出的窗口中确认。\n\n如果没有弹窗，或取消后想重试，可使用「手动添加」。")
                .setPositiveButton("请求添加",null)
                .setNeutralButton("手动添加",(d,w)->widgetHelp())
                .setNegativeButton("关闭",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            AppWidgetManager manager=AppWidgetManager.getInstance(this);
            try {
                if (!manager.isRequestPinAppWidgetSupported()) {
                    dialog.setMessage("当前桌面不支持应用内添加。请点击「手动添加」，从系统小组件列表添加「一日一句」。");
                    return;
                }
                boolean accepted=manager.requestPinAppWidget(new ComponentName(this,QuoteWidget.class),null,
                        QuoteWidget.action(this,QuoteWidget.PINNED,4));
                dialog.setMessage(accepted
                        ? "已发送添加请求，请在桌面弹窗中确认。请求发出不代表已经添加成功。\n\n如果没有弹窗，请点击「手动添加」。"
                        : "桌面未接受添加请求。请点击「手动添加」，从系统小组件列表添加。");
            } catch (IllegalStateException | SecurityException | IllegalArgumentException e) {
                dialog.setMessage("当前桌面无法完成应用内添加。请点击「手动添加」，从系统小组件列表添加。");
            }
        }));
        dialog.show();
    }
    private void widgetHelp() {
        new AlertDialog.Builder(this).setTitle("从系统小组件添加")
                .setMessage("1. 回到手机桌面，长按空白处。\n2. 选择「小组件」「添加小部件」或「窗口小工具」。\n3. 找到「一日一句」，长按卡片并拖到桌面。\n\n推荐 4×2，可长按卡片调整大小。部分桌面需进入「全部」或「Android 小部件」才能看到第三方组件。\n\n如果列表中暂时没有它，请先打开本应用一次，再返回桌面重试；确认应用安装在当前用户空间中。")
                .setPositiveButton("回到桌面",(d,w)->{
                    try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)); }
                    catch (ActivityNotFoundException e) { message("回到桌面","请使用手机的 Home 手势或主页键返回桌面。"); }
                }).setNegativeButton("关闭",null).show();
    }
    private void favorites() {
        List<Integer> ids=Store.favorites(this);
        if (ids.isEmpty()) { message("收藏夹还是空的","遇到触动你的内容，点「♡ 收藏」，就会留在当前句库的收藏夹里。");return; }
        String[] labels=new String[ids.size()];
        for (int i=0;i<ids.size();i++) labels[i]=String.format(Locale.CHINA,"%03d  ",ids.get(i)+1)+Store.quote(this,ids.get(i)).optString("text");
        new AlertDialog.Builder(this).setTitle("我想记住的话").setItems(labels,(d,pos)->{
            int id=ids.get(pos);
            new AlertDialog.Builder(this).setTitle(Libraries.active(this).title)
                    .setMessage(Store.quote(this,id).optString("text")+"\n\n"+Store.source(this,id))
                    .setPositiveButton("关闭",null).setNeutralButton("取消收藏",(dialog,w)->{Store.toggle(this,id);render();}).show();
        }).setNegativeButton("关闭",null).show();
    }
    private void chooseLibrary() {
        List<QuoteLibrary> libraries=Libraries.all(this);
        String[] labels=new String[libraries.size()];int selected=0;
        for (int i=0;i<labels.length;i++) {
            QuoteLibrary lib=libraries.get(i);labels[i]=lib.title+" · "+lib.items.length()+" 条";
            if (lib.id.equals(currentLibrary)) selected=i;
        }
        new AlertDialog.Builder(this).setTitle("选择桌面显示的句库").setSingleChoiceItems(labels,selected,(d,i)->{
            Libraries.select(this,libraries.get(i).id);d.dismiss();render();
        }).setNegativeButton("关闭",null).show();
    }
    private void importFile() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try { startActivityForResult(intent,IMPORT); } catch (ActivityNotFoundException e) { message("无法选择文件","设备没有可用的系统文件选择器。"); }
    }
    private void exportFile() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                .putExtra(Intent.EXTRA_TITLE,Libraries.active(this).id+".json");
        try { startActivityForResult(intent,EXPORT); } catch (ActivityNotFoundException e) { message("无法保存文件","设备没有可用的系统文件选择器。"); }
    }
    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);
        if (result!=RESULT_OK || intent==null || intent.getData()==null) return;
        Uri uri=intent.getData();
        if (request==IMPORT) {
            Toast.makeText(this,"正在检查句库…",Toast.LENGTH_SHORT).show();
            new Thread(()->{
                try {
                    QuoteLibrary library=QuoteLibrary.parse(Libraries.read(getContentResolver().openInputStream(uri)));
                    if (Libraries.BUILTIN.equals(library.id)) throw new IllegalArgumentException("该 id 属于内置示例，请修改句库 id 后导入。");
                    runOnUiThread(()->{ if (!isFinishing()&&!isDestroyed()) previewImport(library); });
                } catch (Exception e) { runOnUiThread(()->{ if (!isFinishing()&&!isDestroyed()) message("导入失败",e.getMessage()==null?"文件无法读取或格式不正确。":e.getMessage()); }); }
            },"quote-import").start();
        } else if (request==EXPORT) {
            String json=Libraries.exportJson(this);
            new Thread(()->{
                try (OutputStream output=getContentResolver().openOutputStream(uri,"wt")) {
                    if (output==null) throw new java.io.IOException("无法写入所选位置");
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                    runOnUiThread(()->Toast.makeText(this,"句库已导出（不含收藏和阅读进度）",Toast.LENGTH_LONG).show());
                } catch (Exception e) { runOnUiThread(()->{ if (!isFinishing()&&!isDestroyed()) message("导出失败","无法保存到所选位置，请重试。"); }); }
            },"quote-export").start();
        }
    }
    private void previewImport(QuoteLibrary library) {
        boolean replacing=Libraries.exists(this,library.id);
        String sample=library.items.optJSONObject(0).optString("text");
        if (sample.length()>180) sample=sample.substring(0,180)+"…";
        String note=replacing?"同 id 句库将被更新；保留仍存在的内容的收藏，重新开始阅读顺序。":"将添加新句库，不影响其他句库的收藏和进度。";
        new AlertDialog.Builder(this).setTitle(library.title+" · "+library.items.length()+" 条")
                .setMessage(sample+"\n\n"+note).setNegativeButton("取消",null)
                .setPositiveButton(replacing?"更新并使用":"导入并使用",(d,w)->{
                    try { Libraries.save(this,library);render(); }
                    catch (Exception e) { message("保存失败","无法写入句库，原有文件将尽可能保留，请检查剩余存储空间。"); }
                }).show();
    }
    private void message(String title,String content) { new AlertDialog.Builder(this).setTitle(title).setMessage(content).setPositiveButton("知道了",null).show(); }
    private void help() {
        message("每天一点，慢慢体会","本应用适用于支持标准小组件的 Android 8.0 及以上设备。长按桌面添加「一日一句」，推荐横向 4×2，长内容可放大或点开全文。\n\n支持从系统文件选择器导入 JSON，切换不同句库。可先导出当前句库作为格式参考；导出只包含内容，不包含收藏和阅读进度。\n\n多个桌面卡片共享当前句库。每日更新可能受系统省电策略影响而延迟，打开应用会检查日期并刷新。\n\n应用离线运行，无账号、广告及联网权限；内置书摘仅为示例内容，页码为 PDF 页序。");
    }
}
