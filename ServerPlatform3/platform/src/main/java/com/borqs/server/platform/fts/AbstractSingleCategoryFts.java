package com.borqs.server.platform.fts;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractSingleCategoryFts implements FTS {
    protected final String category;

    protected AbstractSingleCategoryFts(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    protected void checkCategory(String category) {
        if (!StringUtils.equals(category, this.category))
            throw new ServerException(E.FTS);
    }

    protected void checkDocCategory(FTDoc... docs) {
        for (FTDoc doc : docs) {
            if (doc != null)
                checkCategory(doc.getCategory());
        }
    }
}
