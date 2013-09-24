package com.borqs.server.market.utils.mybatis;


import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.Map;

public class SqlSessionUtils2 {
    @SuppressWarnings("unchecked")
    public static <T> T selectValue(SqlSession session, String statement, Object param) {
        Object o = session.selectOne(statement, param);
        if (o != null) {
            if (o instanceof Map) {
                return (T)((Map)o).values().iterator().next();
            } else {
                throw new RuntimeException("MyBatis result type error");
            }
        } else {
            return null;
        }
    }


    public static <T> T openSession(SqlSessionFactory sessionFactory, SqlSessionHandler<T> handler) throws Exception {
        SqlSession session = sessionFactory.openSession();
        try {
            return handler.handleSession(session);
        } finally {
            session.close();
        }
    }
}
