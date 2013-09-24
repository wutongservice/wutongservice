package com.borqs.server.dist;


import com.borqs.server.impl.account.AccountImpl;
import com.borqs.server.impl.cibind.CibindDb;
import com.borqs.server.impl.comment.CommentDb;
import com.borqs.server.impl.contact.ContactDb;
import com.borqs.server.impl.conversation.ConversationDb;
import com.borqs.server.impl.friend.FriendDb;
import com.borqs.server.impl.ignore.IgnoreDb;
import com.borqs.server.impl.login.TicketDb;
import com.borqs.server.impl.opline.OpLineDb;
import com.borqs.server.impl.privacy.PrivacyDb;
import com.borqs.server.impl.psuggest.PeopleSuggestDb;
import com.borqs.server.impl.request.RequestDb;
import com.borqs.server.impl.setting.SettingDb;
import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;

public class BuildDb {

    private static final Class<?>[] IMPL_CLASSES = {
            AccountImpl.class,
            CibindDb.class,
            CommentDb.class,
            ContactDb.class,
            ConversationDb.class,
            FriendDb.class,
            IgnoreDb.class,
            TicketDb.class,
            PrivacyDb.class,
            PeopleSuggestDb.class,
            SettingDb.class,
            StreamDb.class,
            RequestDb.class,
            OpLineDb.class,
    };

    public static void main(String[] args) {
        final String USAGE = String.format("java -cp ... %s build|rebuild|clear jdbc:mysql://your_db_url", BuildDb.class.getName());

        if (args.length < 2) {
            System.err.println(USAGE);
            return;
        }

        String action = args[0];
        if (!ArrayUtils.contains(new String[]{"build", "rebuild", "clear"}, action)) {
            System.err.println(USAGE);
            return;
        }

        String db = args[1];
        ArrayList<DBSchemaBuilder.Script> scripts = new ArrayList<DBSchemaBuilder.Script>();
        for (Class clazz : IMPL_CLASSES)
            scripts.add(DBSchemaBuilder.scriptInClasspath(clazz, "build.sql", "clear.sql"));

        DBSchemaBuilder builder = new DBSchemaBuilder(scripts);
        if (action.equals("build")) {
            builder.build(db, false);
        } else if (action.equals("rebuild")) {
            builder.build(db, true);
        } else if (action.equals("clear")) {
            builder.clear(db);
        }
    }
}
