package com.borqs.server.impl.migration.comment;

import com.borqs.server.platform.sql.Sql;

public class CommentMigSql {

  public static String getComment(String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" destroyed_time = 0 ").toString();
    }
     public static String findAllCommentIds( String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" destroyed_time = 0 ").toString();
    }
}
