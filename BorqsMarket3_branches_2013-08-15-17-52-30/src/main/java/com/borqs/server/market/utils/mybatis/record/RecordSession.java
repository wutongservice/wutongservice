package com.borqs.server.market.utils.mybatis.record;


import com.borqs.server.market.utils.PrimitiveTypeConverter;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.ibatis.session.SqlSession;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class RecordSession implements Closeable {
    private final SqlSession session;

    public RecordSession(SqlSession session) {
        this.session = session;
    }


    public Record selectOne(String stmt) {
        return selectOne(stmt, null);
    }

    public Record selectOne(String stmt, Object param) {
        return selectOne(stmt, param, null);
    }


    public Record selectOne(String stmt, RecordMapper mapper) {
        Record rec = session.selectOne(stmt);
        if (mapper != null) {
            rec = mapper.map(rec);
        }
        return rec;
    }

    public Record selectOne(String stmt, Object param, RecordMapper mapper) {
        Record rec = session.selectOne(stmt, param);
        if (mapper != null) {
            rec = mapper.map(rec);
        }
        return rec;
    }

    private static Records mapRecords(List<Record> l, RecordMapper mapper) {
        if (l == null)
            return null;

        Records recs = new Records(l);
        if (mapper != null) {
            int len = l.size();
            for (int i = 0; i < len; i++)
                recs.set(i, mapper.map(recs.get(i)));
        }
        return recs;
    }

    public Records selectList(String stmt) {
        return selectList(stmt, null);
    }

    public Records selectList(String stmt, Object param) {
        return selectList(stmt, param, null);
    }

    public Records selectList(String stmt, RecordMapper mapper) {
        List<Record> l = session.selectList(stmt);
        return mapRecords(l, mapper);
    }

    public Records selectList(String stmt, Object param, RecordMapper mapper) {
        List<Record> l = session.selectList(stmt, param);
        return mapRecords(l, mapper);
    }

    public RecordsWithTotal selectListWithTotal(String stmt) {
        return selectListWithTotal(stmt, null);
    }

    public RecordsWithTotal selectListWithTotal(String stmt, Object param) {
        return selectListWithTotal(stmt, param, null);
    }

    public RecordsWithTotal selectListWithTotal(String stmt, RecordMapper mapper) {
        List<Record> l = session.selectList(stmt);
        int total = getTotalRows(session);
        Records recs = mapRecords(l, mapper);
        return new RecordsWithTotal(recs, total);
    }

    public RecordsWithTotal selectListWithTotal(String stmt, Object param, RecordMapper mapper) {
        List<Record> l = session.selectList(stmt, param);
        int total = getTotalRows(session);
        Records recs = mapRecords(l, mapper);
        return new RecordsWithTotal(recs, total);
    }

    private static int getTotalRows(SqlSession session) {
        Statement st0 = null;
        ResultSet rs0 = null;
        try {
            st0 = session.getConnection().createStatement();
            rs0 = st0.executeQuery("SELECT FOUND_ROWS();");
            if (rs0.next()) {
                return rs0.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Get total result number error", e);
        } finally {
            closeQuietly(rs0);
            closeQuietly(st0);
        }
    }

    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static void closeQuietly(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private Object getValue(Record rec, Object def) {
        if (rec == null || rec.isEmpty()) {
            return def;
        } else {
            return rec.values().iterator().next();
        }
    }

    public Object selectValue(String stmt, Object def) {
        Record rec = selectOne(stmt);
        return getValue(rec, def);
    }

    public Object selectValue(String stmt, Object param, Object def) {
        Record rec = selectOne(stmt, param);
        return getValue(rec, def);
    }

    public int selectIntValue(String stmt, int def) {
        return PrimitiveTypeConverter.toInt(selectValue(stmt, def), def);
    }

    public int selectIntValue(String stmt, Object param, int def) {
        return PrimitiveTypeConverter.toInt(selectValue(stmt, param, def), def);
    }

    public long selectLongValue(String stmt, long def) {
        return PrimitiveTypeConverter.toLong(selectValue(stmt, def), def);
    }

    public long selectLongValue(String stmt, Object param, long def) {
        return PrimitiveTypeConverter.toLong(selectValue(stmt, param, def), def);
    }

    public boolean selectBooleanValue(String stmt, boolean def) {
        return PrimitiveTypeConverter.toBoolean(selectValue(stmt, def), def);
    }

    public boolean selectBooleanValue(String stmt, Object param, boolean def) {
        return PrimitiveTypeConverter.toBoolean(selectValue(stmt, param, def), def);
    }

    public String selectStringValue(String stmt, String def) {
        return PrimitiveTypeConverter.toStr(selectValue(stmt, def));
    }

    public String selectStringValue(String stmt, Object param, String def) {
        return PrimitiveTypeConverter.toStr(selectValue(stmt, param, def));
    }

    public int insert(String stmt) {
        return session.insert(stmt);
    }

    public int insert(String stmt, Object param) {
        return session.insert(stmt, param);
    }

    public int update(String stmt) {
        return session.update(stmt);
    }

    public int update(String stmt, Object param) {
        return session.update(stmt, param);
    }

    public int delete(String stmt) {
        return session.delete(stmt);
    }

    public int delete(String stmt, Object param) {
        return session.delete(stmt, param);
    }

    @Override
    public void close() {
        session.close();
    }
}
