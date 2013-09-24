package com.borqs.server.qiupu;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Hook;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.HashMap;
import java.util.Map;


public class QiupuAccountHook implements Hook {
    @Override
    public void before(Context ctx, RecordSet recs) {
        String userIds = (String) ctx.getSession("userIds");
        QiupuLogic qiupu = QiupuLogics.getQiubpu();
        RecordSet rs = qiupu.getUsersAppCount(ctx, userIds, String.valueOf(1 << 3));

        Map app_map = new HashMap();
        for (Record ur : rs) {
            app_map.put(ur.getString("user_id"), ur.getString("count"));
        }
        for (Record rec : recs) {
            rec.put("favorites_count", app_map.get(rec.getString("user_id")));
        }
    }

    @Override
    public void after(Context ctx, RecordSet recs) {

    }
}
