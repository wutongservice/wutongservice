package com.borqs.server.platform.fts.simple;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.fts.AbstractSingleCategoryFts;
import com.borqs.server.platform.fts.FTDoc;
import com.borqs.server.platform.fts.FTResult;
import com.borqs.server.platform.sql.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SingleCategorySimpleFts extends AbstractSingleCategoryFts {

    private Db db = new Db();

    public SingleCategorySimpleFts(String category) {
        super(category);
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getFtIndexTable() {
        return db.getFtIndexTable();
    }

    public void setFtIndexTable(Table ftIndexTable) {
        db.setFtIndexTable(ftIndexTable);
    }

    @Override
    public void saveDoc(Context ctx, FTDoc... docs) {
        checkDocCategory(docs);
        if (ArrayUtils.isNotEmpty(docs))
            db.saveDoc(ctx, docs);
    }

    @Override
    public void deleteDoc(Context ctx, FTDoc... docs) {
        checkDocCategory(docs);
        if (ArrayUtils.isNotEmpty(docs))
            db.deleteDoc(ctx, docs);
    }

    @Override
    public void addFields(Context ctx, FTDoc... docs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFields(Context ctx, FTDoc... docs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFields(Context ctx, FTDoc... docs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FTResult search(Context ctx, FTResult reuse, String category, String word, Options opts, int count) {
        checkCategory(category);
        return db.search(ctx, reuse, word, opts, count);
    }

    private static class SqlHelper {
        public static List<String> saveDoc(String table, FTDoc[] docs) {
            ArrayList<String> sqls = new ArrayList<String>();
            sqls.addAll(deleteDoc(table, docs));
            for (FTDoc doc : docs) {
                if (doc == null)
                    continue;

                String id = doc.getId();
                if (StringUtils.isEmpty(id))
                    continue;

                Map<String, String> m = doc.getOriginalContents();
                if (MapUtils.isNotEmpty(m)) {
                    for (Map.Entry<String, String> e : m.entrySet()) {
                        String orgContent = ObjectUtils.toString(e.getValue());
                        String ftContent = ObjectUtils.toString(doc.getFulltextContent(e.getKey()));

                        sqls.add(new Sql()
                                .insertInto(table)
                                .values(
                                        Sql.value("id", id),
                                        Sql.value("field", ObjectUtils.toString(e.getKey())),
                                        Sql.value("org_content", orgContent),
                                        Sql.value("ft_content", ftContent),
                                        Sql.value("weight", doc.getWeight())
                                ).toString());
                    }
                }
            }
            return sqls;
        }


        public static List<String> deleteDoc(String table, FTDoc[] docs) {
            ArrayList<String> sqls = new ArrayList<String>();
            for (FTDoc doc : docs) {
                if (doc == null)
                    continue;

                String id = doc.getId();
                if (StringUtils.isNotEmpty(id)) {
                    sqls.add(new Sql().deleteFrom(table).where("id=:id", "id", id).toString());
                }
            }
            return sqls;
        }

        public static String search(String table, String word, Options opts, int count) {
            Sql sql = new Sql();
            String[] inIds = opts.getInIds();
            int method = opts.getMethod();
            if (method == Options.METHOD_EQUALS) {
                if (ArrayUtils.isEmpty(inIds)) {
                    sql.select("*")
                            .from(table)
                            .where("org_content=:content ", "content", word);
                } else {
                    sql.select("*")
                            .from(table)
                            .where("id IN ($ids)", "ids", Sql.joinSqlValues(inIds, ","))
                            .and("org_content=:content ", "content", word);

                }
            } else if (method == Options.METHOD_LIKE) {
                if (ArrayUtils.isEmpty(inIds)) {
                    sql.select("*")
                            .from(table)
                            .where("org_content LIKE :content ", "content", "%" + word + "%");

                } else {
                    sql.select("*")
                            .from(table)
                            .where("id IN ($ids)", "ids", Sql.joinSqlValues(inIds, ","))
                            .and("org_content LIKE :content ", "content", "%" + word + "%");

                }
            } else if (method == Options.METHOD_FT_MATCH) {
                if (ArrayUtils.isEmpty(inIds)) {
                    sql.select("*")
                            .from(table)
                            .where("MATCH(ft_content) AGAINST(:content IN BOOLEAN MODE)", "content", word);

                } else {
                    sql.select("*")
                            .from(table)
                            .where("id IN ($ids)", "ids", Sql.joinSqlValues(inIds, ","))
                            .and("MATCH(ft_content) AGAINST(:content IN BOOLEAN MODE)", "content", word);

                }
            } else {
                throw new IllegalArgumentException();
            }
            String[] inFields = opts.getInFields();
            if (ArrayUtils.isNotEmpty(inFields)) {
                sql.and("field IN ($fields)", "fields", Sql.joinSqlValues(inFields, ","));
            }
            sql.orderBy("weight", "DESC").limit(count);
            return sql.toString();
        }
    }

    private class Db extends SqlSupport {

        private Table ftIndexTable;

        private Db() {
        }


        public Table getFtIndexTable() {
            return ftIndexTable;
        }

        public void setFtIndexTable(Table ftIndexTable) {
            if (ftIndexTable != null && ftIndexTable.getShardCount() != 1)
                throw new IllegalArgumentException("Not support shard table");
            this.ftIndexTable = ftIndexTable;
        }

        private ShardResult shard() {
            return ftIndexTable.getShard(0);
        }

        public void saveDoc(final Context ctx, final FTDoc[] docs) {
            final ShardResult sr = shard();
            sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    List<String> sqls = SqlHelper.saveDoc(sr.table, docs);
                    SqlExecutor.executeUpdate(ctx, conn, sqls);
                    return null;
                }
            });
        }

        public void deleteDoc(final Context ctx, final FTDoc[] docs) {
            final ShardResult sr = shard();
            sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    List<String> sqls = SqlHelper.deleteDoc(sr.table, docs);
                    SqlExecutor.executeUpdate(ctx, conn, sqls);
                    return null;
                }
            });
        }

        public FTResult search(final Context ctx, final FTResult reuse, final String word, final Options opts, final int count) {
            final ShardResult sr = shard();
            final FTResult result = reuse != null ? reuse : new FTResult();
            sqlExecutor.openConnection(sr.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = SqlHelper.search(sr.table, word, opts, count);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            FTRs.readFTResult(rs, result, category, opts, count);
                        }
                    });
                    return null;
                }
            });
            return result;
        }
    }

    private static class FTRs {

        public static FTResult readFTResult(ResultSet rs, FTResult reuse, String category, Options opts, int count) throws SQLException {
            if (reuse == null)
                reuse = new FTResult();

            double incrWeight = opts.getIncrWeight();
            while (rs.next()) {
                double weight = rs.getDouble("weight") + incrWeight;
                reuse.addEntry(category, rs.getString("id"), weight, rs.getString("field"), rs.getString("org_content"));
                if (reuse.size() > count)
                    break;
            }

            return reuse;
        }
    }
}
