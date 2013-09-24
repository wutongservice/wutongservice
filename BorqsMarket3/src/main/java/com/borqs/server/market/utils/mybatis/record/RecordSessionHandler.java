package com.borqs.server.market.utils.mybatis.record;


public interface RecordSessionHandler<T> {
    T handle(RecordSession session) throws Exception;
}
