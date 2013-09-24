package com.borqs.server.wutong.group;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

@Deprecated
public interface MemberListLogic {
    public static final String COL_FID = "fid";
    public static final String COL_F01 = "f01";
    public static final String COL_F02 = "f02";
    public static final String COL_F03 = "f03";
    public static final String COL_F04 = "f04";
    public static final String COL_F05 = "f05";
    public static final String COL_F06 = "f06";
    public static final String COL_F07 = "f07";
    public static final String COL_F08 = "f08";
    public static final String COL_F09 = "f09";
    public static final String COL_F10 = "f10";
    public static final String COL_F11 = "f11";
    public static final String COL_F12 = "f12";
    public static final String[] COLS = {
            COL_F01, COL_F02, COL_F03, COL_F04, COL_F05, COL_F06,
            COL_F07, COL_F08, COL_F09, COL_F10, COL_F11, COL_F12,
    };


    Record addMember(Context ctx, long groupId, Record rec);
    void deleteMembers(Context ctx, long groupId, String[] ids);
    Record updateMember(Context ctx, long groupId, Record rec);
    int putMembers(Context ctx, long groupId, RecordSet recs, boolean merge);
    RecordSet getMembers(Context ctx, long groupId, String sort, int page, int count);
    RecordSet searchMember(Context ctx, long groupId, String kw, String sort, int count);
    Record getMember(Context ctx, long groupId, String fid);
    Record getMemberNoThrow(Context ctx, long groupId, String fid);
}
