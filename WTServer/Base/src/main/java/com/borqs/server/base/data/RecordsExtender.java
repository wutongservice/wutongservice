package com.borqs.server.base.data;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.context.Context;
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

    public abstract void extend(Context ctx, RecordSet recs, Set<String> cols);


    public RecordSet extendRecords( Context ctx, Set<String> cols, RecordsProducer producer) {
        Validate.notNull(cols);
        Validate.notNull(producer);

        try {
            LinkedHashSet<String> produceCols = new LinkedHashSet<String>(cols);
            produceCols.removeAll(extendedColumns());
            produceCols.addAll(necessaryColumns());


            RecordSet recs = producer.product(produceCols);
            extend(ctx, recs, cols);

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
}
