package com.sinter.daily;

import org.junit.Test;
import org.json.JSONException;
import static org.junit.Assert.*;

public class QuoteLibraryTest {
    private String library(String items) { return "{\"schemaVersion\":1,\"id\":\"notes\",\"title\":\"随手记\",\"items\":"+items+"}"; }
    @Test public void parsesCustomLibraryAndStableItemIds() throws Exception {
        QuoteLibrary a=QuoteLibrary.parse(library("[\"先观察，再判断。\",{\"id\":\"x\",\"text\":\"允许自己修改观点。\",\"author\":\"我\"}]"));
        assertEquals("随手记",a.title);assertEquals(2,a.items.length());
        assertEquals("x",a.items.getJSONObject(1).getString("id"));
        QuoteLibrary b=QuoteLibrary.parse(library("[\"先观察，再判断。\"]"));
        assertEquals(a.items.getJSONObject(0).getString("id"),b.items.getJSONObject(0).getString("id"));
    }
    @Test public void acceptsLegacyExtractedArray() throws Exception {
        QuoteLibrary a=QuoteLibrary.parse("[{\"id\":1,\"text\":\"一句话\",\"page\":9,\"endPage\":10}]");
        assertEquals("1",a.items.getJSONObject(0).getString("id"));
        assertEquals(10,a.items.getJSONObject(0).getInt("endPage"));
    }
    @Test public void normalizedExportCanBeReimported() throws Exception {
        QuoteLibrary a=QuoteLibrary.parse(library("[\"一句话\"]"));
        QuoteLibrary b=QuoteLibrary.parse(a.json.toString());
        assertEquals(a.json.toString(),b.json.toString());
    }
    @Test public void utf8BomIsAccepted() throws Exception {
        assertEquals(1,QuoteLibrary.parse("\uFEFF"+library("[\"一句话\"]")).items.length());
    }
    @Test(expected=JSONException.class) public void duplicateIdsRejected() throws Exception { QuoteLibrary.parse(library("[{\"id\":\"a\",\"text\":\"甲\"},{\"id\":\"a\",\"text\":\"乙\"}]")); }
    @Test(expected=JSONException.class) public void emptyTextRejected() throws Exception { QuoteLibrary.parse(library("[\"  \"]")); }
    @Test(expected=JSONException.class) public void emptyLibraryRejected() throws Exception { QuoteLibrary.parse(library("[]")); }
    @Test(expected=JSONException.class) public void pathIdRejected() throws Exception { QuoteLibrary.parse(library("[\"甲\"]").replace("\"notes\"","\"../other\"")); }
    @Test(expected=JSONException.class) public void unknownVersionRejected() throws Exception { QuoteLibrary.parse(library("[\"甲\"]").replace(":1",":2")); }
    @Test(expected=JSONException.class) public void extraContentRejected() throws Exception { QuoteLibrary.parse(library("[\"甲\"]")+"{}"); }
    @Test(expected=JSONException.class) public void invalidPageRangeRejected() throws Exception { QuoteLibrary.parse(library("[{\"text\":\"甲\",\"page\":9,\"endPage\":3}]")); }
    @Test(expected=JSONException.class) public void oversizedTextRejected() throws Exception {
        StringBuilder s=new StringBuilder();for(int i=0;i<4001;i++)s.append('a');
        QuoteLibrary.parse(library("[\""+s+"\"]"));
    }
}
