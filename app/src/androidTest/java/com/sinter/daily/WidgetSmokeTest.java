package com.sinter.daily;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileOutputStream;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

/** Device tests exercise real SharedPreferences and Android RemoteViews inflation. */
@RunWith(AndroidJUnit4.class)
public class WidgetSmokeTest {
    private Instrumentation getInstrumentation() { return InstrumentationRegistry.getInstrumentation(); }
    @Before public void selectBuiltin() { Libraries.select(getInstrumentation().getTargetContext(),Libraries.BUILTIN); }
    @After public void restoreBuiltin() { Libraries.select(getInstrumentation().getTargetContext(),Libraries.BUILTIN); }

    @Test public void testSystemDiscoversAndBindsWidget() {
        Context c=getInstrumentation().getTargetContext();
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        android.content.ComponentName component=new android.content.ComponentName(c,QuoteWidget.class);
        android.appwidget.AppWidgetProviderInfo info=null;
        for (android.appwidget.AppWidgetProviderInfo candidate:manager.getInstalledProviders()) {
            if (component.equals(candidate.provider)) { info=candidate;break; }
        }
        assertNotNull("Provider must appear in the system widget catalog",info);
        assertEquals("一日一句",info.loadLabel(c.getPackageManager()));
        assertTrue((info.widgetCategory & android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)!=0);
        android.appwidget.AppWidgetHost host=new android.appwidget.AppWidgetHost(c,731);
        getInstrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
        int id=host.allocateAppWidgetId();
        try {
            assertTrue("System host must be able to bind this provider",manager.bindAppWidgetIdIfAllowed(id,component));
            assertEquals(component,manager.getAppWidgetInfo(id).provider);
            getInstrumentation().runOnMainSync(()->{
                android.appwidget.AppWidgetHostView view=host.createView(c,id,manager.getAppWidgetInfo(id));
                view.updateAppWidget(QuoteWidget.buildViews(c,Store.current(c),new Bundle()));
                assertNotNull(view.findViewById(R.id.quote));
            });
        } finally {
            host.deleteAppWidgetId(id);
            getInstrumentation().getUiAutomation().dropShellPermissionIdentity();
        }
    }

    @Test public void testWidgetInflatesOnAndroid() throws Throwable {
        final Context c = getInstrumentation().getTargetContext();
        getInstrumentation().runOnMainSync(() -> {
            int longest = 0;
            for (int i = 1; i < Store.data(c).length(); i++) {
                if (Store.quote(c,i).optString("text").length() > Store.quote(c,longest).optString("text").length()) longest = i;
            }
            captureWidget(c, 0, "widget-short.png");
            captureWidget(c, longest, "widget-long.png");
        });
    }

    @Test public void testRealPreferencesAndDateTransition() {
        Context c = getInstrumentation().getTargetContext();
        Store.prefs(c).edit().clear().commit();
        int first = Store.current(c);
        assertEquals(first, Store.current(c));
        Store.toggle(c, first);
        assertTrue(Store.favorite(c, first));
        Store.hold(c);
        Store.prefs(c).edit().putString("day", "2000-01-01").commit();
        assertEquals(first, Store.current(c));
        Store.hold(c);
        Store.prefs(c).edit().putString("day", "2000-01-01").commit();
        assertFalse(first == Store.current(c));
        assertTrue(Store.favorite(c, first));
    }

    @Test public void testMainActivityLaunches() throws Throwable {
        Context c = getInstrumentation().getTargetContext();
        Activity a = getInstrumentation().startActivitySync(
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        assertNotNull(a);
        assertFalse(a.isFinishing());
        getInstrumentation().runOnMainSync(a::finish);
    }
    @Test public void testLibrariesAreIndependentAndUpdatesKeepFavoriteIds() throws Exception {
        Context c=getInstrumentation().getTargetContext();
        QuoteLibrary a=QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"test-a\",\"title\":\"A\",\"items\":[{\"id\":\"first\",\"text\":\"First\"},{\"id\":\"second\",\"text\":\"Second\"}]}");
        QuoteLibrary b=QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"test-b\",\"title\":\"B\",\"items\":[\"Other\"]}");
        Store.prefsFor(c,a.id).edit().clear().commit();
        Libraries.save(c,a);Store.toggle(c,0);int current=Store.current(c);
        Libraries.save(c,b);assertEquals(0,Store.favorites(c).size());
        Libraries.select(c,a.id);assertTrue(Store.favorite(c,0));assertEquals(current,Store.current(c));
        QuoteLibrary updated=QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"test-a\",\"title\":\"A updated\",\"items\":[{\"id\":\"second\",\"text\":\"Second\"},{\"id\":\"first\",\"text\":\"First updated\"}]}");
        Libraries.save(c,updated);assertTrue(Store.favorite(c,1));assertFalse(Store.favorite(c,0));
        assertEquals("A updated",Libraries.active(c).title);
        QuoteLibrary exported=QuoteLibrary.parse(Libraries.exportJson(c));assertEquals(updated.id,exported.id);
        Libraries.select(c,Libraries.BUILTIN);
        assertFalse(Libraries.BUILTIN.equals(QuoteLibrary.parse(Libraries.exportJson(c)).id));
    }

    @Test public void testCrudPreservesStateAndEmptyLibraryWorks() throws Exception {
        Context c=getInstrumentation().getTargetContext();
        QuoteLibrary before=QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"crud-test\",\"title\":\"CRUD\",\"items\":[{\"id\":\"a\",\"text\":\"A\"},{\"id\":\"b\",\"text\":\"B\"}]}");
        Libraries.save(c,before);
        Store.prefs(c).edit().putInt("current",1).putString("queue","0").putString("day",Store.day()).putBoolean("hold",true).commit();
        Store.toggle(c,1);
        QuoteLibrary edited=LibraryEdits.change(before,"b",new org.json.JSONObject().put("text","Edited B").put("author","Me"));
        Libraries.saveEdited(c,before,edited);
        assertEquals(1,Store.current(c));assertTrue(Store.held(c));assertTrue(Store.favorite(c,1));
        assertEquals("Edited B",Store.quote(c,Store.current(c)).optString("text"));
        QuoteLibrary deleted=LibraryEdits.change(edited,"a",null);Libraries.saveEdited(c,edited,deleted);
        assertEquals(0,Store.current(c));assertTrue(Store.favorite(c,0));assertTrue(Store.held(c));
        Libraries.select(c,Libraries.BUILTIN);Libraries.select(c,"crud-test");
        assertEquals("Edited B",Libraries.active(c).items.optJSONObject(0).optString("text"));
        QuoteLibrary empty=LibraryEdits.change(deleted,"b",null);Libraries.saveEdited(c,deleted,empty);
        assertEquals(-1,Store.current(c));assertEquals(-1,Store.next(c));assertTrue(Store.favorites(c).isEmpty());
        assertEquals(0,QuoteLibrary.parse(Libraries.exportJson(c)).items.length());
        getInstrumentation().runOnMainSync(()->{
            View widget=QuoteWidget.buildViews(c,Store.current(c),new Bundle()).apply(c,new FrameLayout(c));
            assertTrue(((android.widget.TextView)widget.findViewById(R.id.quote)).getText().toString().contains("暂无内容"));
        });
        Activity a=getInstrumentation().startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().runOnMainSync(a::finish);
        QuoteLibrary added=LibraryEdits.change(empty,null,new org.json.JSONObject().put("text","New"));
        Libraries.saveEdited(c,empty,added);assertEquals("New",Store.quote(c,Store.current(c)).optString("text"));
    }

    @Test public void testContentManagerLaunchesAndSearchesImportedItems() throws Exception {
        Context c=getInstrumentation().getTargetContext();
        Libraries.save(c,QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"browse-test\",\"title\":\"Browse\",\"items\":[\"Alpha\",\"Beta\"]}"));
        Activity a=getInstrumentation().startActivitySync(new Intent(c,LibraryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().runOnMainSync(()->{
            android.widget.ListView list=findView(a.getWindow().getDecorView(),android.widget.ListView.class);
            android.widget.EditText search=findView(a.getWindow().getDecorView(),android.widget.EditText.class);
            assertNotNull(list);assertNotNull(search);assertEquals(2,list.getAdapter().getCount());
            search.setText("Beta");assertEquals(1,list.getAdapter().getCount());
            assertTrue(list.getAdapter().getItem(0).toString().contains("Beta"));
            search.setText("No match");assertEquals(0,list.getAdapter().getCount());a.finish();
        });
    }
    private <T extends View> T findView(View root,Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)root;
            for (int i=0;i<group.getChildCount();i++) { T found=findView(group.getChildAt(i),type);if (found!=null) return found; }
        }
        return null;
    }

    @Test public void testLargestFontFitsSquareAndSingleRowWidgets() {
        Context c=getInstrumentation().getTargetContext();
        int previous=QuoteWidget.fontSize(c);
        try {
            QuoteWidget.setFontSize(c,999);assertEquals(24,QuoteWidget.fontSize(c));
            getInstrumentation().runOnMainSync(()->{
                int longest=0;
                for (int i=1;i<Store.data(c).length();i++)
                    if (Store.quote(c,i).optString("text").length()>Store.quote(c,longest).optString("text").length()) longest=i;
                captureWidget(c,longest,"widget-square.png",140,140);
                captureWidget(c,longest,"widget-strip.png",300,55);
                android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());
                config.fontScale=2;
                captureWidget(c.createConfigurationContext(config),longest,"widget-accessible.png",110,40);
            });
            QuoteWidget.setFontSize(c,0);assertEquals(14,QuoteWidget.fontSize(c));
        } finally { QuoteWidget.setFontSize(c,previous); }
    }

    @Test public void testBuiltinEditsPersistWithoutCopy() throws Exception {
        Context c=getInstrumentation().getTargetContext();
        Libraries.select(c,Libraries.BUILTIN);
        QuoteLibrary before=Libraries.active(c);
        try {
            String id=before.items.optJSONObject(0).optString("id");
            QuoteLibrary changed=LibraryEdits.change(before,id,new org.json.JSONObject().put("text","Direct built-in edit"));
            Libraries.saveEdited(c,before,changed);
            Libraries.select(c,Libraries.BUILTIN);
            assertEquals("Direct built-in edit",Libraries.active(c).items.optJSONObject(0).optString("text"));
            int count=0;for (QuoteLibrary lib:Libraries.all(c)) if (Libraries.BUILTIN.equals(lib.id)) count++;
            assertEquals(1,count);
        } finally { Libraries.save(c,before); }
    }

    @Test public void testFavoritesScreenFiltersSavedItems() throws Exception {
        Context c=getInstrumentation().getTargetContext();
        Libraries.save(c,QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"favorites-ui\",\"title\":\"Favorites\",\"items\":[\"Alpha\",\"Beta\"]}"));
        Store.prefs(c).edit().clear().commit();Store.toggle(c,1);
        Activity a=getInstrumentation().startActivitySync(new Intent(c,LibraryActivity.class).putExtra("favorites",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().runOnMainSync(()->{
            android.widget.ListView list=findView(a.getWindow().getDecorView(),android.widget.ListView.class);
            assertEquals(1,list.getAdapter().getCount());assertTrue(list.getAdapter().getItem(0).toString().contains("Beta"));
            a.finish();
        });
    }

    private void captureWidget(Context c, int id, String filename) { captureWidget(c,id,filename,360,180); }
    private void captureWidget(Context c, int id, String filename,int widthDp,int heightDp) {
        Bundle size = new Bundle();
        size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
        size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        RemoteViews views = QuoteWidget.buildViews(c, id, size);
        View widget = views.apply(c, new FrameLayout(c));
        assertNotNull(widget.findViewById(R.id.quote));
        assertNotNull(widget.findViewById(R.id.day_number));
        float density = c.getResources().getDisplayMetrics().density;
        int width = (int)(widthDp * density), height = (int)(heightDp * density);
        widget.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        widget.layout(0,0,width,height);
        android.widget.TextView quoteView = widget.findViewById(R.id.quote);
        android.text.Layout layout = quoteView.getLayout();
        int lastLine = Math.min(layout.getLineCount(),quoteView.getMaxLines()) - 1;
        assertTrue("The last displayed line must fit completely",
                layout.getLineBottom(lastLine) <= quoteView.getHeight());
        if (filename.equals("widget-long.png")) assertTrue("Long content must end with an ellipsis",
                layout.getEllipsisCount(lastLine) > 0);
        Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        widget.draw(new Canvas(bitmap));
        try (FileOutputStream output = new FileOutputStream(new File(c.getExternalFilesDir(null),filename))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));
        } catch (Exception e) { throw new AssertionError(e); }
        bitmap.recycle();
        // Gradle uninstalls the test application afterward; preserve previews outside app storage.
        String command = "cp " + new File(c.getExternalFilesDir(null),filename).getAbsolutePath()
                + " /data/local/tmp/" + filename;
        try (android.os.ParcelFileDescriptor result = getInstrumentation().getUiAutomation().executeShellCommand(command);
             java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(result)) {
            while (input.read() != -1) { /* Wait until the shell copy is complete. */ }
        } catch (Exception e) { throw new AssertionError(e); }
    }

}
