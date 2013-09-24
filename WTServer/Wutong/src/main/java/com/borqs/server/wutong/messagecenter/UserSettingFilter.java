package com.borqs.server.wutong.messagecenter;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.setting.SettingLogic;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;

public class UserSettingFilter {

    public static final String CONFIG_FALSE = "0";
    public static final String CONFIG_TRUE = "1";

    /**
     * filter userIds by user setting
     *
     * @param ctx
     * @param type    not use
     * @param key
     * @param userids
     * @return
     */
    public static String getUserSettingFilter(Context ctx, String type, String key, String userids) {
        if (StringUtils.isBlank(key)) {
            return "you should give me the key";
        }
        if (StringUtils.isBlank(userids)) {
            return "you should give me the userIds";
        }
        SettingLogic settingLogic = GlobalLogics.getSetting();
        Record record = settingLogic.getByUsers(ctx, key, userids);

        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry entry : record.entrySet()) {
            if (CONFIG_TRUE.equals(entry.getValue())) {
                stringBuffer.append(entry.getKey()).append(",");
            }
        }
        String str = stringBuffer.toString();
        return str.length() > 0 ? StringUtils.substring(str, 0, str.length() - 1) : str;
    }

    /**
     * 得到用户所有的配置，包含true和false ，如果没有设置返回true,实际情况可能是 返回0 false 1 true
     * @param ctx
     * @param key
     * @param userid
     * @return
     */
    public static String getAllUserSettingFilter(Context ctx, String key, String userid) {
        if (StringUtils.isBlank(key)) {
            return "you should give me the key";
        }
        if (StringUtils.isBlank(userid)) {
            return "you should give me the userId";
        }

        SettingLogic settingLogic = GlobalLogics.getSetting();
        Record record = settingLogic.getByUsers(ctx, key, userid);
        if(record !=null)
            return record.getString(userid);
        else{
            return CONFIG_TRUE;
        }
    }
}
