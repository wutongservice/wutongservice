package com.borqs.server.platform.feature;


import com.borqs.server.platform.util.TextEnum;

public class Actions {

    // max value is 255

    public static final int NONE = 0;

    public static final int CREATE = 1;
    public static final int DESTROY = 2;
    public static final int UPDATE = 3;
    public static final int RECOVER = 4;
    public static final int ACCEPT = 5;
    public static final int REJECT = 6;
    public static final int REQUEST = 7;
    public static final int DONE = 8;


    public static final int COMMENT = 11;

    public static final int LIKE = 21;

    public static final int FAVORITE = 31;

    public static final int SHARE = 41;
    public static final int RESHARE = 42;
    public static final int SHARE_ATTACHMENTS = 43;

    public static final int UPLOAD = 51;

    public static final int IGNORE = 61;
    public static final int UNIGNORE = 62;

    public static final int TO = 71;
    public static final int ADDTO = 72;

    public static final int CI_BIND = 81;
    public static final int CI_UNBIND = 82;

    public static final int STATUS = 91;

    public static final int SET_FRIENDS = 101;
    public static final int ADD_FRIENDS = 102;
    public static final int DELETE_FRIENDS = 103;
    public static final int SET_REMARK = 104;

    public static final int LOGIN = 111;
    public static final int LOGOUT = 112;

    public static final int RECOMMEND = 113;

    public static final TextEnum ACTIONS_ENUM = TextEnum.of(new Object[][] {
            {"create", CREATE},
            {"destroy", DESTROY},
            {"update", UPDATE},
            {"recover", RECOVER},
            {"accept", ACCEPT},
            {"reject", REJECT},
            {"request", REQUEST},
            {"done", DONE},

            {"comment", COMMENT},

            {"like", LIKE},

            {"favorite", FAVORITE},

            {"share", SHARE},
            {"reshare", RESHARE},

            {"upload", UPLOAD},

            {"ignore", IGNORE},
            {"unignore", UNIGNORE},

            {"to", TO},
            {"addto", ADDTO},

            {"ci_bind", CI_BIND},
            {"ci_unbind", CI_UNBIND},

            {"status", STATUS},

            {"set_friends", SET_FRIENDS},
            {"add_friends", ADD_FRIENDS},
            {"delete_friends", DELETE_FRIENDS},
            {"set_remark", SET_REMARK},

            {"login", LOGIN},
            {"logout", LOGOUT},

            {"recommend", RECOMMEND},
    });

    public static String actionToStr(int action) {
        return ACTIONS_ENUM.getText(action, "unknown");
    }
}
