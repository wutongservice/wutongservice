package com.borqs.server.qiupu;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.qiupu.AbstractQiupuUserExpansion;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

public class QiupuUserExpansion extends AbstractQiupuUserExpansion {

    private QiupuFactory qiupuFactory;

    public QiupuUserExpansion() {
    }

    public QiupuFactory getQiupuFactory() {
        return qiupuFactory;
    }

    public void setQiupuFactory(QiupuFactory qiupuFactory) {
        this.qiupuFactory = qiupuFactory;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, COL_FAVORITE_APP_COUNT))
            expandFavoriteApkCount(ctx, data);
    }

    private void expandFavoriteApkCount(Context ctx, Users users) {
        long[] userIds = users.getUserIds();
        Qiupu q = qiupuFactory.createQiupu();
        RecordSet rs;
        try {
            rs = q.getUsersAppCount(StringHelper.join(userIds, ","), String.valueOf(1 << 3));
        } catch (Exception e) {
            rs = new RecordSet();
        }

        for (User user : users) {
            if (user == null || user.getUserId() <= 0)
                continue;

            int favoriteApkCount = getUserAppCount(rs, user.getUserId());
            user.setAddon(COL_FAVORITE_APP_COUNT, favoriteApkCount);
        }
    }

    private static int getUserAppCount(RecordSet recs, long userId) {
        for (Record rec : recs) {
            if (rec.getInt("user_id") == userId)
                return (int)rec.getInt("count");
        }
        return 0;
    }
}
