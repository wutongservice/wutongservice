package com.borqs.server.test.stream.test1;

import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.RandomHelper;

import java.util.HashSet;
import java.util.Set;

public class StreamLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(StreamDb.class);
    }

    private StreamLogic getPostLogic() {
        return (StreamLogic) getBean("logic.stream");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreateStream() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();

        streamLogic.createPost(ctx, post);
        //Post postSave = streamLogic.getPost(ctx, 234121l);

        //assertEquals(post.getMessage(), postSave.getMessage());
        //assertEquals(post.getAddTo(), postSave.getAddTo());

    }


    public void testDestroyedPosts() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);

        long[] longs = {postSave.getPostId()};
        boolean b = streamLogic.destroyPosts(ctx, longs);

        assertEquals(b, true);
    }

    public void testUpdatePost() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);
        postSave.setMessage("ddddddsssssssssssss");
        streamLogic.updatePost(ctx, postSave);
    }

    public void testHasPost() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post post2 = getStream();
        post2.setPostId(RandomHelper.generateId());
        Post post3 = getStream();
        post3.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);
        Post postSave2 = streamLogic.createPost(ctx, post2);
        Post postSave3 = streamLogic.createPost(ctx, post3);
        boolean b = streamLogic.hasPost(ctx, postSave.getPostId());
        assertEquals(b, true);
    }

    public void testHasAllPosts() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post post2 = getStream();
        post2.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);
        Post postSave2 = streamLogic.createPost(ctx, post2);
        long[] l = {postSave.getPostId(), postSave2.getPostId()};
        boolean b = streamLogic.hasAllPosts(ctx, l);

        assertEquals(b, true);
    }

    public void testHasAnyPosts() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post post2 = getStream();
        post2.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);
        Post postSave2 = streamLogic.createPost(ctx, post2);
        long[] l = {postSave.getPostId(), postSave2.getPostId(), 234234l};

        boolean b = streamLogic.hasAnyPosts(ctx, l);

        assertEquals(b, true);
    }

    public void testGetPosts() {
        StreamLogic streamLogic = this.getPostLogic();
        Post post = getStream();
        post.setPostId(RandomHelper.generateId());
        Post post2 = getStream();
        post2.setPostId(RandomHelper.generateId());
        Post postSave = streamLogic.createPost(ctx, post);
        Post postSave2 = streamLogic.createPost(ctx, post2);
        long[] l = {postSave.getPostId(), postSave2.getPostId(), 234234l};

        Posts posts = streamLogic.getPosts(ctx, Post.FULL_COLUMNS, l);
        assertEquals(posts.size(), 2);

    }

    public void testUserTimeLine() {
        StreamLogic streamLogic = this.getPostLogic();
        PostFilter postFilter = new PostFilter(Post.ALL_POST_TYPES, 1, 0, 2790800528702898528l, null);

        Page page = new Page();
        page.count = 20;
        page.page = 0;
        streamLogic.getUserTimeline(ctx, 10001, null, Post.FULL_COLUMNS, page);
    }

    public void testFriendTimeLine() {
        StreamLogic streamLogic = this.getPostLogic();
        Set<PeopleId> peopleIds = new HashSet<PeopleId>();
        PeopleId peopleId = new PeopleId(PeopleId.USER,"10001");
        PeopleId peopleId2 = new PeopleId(PeopleId.USER,"10002");
        PeopleId peopleId3 = new PeopleId(PeopleId.USER,"10003");
        PeopleId peopleId4 = new PeopleId(PeopleId.USER,"10004");
        PeopleId peopleId5= new PeopleId(PeopleId.USER,"10005");

        peopleIds.add(peopleId);
        peopleIds.add(peopleId2);
        peopleIds.add(peopleId3);
        peopleIds.add(peopleId4);
        peopleIds.add(peopleId5);

        PostFilter postFilter = new PostFilter(Post.ALL_POST_TYPES, 1, 0, 0, peopleIds);

        Page page = new Page();
        page.count = 20;
        page.page = 0;
        streamLogic.getFriendsTimeline(ctx, 10001, postFilter, Post.FULL_COLUMNS, page);
    }

    private Post getStream() {
        Post post = new Post();
        post.setPostId(RandomHelper.generateId());
        post.setSourceId(ctx.getViewer());
        post.setTo(PeopleIds.forUserIds(SteveAndBill.BILL_ID, SteveAndBill.STEVE_ID));
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
