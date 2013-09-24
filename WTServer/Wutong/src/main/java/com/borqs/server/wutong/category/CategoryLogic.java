package com.borqs.server.wutong.category;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface CategoryLogic {
    RecordSet createCategoryType(Context ctx, RecordSet categories);

    Record createCategory(Context ctx, String userId,String categoryId,String targetType,String targetId);

    Record updateCategoryType(Context ctx, Record category);

    Record updateCategory(Context ctx, Record category);

    boolean destroyedCategories(Context ctx, Record category);

    RecordSet getCategories(Context ctx, String category_id,long startTime,long endTime,int page,int count);

    RecordSet getCategoryTypes(Context ctx, String scope);

    Record getCategoryType(Context ctx, String category_id);
}