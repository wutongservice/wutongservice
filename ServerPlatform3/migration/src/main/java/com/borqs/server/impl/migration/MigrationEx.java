package com.borqs.server.impl.migration;


import com.borqs.server.impl.account.UserDb;
import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;

public class MigrationEx {
    private static final Logger L = Logger.get(MigrationEx.class);
    // db
    private final MigrationDb db_migration = new MigrationDb();
    private final StreamDb db = new StreamDb();
    private final UserDb db_account = new UserDb();

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
        db_account.setSqlExecutor(sqlExecutor);
    }

    public Table getPostTable() {
        return db.getPostTable();
    }

    public void setPostTable(Table postTable) {
        db.setPostTable(postTable);
    }

    public void setUserTable(Table userTable) {
        db_account.setUserTable(userTable);
    }
    public void setUserPropertyTable(Table userPropertyTable) {
        db_account.setPropertyTable(userPropertyTable);
    }


    public Table getStreamTable() {
        return db_migration.getStreamTable();
    }

    public void setFriendTable(Table friendTable) {
        db_migration.setFriendTable(friendTable);
    }

    public void setCircleTable(Table circleTable) {
        db_migration.setCircleTable(circleTable);
    }

    public void setStreamTable(Table streamTable) {
        db_migration.setStreamTable(streamTable);
    }

    public void setCircleOldTable(Table circleTable) {
        db_migration.setCircleOldTable(circleTable);
    }

    public void setFriendOldTable(Table friendTable) {
        db_migration.setFirendOldTable(friendTable);
    }

    public void setFollowerTable(Table friendTable) {
        db_migration.setFollowerTable(friendTable);
    }

    public void setAccountTable(Table accountTable) {
        db_migration.setAccountTable(accountTable);
    }


    public boolean streamMigration(Context ctx) {
        Posts posts = null;
        try {
            posts = db_migration.getPost(ctx);
            for (Post post : posts) {
                db.createStream(ctx, post);
            }
            return true;
        } catch (RuntimeException e) {

            long[] postIds = new long[posts.size()];
            int i = 0;
            for (long l : postIds) {
                postIds[i] = posts.get(i++).getPostId();
            }
            db.destroyPosts(ctx, postIds);
            throw e;
        }

    }

    public boolean friendMigration(Context ctx) {
        try {
            db_migration.friendMigration(ctx);
            return true;
        } catch (RuntimeException e) {

            throw e;
        }
    }

    public boolean circleMigration(Context ctx) {
        try {
            db_migration.circleMigration(ctx);
            return true;
        } catch (RuntimeException e) {

            throw e;
        }
    }

    public void accountMigration(Context ctx) {
        List<User> list = db_migration.getAccounts(ctx);
        List<User> errorList = new ArrayList<User>();
        for (User user : list) {
            try {

                db_account.createUserMigration(ctx, user);

            } catch (Exception e) {
                errorList.add(user);
            }
        }
        System.out.println("-------------------------------------");
        System.out.println("------------------"+errorList.toString()+"-------------------");
        System.out.println("-------------------------------------");
    }
}
