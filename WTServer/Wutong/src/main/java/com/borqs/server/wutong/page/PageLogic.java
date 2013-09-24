package com.borqs.server.wutong.page;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface PageLogic {
    public static final String BASIC_COLS =
            "page_id, created_time, updated_time, type, flags, " +
            "email_domain1, email_domain2, email_domain3, email_domain4, " +
            "name, name_en, address, address_en, email, website, tel, fax, zip_code, description, description_en, " +
            "small_logo_url, logo_url, large_logo_url, small_cover_url, cover_url, large_cover_url, " +
            "associated_id, free_circle_ids,creator, ";


    //Record createPageWithCircle(Context ctx, Record pageRec, String type, String subType);
    Record createPage(Context ctx, Record pageRec);
    void destroyPage(Context ctx, long pageId, boolean destroyAssociated);
    void destroyPageByAssociated(Context ctx, long associatedId, boolean destroyAssociated);
    Record updatePage(Context ctx, Record pageRec, boolean withAssociated, boolean checkPermission);

    RecordSet getPages(Context ctx, long[] pageIds);
    RecordSet getPagesForMe(Context ctx);
    Record getPage(Context ctx, long pageId);
    Record getPageNoThrow(Context ctx, long pageId);
    RecordSet searchPages(Context ctx, String kw, int page, int count);

    boolean hasPage(Context ctx, long pageId);
}
