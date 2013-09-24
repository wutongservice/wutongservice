package com.borqs.server.base.data;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.StringUtils2;
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


    public RecordSet extendRecords(Set<String> cols, RecordsProducer producer) {
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
                re.extend(recs, cols);
            }

            return recs.retainColumns(cols);
        } catch (Exception e) {
            throw new DataException("Extend records error", e);
        }
    }

    public RecordSet extendRecords(String[] cols, RecordsProducer producer) {
        return extendRecords(CollectionUtils2.asSet(cols), producer);
    }

    public RecordSet extendRecords(List<String> cols, RecordsProducer producer) {
        return extendRecords(CollectionUtils2.asSet(cols), producer);
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
