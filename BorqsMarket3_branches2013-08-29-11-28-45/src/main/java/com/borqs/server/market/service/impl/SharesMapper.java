package com.borqs.server.market.service.impl;


import com.borqs.server.market.utils.PrimitiveTypeConverter;
import com.borqs.server.market.utils.mybatis.record.RecordMapper;
import com.borqs.server.market.utils.record.Record;

public class SharesMapper implements RecordMapper {

    private static final SharesMapper instance = new SharesMapper();

    private SharesMapper() {
    }

    public static SharesMapper get() {
        return instance;
    }

    @Override
    public Record map(Record rec) {
        if (rec == null)
            return null;

        rec.transactValue("supported_mod", GenericMapper.arrayTransactor);
        GenericMapper.trimBooleanFields(rec);
        return rec;
    }
}
