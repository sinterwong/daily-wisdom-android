package com.sinter.daily;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class LibraryEditsTest {
    private QuoteLibrary base() throws Exception {
        return QuoteLibrary.parse("{\"schemaVersion\":1,\"id\":\"notes\",\"title\":\"Notes\",\"author\":\"Library author\",\"items\":[{\"id\":\"a\",\"text\":\"Original\",\"page\":1}]}");
    }
    @Test public void editKeepsIdentityAndDoesNotMutateOriginal() throws Exception {
        QuoteLibrary before=base();
        QuoteLibrary after=LibraryEdits.change(before,"a",new JSONObject().put("text","Changed").put("source","Source").put("page",3).put("endPage",5));
        assertEquals("a",after.items.getJSONObject(0).getString("id"));
        assertEquals("Original",before.items.getJSONObject(0).getString("text"));
        assertEquals("Library author",after.json.getString("author"));
        assertEquals(5,after.items.getJSONObject(0).getInt("endPage"));
    }
    @Test public void newDuplicateTextGetsUniqueIdentity() throws Exception {
        QuoteLibrary after=LibraryEdits.change(base(),null,new JSONObject().put("text","Original"));
        assertEquals(2,after.items.length());
        assertNotEquals("a",after.items.getJSONObject(1).getString("id"));
    }
    @Test public void deletingLastItemExportsAndAcceptsNewContent() throws Exception {
        QuoteLibrary empty=LibraryEdits.change(base(),"a",null);
        assertEquals(0,QuoteLibrary.parse(empty.json.toString()).items.length());
        assertEquals(1,LibraryEdits.change(empty,null,new JSONObject().put("text","New")).items.length());
    }
    @Test public void invalidEditLeavesOriginalUntouched() throws Exception {
        QuoteLibrary before=base();
        try { LibraryEdits.change(before,"a",new JSONObject().put("text"," "));fail(); }
        catch (org.json.JSONException expected) { assertEquals("Original",before.items.getJSONObject(0).getString("text")); }
    }
    @Test(expected=IllegalArgumentException.class) public void staleIdCannotSilentlyAddOrDelete() throws Exception {
        LibraryEdits.change(base(),"missing",null);
    }
}
