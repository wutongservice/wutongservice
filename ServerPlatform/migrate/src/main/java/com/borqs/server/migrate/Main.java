package com.borqs.server.migrate;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.migrate.platform.*;
import com.borqs.server.migrate.qiupu.ApkHistoryMigration;
import com.borqs.server.migrate.qiupu.ApkMigration;
import com.borqs.server.migrate.qiupu.UserAppMigration;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Configuration conf = Configuration.loadArgs(args).expandMacros();

        if (!conf.containsKey("migrations")) {
            System.out.println("Need migrations argument");
            return;
        }

        String migrateActions = conf.getString("migrations", "");
        if (migrateActions.equalsIgnoreCase("all"))
            migrateActions = "user, friend, stream, comment, like, apk, userApp, apkHistory";
        Set<String> migrations = StringUtils2.splitSet(migrateActions, ",", true);

        if (migrations.contains("user")) {
            System.out.println("migrate user");
            Migration.createMigration(conf, UserMigration.class).migrate();
        }

        if (migrations.contains("friend")) {
            System.out.println("migrate friend");
            Migration.createMigration(conf, FriendMigration.class).migrate();
        }
        if (migrations.contains("stream")) {
            System.out.println("migrate stream");
            Migration.createMigration(conf, StreamMigration.class).migrate();
        }

        if (migrations.contains("comment")) {
            System.out.println("migrate comment");
            Migration.createMigration(conf, CommentMigration.class).migrate();
        }

        if (migrations.contains("like")) {
            System.out.println("migrate like");
            Migration.createMigration(conf, LikeMigration.class).migrate();
        }

        if (migrations.contains("apk")) {
            System.out.println("migrate apk");
            Migration.createMigration(conf, ApkMigration.class).migrate();
        }

        if (migrations.contains("userApp")) {
            System.out.println("migrate userApp");
            Migration.createMigration(conf, UserAppMigration.class).migrate();
        }

        if (migrations.contains("apkHistory")) {
            System.out.println("migrate apkHistory");
            Migration.createMigration(conf, ApkHistoryMigration.class).migrate();
        }
    }

}
