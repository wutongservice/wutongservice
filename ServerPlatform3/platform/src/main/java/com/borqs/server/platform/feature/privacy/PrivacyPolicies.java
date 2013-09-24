package com.borqs.server.platform.feature.privacy;


public class PrivacyPolicies {

    private static final PrivacyEntry[] DEFAULT_POLICES = {
            new PrivacyEntry(0, PrivacyResources.RES_VCARD, PrivacyTarget.all(), false),
    };

    public static PrivacyEntry[] getDefaultPolices() {
        return DEFAULT_POLICES;
    }

    public static PrivacyEntry getDefault(String res) {
        for (PrivacyEntry pe : getDefaultPolices()) {
            if (pe.resource.equals(res))
                return pe;
        }
        return null;
    }

    public static boolean getDefaultBoolean(String res) {
        PrivacyEntry pe = getDefault(res);
        return pe != null && pe.allow;
    }

}
