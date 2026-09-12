package com.sinter.daily;

import org.json.JSONArray;
import org.json.JSONObject;

/** Copy-on-write edits use the same validation as JSON import. */
final class LibraryEdits {
    static QuoteLibrary change(QuoteLibrary library, String itemId, JSONObject replacement) throws Exception {
        JSONObject root=new JSONObject(library.json.toString());
        JSONArray items=new JSONArray();
        boolean found=itemId==null;
        for (int i=0;i<library.items.length();i++) {
            JSONObject item=library.items.getJSONObject(i);
            if (item.optString("id").equals(itemId)) {
                found=true;
                if (replacement!=null) {
                    JSONObject copy=new JSONObject(replacement.toString());
                    copy.put("id",itemId);
                    items.put(copy);
                }
            } else items.put(item);
        }
        if (!found) throw new IllegalArgumentException("该条目已不存在，请刷新列表。");
        if (itemId==null && replacement!=null) {
            JSONObject copy=new JSONObject(replacement.toString());
            copy.put("id",java.util.UUID.randomUUID().toString());
            items.put(copy);
        }
        root.put("items",items);
        return QuoteLibrary.parse(root.toString());
    }
}
