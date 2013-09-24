package com.borqs.server.platform.app;


import com.borqs.server.base.data.Schema;

import static com.borqs.server.base.data.DataType.INT;
import static com.borqs.server.base.data.DataType.STRING;

public class SApplication extends Schema {
    public static final SApplication INSTANCE = new SApplication();
    private SApplication() {
        super("application");

        addColumn("app_id", INT);
        addColumn("name", STRING);
        addColumn("secret", STRING);
        addColumn("description", STRING);
        addColumn("author", STRING);
    }
}
