package com.borqs.server.market.models;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

public class IPSource {

    public static String getCountry(RecordSession session, String ip) {
        Long ip2num = ipToLong(ip);
        Record ips = session.selectOne("statistics.findIp", CC.map("ip=>", ip2num));
        if (MapUtils.isNotEmpty(ips)) {
            return ips.asString("alias");
        } else {
            return "";
        }
    }

    private static Long ipToLong(String ip) {
        String[] splitIp = StringUtils.split(ip, '.');
        Long ip2num = null;
        if (splitIp.length == 4) {
            ip2num = new Long(splitIp[0]) * 16777216 + new Long(splitIp[1]) * 65536 + new Long(splitIp[2]) * 256 + new Long(splitIp[3]);
        }
        return ip2num;
    }
}
