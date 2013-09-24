package com.borqs.server.market.utils.mybatis;


import org.apache.ibatis.session.SqlSession;

public interface SqlSessionHandler<T> {
    T handleSession(SqlSession session) throws Exception;
}
