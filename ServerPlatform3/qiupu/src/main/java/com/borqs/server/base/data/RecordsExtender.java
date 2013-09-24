package com.borqs.server.base.data;


import com.borqs.server.base.util.CollectionUtils2;
import org.apache.commons.lang.Validate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class RecordsExtender {

    protected RecordsExtender() {
    }

    public abstract Set<String> necessaryColumns();

    public abstract Set<String> extendedColumns();

    public abstract void extend(RecordSet recs, Set<String> cols);


    public RecordSet extendRecords(Set<String> cols, RecordsProducer producer) {
        Validate.notNull(cols);
        Validate.notNull(producer);

        try {
            LinkedHashSet<String> produceCols = new LinkedHashSet<String>(cols);
            produceCols.removeAll(extendedColumns());
            produceCols.addAll(necessaryColumns());


            RecordSet recs = producer.product(produceCols);
            extend(recs, cols);

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
}
