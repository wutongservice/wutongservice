package com.borqs.server.platform.fts;


import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.util.PinyinHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;


import java.util.HashMap;
import java.util.Map;

public class FTDoc {

    public static final String FULLTEXT_POSTFIX = ".ft";

    private String category;
    private String id;
    private double weight = 1.0;
    private Map<String, String> contents;
    private Options options;

    public FTDoc(String category, String id) {
        this(category, id, 1.0, null, null);
    }

    public FTDoc(String category, String id, Map<String, String> contents) {
        this(category, id, 1.0, contents, null);
    }

    public FTDoc(String category, String id, double weight, Map<String, String> contents) {
        this(category, id, weight, contents, null);
    }

    public FTDoc(String category, String id, double weight, Map<String, String> contents, Options options) {
        this.category = category;
        this.id = id;
        this.weight = weight;
        this.contents = contents;
        this.options = options;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Map<String, String> getContents() {
        return contents;
    }

    public Map<String, String> getOriginalContents() {
        if (contents == null)
            return null;

        HashMap<String, String> m = new HashMap<String, String>();
        if (!contents.isEmpty()) {
            for (Map.Entry<String, String> e : contents.entrySet()) {
                if (!e.getKey().endsWith(FULLTEXT_POSTFIX))
                    m.put(e.getKey(), e.getValue());
            }
        }
        return m;
    }

    public String getFulltextContent(String orgKey) {
        if (contents == null)
            return null;

        if (orgKey.endsWith(FULLTEXT_POSTFIX))
            return null;

        return contents.get(orgKey + FULLTEXT_POSTFIX);
    }

    public void setContents(Map<String, String> contents) {
        this.contents = contents;
    }

    public Options getOptions() {
        return options;
    }

    public void setOpts(Options options) {
        this.options = options;
    }

    public static class Options extends Record {
    }
}
