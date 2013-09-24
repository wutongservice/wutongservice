package com.borqs.server.market.utils.mybatis.typehandlers;


import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractStringHandler<T> implements TypeHandler<T> {
    protected AbstractStringHandler() {
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, toString(parameter));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        try {
            return fromString(rs.getString(columnName));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            return fromString(rs.getString(columnIndex));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            return fromString(cs.getString(columnIndex));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    protected abstract String toString(T val) throws Exception;
    protected abstract T fromString(String s) throws Exception;
}
