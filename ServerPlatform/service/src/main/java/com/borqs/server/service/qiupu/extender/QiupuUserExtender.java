package com.borqs.server.service.qiupu.extender;


import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.service.platform.extender.PlatformExtender;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;

import java.util.Set;

public class QiupuUserExtender extends QiupuExtender {
    private final Set<String> NECESSARY_COLUMNS = CollectionUtils2.asSet("user_id");
    private final Set<String> EXTENDED_COLUMNS = CollectionUtils2.asSet("user_app_count");

    public QiupuUserExtender() {
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        // TODO: extend user
    }

    @Override
    public Set<String> necessaryColumns() {
        return NECESSARY_COLUMNS;
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTENDED_COLUMNS;
    }
}
