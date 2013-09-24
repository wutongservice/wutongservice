package com.borqs.server.test.pubapi.test1.stream;

import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.web.AbstractHttpClient;

import java.util.ArrayList;
import java.util.List;

public class StreamApiTest1 extends ServletTestCase {
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

    public void testCommentCreate() {
        List<Post> list = new ArrayList<Post>();

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/post/create", new Object[][]{
                {"message", "ssssssssssssss"}
        });
    }

    public void testHasPost() {
        Post post = getStream();

        Post postSave = getPostLogic().createPost(ctx, post);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/post/hasPost", new Object[][]{
                {"postId", postSave.getPostId()}
        });
    }

    public void testHasAllPost() {
        Post post = getStream();
        Post post2 = getStream();

        Post postSave = getPostLogic().createPost(ctx, post);
        Post postSave2 = getPostLogic().createPost(ctx, post2);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        String longs = StringHelper.join(new long[]{postSave.getPostId(), postSave2.getPostId()}, ",");
        AbstractHttpClient.Response resp = client.get(PUB_API + "/post/hasAllPosts", new Object[][]{
                {"postIds", longs}
        });
    }

    public void testGetPost() {
        Post post = getStream();

        Post postSave = getPostLogic().createPost(ctx, post);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/post/getPost", new Object[][]{
                {"postId", postSave.getPostId()}
        });
    }

    public void testGetPosts() {
        Post post = getStream();
        Post post2 = getStream();

        Post postSave = getPostLogic().createPost(ctx, post);
        Post postSave2 = getPostLogic().createPost(ctx, post2);

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        String longs = StringHelper.join(new long[]{postSave.getPostId(), postSave2.getPostId()}, ",");
        AbstractHttpClient.Response resp = client.get(PUB_API + "/post/getPosts", new Object[][]{
                {"postIds", longs}
        });
    }


    private Post getStream() {
        Post post = new Post();
        post.setPostId(RandomHelper.generateId());
        post.setSourceId(ctx.getViewer());
        post.setTo(PeopleIds.forUserIds(SteveAndBill.BILL_ID));
        post.setAddTo(PeopleIds.forUserIds(SteveAndBill.BILL_ID, SteveAndBill.STEVE_ID));
        post.setApp(1);
        post.setType(2);
        post.setCanLike(true);
        post.setCanComment(true);
        post.setCanQuote(true);

        post.setGeoLocation(new GeoLocation(21.23, 10.12));
        post.setPrivate(true);
        post.setDestroyedTime(0);
        post.setDevice("device1");
        post.setMessage("dddd");
        return post;
    }
}
