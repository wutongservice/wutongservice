package com.borqs.server.test.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.timeline.*;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.ThreadHelper;

public class TimelineTest extends ConfigurableTestCase {

    private OutboxTimeline getOutboxTimeline() {
        return (OutboxTimeline)getBean("timeline.outbox");
    }

    private WallTimeline getWallTimeline() {
        return (WallTimeline)getBean("timeline.wall");
    }

    private FriendsTimeline getFriendsTimeline() {
        return (FriendsTimeline)getBean("timeline.friends");
    }

    public void testOutboxTimeline() {
        OutboxTimeline outbox = getOutboxTimeline();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);
        PeopleId user = PeopleId.user(ctx.getViewer());

        TimelineEntry entry1 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry2 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry3 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_MUSIC, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry4 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry5 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);

        // remove timeline
        outbox.removeTimeline(ctx, user);

        // add entries
        outbox.add(ctx, user, entry1);
        outbox.add(ctx, user, entry2);
        outbox.add(ctx, user, entry3);
        outbox.add(ctx, user, entry4);
        outbox.add(ctx, user, entry5);

        // get full
        TimelineResult tr  = outbox.get(ctx, user, PostFilter.newEmpty(), new Page(0, 100));
        assertEquals(5, tr.total);
        assertEquals(5, tr.timeline.size());
        assertEquals(entry5, tr.timeline.get(0));
        assertEquals(entry4, tr.timeline.get(1));
        assertEquals(entry3, tr.timeline.get(2));
        assertEquals(entry2, tr.timeline.get(3));
        assertEquals(entry1, tr.timeline.get(4));

        // get page
        tr = outbox.get(ctx, user, PostFilter.newEmpty(), new Page(1, 2));
        assertEquals(5, tr.total);
        assertEquals(2, tr.timeline.size());
        assertEquals(entry3, tr.timeline.get(0));
        assertEquals(entry2, tr.timeline.get(1));
    }

    public void testWallTimeline() {
        WallTimeline wall = getWallTimeline();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);
        PeopleId user = PeopleId.user(ctx.getViewer());

        TimelineEntry entry1 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry2 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry3 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_MUSIC, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry4 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry5 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry6 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);

        // remove timeline
        wall.removeTimeline(ctx, user);

        // add entries
        wall.add(ctx, user, entry1);
        wall.add(ctx, user, entry2);
        wall.add(ctx, user, entry3);
        wall.add(ctx, user, entry4);
        wall.add(ctx, user, entry5);
        wall.add(ctx, user, entry6);

        // get full
        TimelineResult tr  = wall.get(ctx, user, PostFilter.newEmpty(), new Page(0, 100));
        assertEquals(5, tr.total);
        assertEquals(5, tr.timeline.size());
        assertEquals(entry6, tr.timeline.get(0));
        assertEquals(entry5, tr.timeline.get(1));
        assertEquals(entry4, tr.timeline.get(2));
        assertEquals(entry3, tr.timeline.get(3));
        assertEquals(entry2, tr.timeline.get(4));

        // get page
        tr = wall.get(ctx, user, PostFilter.newEmpty(), new Page(1, 2));
        assertEquals(5, tr.total);
        assertEquals(2, tr.timeline.size());
        assertEquals(entry4, tr.timeline.get(0));
        assertEquals(entry3, tr.timeline.get(1));
    }

    public void testFriendsTimeline() {
        OutboxTimeline outbox = getOutboxTimeline();


        Context ctxGrx = Context.createForViewer(ServerTeam.GRX_ID);
        Context ctxWp = Context.createForViewer(ServerTeam.WP_ID);
        Context ctxJcs = Context.createForViewer(ServerTeam.JCS_ID);

        PeopleId grx = PeopleId.user(ServerTeam.GRX_ID);
        PeopleId wp = PeopleId.user(ServerTeam.WP_ID);
        PeopleId jcs = PeopleId.user(ServerTeam.JCS_ID);

        outbox.removeTimeline(ctxGrx, grx);
        outbox.removeTimeline(ctxWp, wp);
        outbox.removeTimeline(ctxJcs, jcs);

        TimelineEntry entry1 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry2 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry3 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_MUSIC, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry4 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry5 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry6 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry7 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry8 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_MUSIC, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry9 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_PHOTO, (short)0);
        ThreadHelper.sleepSilent(100);
        TimelineEntry entry10 = new TimelineEntry(RandomHelper.generateId(), TestApp.APP2_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);

        outbox.add(ctxGrx, grx, entry1);
        outbox.add(ctxWp, wp, entry2);
        outbox.add(ctxWp, wp, entry3);
        outbox.add(ctxJcs, jcs, entry4);
        outbox.add(ctxGrx, grx, entry5);
        outbox.add(ctxGrx, grx, entry6);
        outbox.add(ctxJcs, jcs, entry7);
        outbox.add(ctxWp, wp, entry8);
        outbox.add(ctxGrx, grx, entry9);
        outbox.add(ctxJcs, jcs, entry10);

        // test all
        FriendsTimeline friendsTimeline = getFriendsTimeline();
        TimelineResult tr = friendsTimeline.get(ctxGrx, grx, PostFilter.newEmptyForFriends(grx, jcs, wp), new Page(0, 100));
        assertEquals(9, tr.total);
        assertEquals(entry10, tr.timeline.get(0));
        assertEquals(entry9, tr.timeline.get(1));
        assertEquals(entry8, tr.timeline.get(2));
        assertEquals(entry7, tr.timeline.get(3));
        assertEquals(entry6, tr.timeline.get(4));
        assertEquals(entry5, tr.timeline.get(5));
        assertEquals(entry4, tr.timeline.get(6));
        assertEquals(entry3, tr.timeline.get(7));
        assertEquals(entry2, tr.timeline.get(8));

        // test page
        friendsTimeline = getFriendsTimeline();
        tr = friendsTimeline.get(ctxGrx, grx, PostFilter.newEmptyForFriends(grx, jcs, wp), new Page(3, 2));
        assertEquals(9, tr.total);
        assertEquals(entry4, tr.timeline.get(0));
        assertEquals(entry3, tr.timeline.get(1));


        // test agg2
        friendsTimeline = getFriendsTimeline();
        tr = friendsTimeline.get(ctxGrx, grx, PostFilter.newEmptyForFriends(grx, jcs), new Page(0, 100));
        assertEquals(7, tr.total);
        assertEquals(entry10, tr.timeline.get(0));
        assertEquals(entry9, tr.timeline.get(1));
        assertEquals(entry7, tr.timeline.get(2));
        assertEquals(entry6, tr.timeline.get(3));
        assertEquals(entry5, tr.timeline.get(4));
        assertEquals(entry4, tr.timeline.get(5));
        assertEquals(entry1, tr.timeline.get(6));

        // test agg1
        friendsTimeline = getFriendsTimeline();
        tr = friendsTimeline.get(ctxGrx, grx, PostFilter.newEmptyForFriends(wp), new Page(0, 100));
        assertEquals(3, tr.total);
        assertEquals(entry8, tr.timeline.get(0));
        assertEquals(entry3, tr.timeline.get(1));
        assertEquals(entry2, tr.timeline.get(2));
    }


    public void testFilterIgnorePrivateFlag() {
        long min = RandomHelper.generateId();
        ThreadHelper.sleepSilent(100);

        TimelineEntry entry = new TimelineEntry(RandomHelper.generateId(), TestApp.APP1_ID, Post.POST_TEXT, (short)0);
        ThreadHelper.sleepSilent(100);

        long max = RandomHelper.generateId();

        PostFilter filter = PostFilter.newEmpty();
        assertTrue(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(Post.POST_BOOK | Post.POST_TEXT, App.APP_NONE, 0, 0, null);
        assertTrue(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(Post.POST_BOOK, App.APP_NONE, 0, 0, null);
        assertFalse(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(0, TestApp.APP1_ID, 0, 0, null);
        assertTrue(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(0, TestApp.APP2_ID, 0, 0, null);
        assertFalse(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(0, App.APP_NONE, 0, min, null);
        assertFalse(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(0, App.APP_NONE, max, 0, null);
        assertFalse(StreamTimeline.filterIgnorePrivateFlag(entry, filter));

        filter = new PostFilter(0, App.APP_NONE, min, max, null);
        assertTrue(StreamTimeline.filterIgnorePrivateFlag(entry, filter));
    }
}
