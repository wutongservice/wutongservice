package com.borqs.server.platform.sql;


import com.borqs.server.platform.util.VfsHelper;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBSchemaBuilder {

    private final List<Script> scripts = new ArrayList<Script>();

    public DBSchemaBuilder() {
    }

    public DBSchemaBuilder(Script... scripts) {
        Collections.addAll(this.scripts, scripts);
    }

    public DBSchemaBuilder(List<Script> scripts) {
        if (CollectionUtils.isNotEmpty(scripts))
            this.scripts.addAll(scripts);
    }

    public List<Script> getScripts() {
        return Collections.unmodifiableList(scripts);
    }

    public void setScripts(List<Script> scripts) {
        this.scripts.clear();
        if (CollectionUtils.isNotEmpty(scripts))
            this.scripts.addAll(scripts);
    }

    public void build(String db, boolean rebuild) {
        if (rebuild)
            clear(db);

        for (Script script : scripts)
            SqlExecutor.executeSource(db, script.sqlCreate);
    }

    public void clear(String db) {
        for (Script script : scripts)
            SqlExecutor.executeSource(db, script.sqlDrop);
    }

    public static Script scriptInFile(String sqlCreatePath, String sqlDropPath) {
        try {
            return new Script(VfsHelper.loadText(sqlCreatePath), VfsHelper.loadText(sqlDropPath));
        } catch (Exception e) {
            return new Script("", "");
        }
    }

    public static Script scriptInPackage(Package pkg, String sqlCreateFile, String sqlDropFile) {
        return scriptInFile(VfsHelper.packageFileToPath(pkg, sqlCreateFile), VfsHelper.packageFileToPath(pkg, sqlDropFile));
    }

    public static Script scriptInClasspath(Class clazz, String sqlCreateFile, String sqlDropFile) {
        return scriptInFile(VfsHelper.classpathFileToPath(clazz, sqlCreateFile), VfsHelper.classpathFileToPath(clazz, sqlDropFile));
    }

    public static class Script {
        public final String sqlCreate;
        public final String sqlDrop;

        public Script(String sqlCreate, String sqlDrop) {
            this.sqlCreate = sqlCreate;
            this.sqlDrop = sqlDrop;
        }
    }
}
