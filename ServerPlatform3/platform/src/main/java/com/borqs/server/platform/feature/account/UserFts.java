package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.fts.FTDoc;
import com.borqs.server.platform.util.ChineseSegmentHelper;
import com.borqs.server.platform.util.PinyinHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class UserFts {
    public static final String CATEGORY = "user";

    public static final String ADDRESS = "address";
    public static final String COMPANY = "company";
    public static final String NAME = "name";
    public static final String NAME_FULL_PINYIN = "name_fpy";
    public static final String NAME_SHORT_PINYIN = "name_spy";

    private static final Set<String> COMPANY_STOP_WORDS;

    static {
        COMPANY_STOP_WORDS = new HashSet<String>();
        Collections.addAll(COMPANY_STOP_WORDS, StringUtils.split("公司 有限公司 股份有限公司", " "));
    }

    public static FTDoc toFtDoc(User user) {
        HashMap<String, String> contents = new HashMap<String, String>();
        String name = user.getDisplayName();
        String nameFullPy = null;
        String nameShortPy = null;
        if (StringUtils.isNotBlank(name)) {
            nameFullPy = PinyinHelper.toFullPinyin(name);
            nameShortPy = PinyinHelper.toShortPinyin(name);
            if (StringUtils.equalsIgnoreCase(nameFullPy, name))
                nameFullPy = null;
            if (StringUtils.equalsIgnoreCase(nameShortPy, name))
                nameShortPy = null;
        }

        if (StringUtils.isNotBlank(name)) {
            contents.put(UserFts.NAME, name);
            String seg = ChineseSegmentHelper.segmentNameString(name);
            contents.put(UserFts.NAME + FTDoc.FULLTEXT_POSTFIX, seg);
        }

        if (StringUtils.isNotEmpty(nameFullPy))
            contents.put(UserFts.NAME_FULL_PINYIN, nameFullPy);

        if (StringUtils.isNotEmpty(nameShortPy))
            contents.put(UserFts.NAME_SHORT_PINYIN, nameShortPy);

        List<AddressInfo> ais = user.getAddress();
        if (CollectionUtils.isNotEmpty(ais)) {
            AddressInfo ai = ais.get(0);
            if (ai != null) {
                String s = ai.getSearchableContent();
                if (StringUtils.isNotBlank(s)) {
                    contents.put(UserFts.ADDRESS, s);
                    String seg = ChineseSegmentHelper.segmentString(s, ChineseSegmentHelper.Options.create(4));
                    contents.put(UserFts.ADDRESS + FTDoc.FULLTEXT_POSTFIX, seg);
                }
            }
        }

        List<OrgInfo> ois = user.getOrganization();
        if (CollectionUtils.isNotEmpty(ois)) {
            OrgInfo oi = ois.get(0);
            if (oi != null) {
                String s = oi.getCompany();
                if (StringUtils.isNotBlank(s)) {
                    contents.put(UserFts.COMPANY, s);
                    String seg = ChineseSegmentHelper.segmentString(s, ChineseSegmentHelper.Options.create(COMPANY_STOP_WORDS, 2));
                    contents.put(UserFts.COMPANY + FTDoc.FULLTEXT_POSTFIX, seg);
                }
            }
        }

        return new FTDoc(CATEGORY, Long.toString(user.getUserId()), 1.0, contents, new FTDoc.Options());
    }
}
