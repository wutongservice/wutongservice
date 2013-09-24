package com.borqs.server.platform.feature;


import com.borqs.server.platform.context.Context;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class TargetInfoFetcher {
    private static final TargetInfoFetcher instance = new TargetInfoFetcher();

    private List<Provider> providers;

    private TargetInfoFetcher() {
    }

    public static TargetInfoFetcher getInstance() {
        return instance;
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }

    public TargetInfo fetchTargetInfo(Context ctx, Target target) {
        if (target == null)
            return null;

        List<Provider> providers = this.providers;
        for (Provider provider : providers) {
            if (provider != null) {
                TargetInfo[] tis = provider.fetchTargetInfo(ctx, target);
                if (ArrayUtils.isNotEmpty(tis))
                    return tis[0];
            }
        }
        return null;
    }

    public TargetInfo fetchTargetInfo(Context ctx, String target) {
        return fetchTargetInfo(ctx, Target.parseCompatibleString(target));
    }

    public TargetInfo[] multiFetchTargetInfo(Context ctx, Target[] targets) {
        List<Provider> providers = this.providers;
        if (CollectionUtils.isEmpty(providers))
            return new TargetInfo[0];

        ArrayList<TargetInfo> l = new ArrayList<TargetInfo>();
        for (Provider provider : providers) {
            if (provider != null) {
                TargetInfo[] tis = provider.fetchTargetInfo(ctx, targets);
                Collections.addAll(l, tis);
            }
        }

        if (!l.isEmpty()) {
            ArrayList<TargetInfo> ll = new ArrayList<TargetInfo>();
            for (Target t : targets) {
                for (TargetInfo ti : l) {
                    Target t2 = ti.getTarget();
                    if (Target.equalsIgnoreClass(t, t2)) {
                        ll.add(ti);
                        break;
                    }
                }
            }
            l = ll;
        }
        return l.toArray(new TargetInfo[l.size()]);
    }


    public TargetInfo[] multiFetchTargetInfo(Context ctx, String... targets) {
        return multiFetchTargetInfo(ctx, Target.fromCompatibleStringArray(targets));
    }

    public static interface Provider {
        TargetInfo[] fetchTargetInfo(Context ctx, Target... targets);
    }
}
