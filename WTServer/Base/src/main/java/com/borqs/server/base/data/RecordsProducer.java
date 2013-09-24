package com.borqs.server.base.data;

import java.util.Set;


public interface RecordsProducer {
    RecordSet product(Set<String> cols) throws Exception;
}
