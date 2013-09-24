package com.borqs.server.market.models;


import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;

public class PublishChannels implements ServiceConsts {

    public static String getDefaultPublishChannel(RecordSession session, String appId) {
        return session.selectStringValue("market.getDefaultPublishChannelForPurchase",
                CC.map("id=>", appId), "");
    }

    public static String getPaidField(String publishChannel) {
        if (PUBCHNL_GOOGLE_PLAY.equals(publishChannel)) {
            return "paid";
        } else if (PUBCHNL_CMCC.equals(publishChannel)) {
            return "cmcc_mm_paid";
        } else {
            throw new IllegalArgumentException("Illegal publishChannel");
        }
    }

    public static String getPriceField(String publishChannel) {
        if (PUBCHNL_GOOGLE_PLAY.equals(publishChannel)) {
            return "price";
        } else if (PUBCHNL_CMCC.equals(publishChannel)) {
            return "cmcc_mm_price";
        } else {
            throw new IllegalArgumentException("Illegal publishChannel");
        }
    }
}
