package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.context.Context;
import org.apache.commons.collections.CollectionUtils;

public class PhotoUrlPrefixUserExpansion implements UserExpansion {
    private String prefix;

    public PhotoUrlPrefixUserExpansion() {
    }

    public PhotoUrlPrefixUserExpansion(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        if (CollectionUtils.isEmpty(data))
            return;

        for (User user : data) {
            if (user != null) {
                PhotoInfo pi = user.getPhoto();
                if (pi != null)
                    pi.addUrlPrefix(prefix);
            }
        }
    }
}
