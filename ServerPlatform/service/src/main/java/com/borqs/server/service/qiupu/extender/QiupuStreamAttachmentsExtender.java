package com.borqs.server.service.qiupu.extender;


import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.service.platform.extender.PlatformExtender;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;

import java.util.Set;

public class QiupuStreamAttachmentsExtender extends QiupuExtender {
    private static final Set<String> NECESSARY_COLUMNS = CollectionUtils2.asSet("post_id", "type", "attachments");
    private static final Set<String> EXTENDED_COLUMNS = CollectionUtils2.asSet("attachments");

    public QiupuStreamAttachmentsExtender() {
    }

    @Override
    public Set<String> necessaryColumns() {
        return NECESSARY_COLUMNS;
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTENDED_COLUMNS;
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (cols.contains("attachments"))
            extendAttachments(recs);
    }

    protected void extendAttachments(RecordSet recs) throws AvroRemoteException {
        // TODO: xx
    }
}
