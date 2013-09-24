package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.fts.FTDoc;
import com.borqs.server.platform.fts.simple.SingleCategorySimpleFts;

import java.util.ArrayList;


public class UserSimpleFts extends SingleCategorySimpleFts {

    public UserSimpleFts() {
        super(UserFts.CATEGORY);
    }

    public void deleteUsers(Context ctx, long... userIds) {
        ArrayList<FTDoc> docs = new ArrayList<FTDoc>();
        for (long userId : userIds) {
            if (userId > 0)
                docs.add(new FTDoc(UserFts.CATEGORY, Long.toString(userId)));

        }
        deleteDoc(ctx, docs.toArray(new FTDoc[docs.size()]));
    }

    public void saveUsers(Context ctx, User... users) {
        ArrayList<FTDoc> docs = new ArrayList<FTDoc>();
        for (User user : users) {
            if (user != null && user.getUserId() > 0)
                docs.add(UserFts.toFtDoc(user));
        }
        saveDoc(ctx, docs.toArray(new FTDoc[docs.size()]));
    }


}
