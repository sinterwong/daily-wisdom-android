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

    private void captureWidget(Context c, int id, String filename) {
        Bundle size = new Bundle();
        size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180);
        RemoteViews views = QuoteWidget.buildViews(c, id, size);
        View widget = views.apply(c, new FrameLayout(c));
        assertNotNull(widget.findViewById(R.id.quote));
        assertNotNull(widget.findViewById(R.id.day_number));
        float density = c.getResources().getDisplayMetrics().density;
        int width = (int)(360 * density), height = (int)(180 * density);
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
