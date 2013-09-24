package com.borqs.server.base.data;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.CollectionUtils2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RecordsExtenders extends ArrayList<RecordsExtender> {

    private List<RecordsExtender> filterExtenders(Set<String> cols) {
        ArrayList<RecordsExtender> l = new RecordsExtenders();
        for (RecordsExtender re : this) {
            if (CollectionUtils2.containsOne(cols, re.extendedColumns()))
                l.add(re);
        }
        return l;
    }


    public RecordSet extendRecords(Context ctx, Set<String> cols, RecordsProducer producer) {
        Validate.notNull(cols);
        Validate.notNull(producer);

        try {
            List<RecordsExtender> filtered = filterExtenders(cols);
            LinkedHashSet<String> produceCols = new LinkedHashSet<String>(cols);
            for (RecordsExtender re : filtered) {
                if (re == null)
                    continue;

                produceCols.removeAll(re.extendedColumns());
                produceCols.addAll(re.necessaryColumns());
            }

            RecordSet recs = producer.product(produceCols);

            for (RecordsExtender re : filtered) {
                re.extend(ctx, recs, cols);
            }

            return recs.retainColumns(cols);
        } catch (Exception e) {
            throw new ServerException(BaseErrors.PLATFORM_RECORD_ERROR, "Extend records error", e);
        }
    }

    public RecordSet extendRecords(Context ctx, String[] cols, RecordsProducer producer) {
        return extendRecords(ctx, CollectionUtils2.asSet(cols), producer);
    }

    public RecordSet extendRecords(Context ctx, List<String> cols, RecordsProducer producer) {
        return extendRecords(ctx, CollectionUtils2.asSet(cols), producer);
    }

    public RecordsExtenders addExtendersInConfig(Configuration conf, String key) {
        String s = conf.getString(key, "");
        if (StringUtils.isNotBlank(s)) {
            List<RecordsExtender> extenders = ClassUtils2.newInstances(RecordsExtender.class, s);
            addAll(extenders);
        }
        return this;
    }
}
