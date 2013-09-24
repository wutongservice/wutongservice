package com.borqs.server.test.pubapi.test1.photo.stream;

import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.web.AbstractHttpClient;

public class PhotoApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(StreamDb.class);
    }

    private StreamLogic getPostLogic() {
        return (StreamLogic) getBean("logic.stream");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testHasPost() {

        Post postSave = getPostLogic().createPost(ctx, null);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/v2/album/create", new Object[][]{
                {"postId", postSave.getPostId()}
        });
    }


}
