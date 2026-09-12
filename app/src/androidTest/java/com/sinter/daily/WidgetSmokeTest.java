package com.sinter.daily;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

/** Device tests exercise real SharedPreferences and Android RemoteViews inflation. */
public class WidgetSmokeTest extends InstrumentationTestCase {
    public void testWidgetInflatesOnAndroid() throws Throwable {
        final Context c = getInstrumentation().getTargetContext();
        runTestOnUiThread(() -> {
            RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget);
            views.setTextViewText(R.id.quote, Store.quote(c, 0).optString("text"));
            views.setTextViewText(R.id.day_number, "12");
            views.setTextViewText(R.id.month, "9月");
            View widget = views.apply(c, new FrameLayout(c));
            assertNotNull(widget.findViewById(R.id.quote));
            assertNotNull(widget.findViewById(R.id.day_number));
        });
    }

    public void testRealPreferencesAndDateTransition() {
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

    public void testMainActivityLaunches() throws Throwable {
        Context c = getInstrumentation().getTargetContext();
        Activity a = getInstrumentation().startActivitySync(
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        assertNotNull(a);
        assertFalse(a.isFinishing());
        runTestOnUiThread(a::finish);
    }
}
