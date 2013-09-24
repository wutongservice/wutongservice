package com.borqs.server.market.service;


public interface ServiceConsts {

    public static final String MODULE_PUBLISH = "publish";
    public static final String MODULE_DEVELOP = "develop";
    public static final String MODULE_OPER = "oper";
    public static final String MODULE_GSTAT = "gstat";

    int ROLE_PURCHASER = 1;
    int ROLE_PUBLISHER = 1 << 1;
    int ROLE_DEVELOPER = 1 << 2;
    int ROLE_OPERATOR = 1 << 3;
    int ROLE_BOSS = 1 << 4;

    int PT_PRICE_ALL = 0;
    int PT_PRICE_FREE = 1;
    int PT_PRICE_PAID = 2;

    int PT_PAYMENT_ONCE = 1;
    int PT_PAYMENT_REPURCHASABLE = 2;

    int PO_DOWNLOAD_COUNT = 1;
    int PO_RATING = 2;
    int PO_PURCHASE_COUNT = 3;

    int PV_STATUS_ACTIVE = 1;
    int PV_STATUS_APPROVED = 1 << 1;
    int PV_STATUS_PUBLISHED = PV_STATUS_ACTIVE | PV_STATUS_APPROVED;

    int PV_ACTION_NONE = 0;
    int PV_ACTION_DOWNLOAD = 1;

    // Partitions
    int PTT_LIST = 1;
    int PTT_RULE = 2;

    int PTS_DEACTIVE = 0;
    int PTS_1 = 1;
    int PTS_2 = 2;
    int PTS_3 = 3;
    int PTS_4 = 4;
    int ACTIVE_PARTITION_COUNTS = 4;

    int SHARE_STATUS_APPROVED = 1 << 1;

    // Product promotion type
    int PPT_PRODUCT = 1;
    int PPT_PARTITION = 2;
    int PPT_TAG = 3;
    int PPT_SHARE = 4;
    int PPT_SORT = 5;

    String PUBCHNL_GOOGLE_PLAY = "g";
    String PUBCHNL_CMCC = "mm";
    String PUBCHNL_AMAZON = "a";
    String[] ALL_PUBCHNLS = {PUBCHNL_GOOGLE_PLAY, PUBCHNL_CMCC, PUBCHNL_AMAZON};
}
