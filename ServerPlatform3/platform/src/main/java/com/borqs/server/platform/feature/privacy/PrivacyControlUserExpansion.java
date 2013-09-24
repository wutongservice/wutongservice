package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserExpansion;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class PrivacyControlUserExpansion implements UserExpansion {

    public static final String COL_ALLOWED = "allowed";
    public static final String COL_HE_ALLOWED = "he_allowed";
    public static final String COL_MUTUAL_ALLOWED = "mutual_allowed";

    public static final String[] EXPAND_COLUMNS = {
            COL_ALLOWED,
            COL_HE_ALLOWED,
            COL_MUTUAL_ALLOWED,
    };

    static {
        User.registerColumnsAlias("@xprivacy,#xprivacy", EXPAND_COLUMNS);
    }

    private PrivacyControlLogic privacyControl;
    private final List<PrivacyProtector> protectors = new ArrayList<PrivacyProtector>();


    public PrivacyControlUserExpansion() {
        this(null);
    }

    public PrivacyControlUserExpansion(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;

        // init protector
        protectors.add(new VcardProtector());
    }

    public PrivacyControlLogic getPrivacyControl() {
        return privacyControl;
    }

    public void setPrivacyControl(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        if (CollectionUtils.isEmpty(data))
            return;

        long[] userIds = data.getUserIds();

        // allowed, he_allowed, mutual_allowed
        Map<Long, StringBuilder> heAllowed = getHeAllowed(ctx, userIds);
        boolean expandAllowed = ArrayUtils.contains(expCols, COL_ALLOWED);
        boolean expandMutualAllowed = ArrayUtils.contains(expCols, COL_MUTUAL_ALLOWED);
        if (expandAllowed || expandMutualAllowed) {
            Map<Long, StringBuilder> allowed = getAllowed(ctx, userIds);
            if (expandAllowed)
                expandAllowed(ctx, data, allowed);
            if (expandMutualAllowed)
                expandMutualAllowed(ctx, data, allowed, heAllowed);
        }

        if (ArrayUtils.contains(expCols, COL_HE_ALLOWED))
            expandHeAllowed(ctx, data, heAllowed);

        // protected privacy
        if (ctx.isPrivacyEnabled() && CollectionUtils.isNotEmpty(protectors)) {
            for (PrivacyProtector pp : protectors) {
                if (pp != null)
                    pp.protect(ctx, data, heAllowed);
            }
        }

    }

    private void expandMutualAllowed(Context ctx, Users users, Map<Long, StringBuilder> allowed, Map<Long, StringBuilder> heAllowed) {
        for (User user : users) {
            if (user == null)
                continue;

            if (user.getUserId() != ctx.getViewer()) {
                Long userId = user.getUserId();
                StringBuilder allowed0 = allowed.get(userId);
                if (allowed0 == null) {
                    user.setAddon(COL_MUTUAL_ALLOWED, "");
                    continue;
                }

                StringBuilder heAllowed0 = heAllowed.get(userId);
                if (heAllowed0 == null) {
                    user.setAddon(COL_MUTUAL_ALLOWED, "");
                    continue;
                }

                Set<String> set1 = StringHelper.splitSet(allowed0.toString(), ",", false);
                Set<String> set2 = StringHelper.splitSet(heAllowed0.toString(), ",", false);
                Collection mutualSet = CollectionUtils.intersection(set1, set2);
                user.setAddon(COL_MUTUAL_ALLOWED, CollectionUtils.isEmpty(mutualSet) ? "" : StringUtils.join(mutualSet, ","));
            } else {
                user.setAddon(COL_MUTUAL_ALLOWED, PrivacyResources.JOINED_RESOURCES);
            }
        }
    }

    private void expandAllowed(Context ctx, Users users, Map<Long, StringBuilder> allowed) {
        for (User user : users) {
            if (user == null)
                continue;

            if (user.getUserId() != ctx.getViewer()) {
                StringBuilder buff = allowed.get(user.getUserId());
                user.setAddon(COL_ALLOWED, ObjectUtils.toString(buff, ""));
            } else {
                user.setAddon(COL_ALLOWED, PrivacyResources.JOINED_RESOURCES);
            }
        }
    }

    private void expandHeAllowed(Context ctx, Users users, Map<Long, StringBuilder> heAllowed) {
        for (User user : users) {
            if (user == null)
                continue;

            if (user.getUserId() != ctx.getViewer()) {
                StringBuilder buff = heAllowed.get(user.getUserId());
                user.setAddon(COL_HE_ALLOWED, ObjectUtils.toString(buff, ""));
            } else {
                user.setAddon(COL_HE_ALLOWED, PrivacyResources.JOINED_RESOURCES);
            }
        }
    }

    private Map<Long, StringBuilder> getAllowed(Context ctx, long[] userIds) {
        LinkedHashMap<Long, StringBuilder> m = new LinkedHashMap<Long, StringBuilder>();

        for (String res : PrivacyResources.RESOURCES) {
            AllowedIds allowedIds = privacyControl.getAllowIds(ctx, ctx.getViewer(), res);
            for (long userId : userIds) {
                if (allowedIds.include(userId)) {
                    StringBuilder buff = m.get(userId);
                    if (buff == null) {
                        buff = new StringBuilder(res);
                        m.put(userId, buff);
                    } else {
                        buff.append(",").append(res);
                    }
                }
            }
        }
        return m;
    }

    private Map<Long, StringBuilder> getHeAllowed(Context ctx, long[] userIds) {
        LinkedHashMap<Long, StringBuilder> m = new LinkedHashMap<Long, StringBuilder>();
        for (String res : PrivacyResources.RESOURCES) {
            Map<Long, Boolean> mm = privacyControl.check(ctx, ctx.getViewer(), res, userIds);
            for (Map.Entry<Long, Boolean> e : mm.entrySet()) {
                Long userId = e.getKey();
                if (BooleanUtils.isTrue(e.getValue())) {
                    StringBuilder buff = m.get(userId);
                    if (buff == null) {
                        buff = new StringBuilder(res);
                        m.put(userId, buff);
                    } else {
                        buff.append(",").append(res);
                    }
                }
            }
        }
        return m;
    }

}
