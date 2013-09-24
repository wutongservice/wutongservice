package com.borqs.server.migrate;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.ClassUtils2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public abstract class Migration extends ConfigurableBase {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();

    protected Migration() {
    }

    public abstract void migrate();

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public String getOldQiupuDb() {
        return getConfig().checkGetString("qiupu.old.db");
    }

    public String getNewQiupuDb() {
        return getConfig().checkGetString("qiupu.new.db");
    }

    public String getOldQiupuDataDir() {
        return getConfig().checkGetString("qiupu.old.dataDir");
    }

    public String getNewQiupuDataDir() {
        return getConfig().checkGetString("qiupu.new.dataDir");
    }

    public String getOldPlatformDb() {
        return getConfig().checkGetString("platform.old.db");
    }

    public String getNewPlatformDb() {
        return getConfig().checkGetString("platform.new.db");
    }

    public String getNewPlatformImageDir(){
        return getConfig().checkGetString("platform.new.imageDir");
    }

    public boolean getCopyErrorResume() {
        return getConfig().getBoolean("copy.errorResume", false);
    }

    public static Migration createMigration(Configuration conf, Class<? extends Migration> migrateClass) {
        Validate.notNull(conf);
        Validate.notNull(migrateClass);
        Migration migrate = (Migration)ClassUtils2.newInstance(migrateClass);
        migrate.setConfig(conf);
        return migrate;
    }

    public static String trimPath(String path) {
        final String OLD_HTTP_PREFIX = "http://cloud.borqs.com/borqsusercenter";
        final String OLD_HTTP_PREFIX2 = "http://apps.borqs.com/borqsusercenter";
        path = StringUtils.removeStart(path, OLD_HTTP_PREFIX);
        return StringUtils.removeStart(path, OLD_HTTP_PREFIX2);
    }
}
