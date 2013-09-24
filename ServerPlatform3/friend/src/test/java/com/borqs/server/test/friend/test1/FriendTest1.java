package com.borqs.server.test.friend.test1;


import com.borqs.server.ServerException;
import com.borqs.server.impl.friend.FriendDb;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.util.ArrayHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.Map;

public class FriendTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(FriendDb.class);
    }

    private FriendLogic getFriend() {
        return (FriendLogic) getBean("logic.friend");
    }

    private long[] getAllUserIds() {
        return ((TestAccount) getBean("logic.account")).getAllUserIds();
    }

    private long getFirstUserId() {
        long[] usersIds = getAllUserIds();
        return ArrayUtils.isNotEmpty(usersIds) ? usersIds[0] : 0L;
    }


    private static void assertCircleIsBuiltinCircles(Circles circles) {
        assertTrue(ArrayHelper.equalsAsSet(Circle.BUILTIN_ACTUAL_CIRCLES, circles.getCircleIds()));
    }

    public void testDefaultCircle() {
        FriendLogic friend = getFriend();
        Context ctx = Context.createForViewer(getFirstUserId());

        assertCircleIsBuiltinCircles(friend.getCircles(ctx, ctx.getViewer(), null, false));
    }

    public void testCreateAndDestroyCircle() {
        FriendLogic friend = getFriend();
        Context ctx = Context.createForViewer(getFirstUserId());

        // Test create
        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");
        Circle circle2 = friend.createCustomCircle(ctx, "Circle2");

        assertEquals(Circle.MIN_CUSTOM_CIRCLE_ID, circle1.getCircleId());
        assertEquals(Circle.MIN_CUSTOM_CIRCLE_ID + 1, circle2.getCircleId());
        assertTrue(ArrayHelper.equalsAsSet(friend.getCircles(ctx, ctx.getViewer(), null, false).getCircleIds(),
                ArrayUtils.addAll(Circle.BUILTIN_ACTUAL_CIRCLES, new int[]{circle1.getCircleId(), circle2.getCircleId()})));

        assertTrue(friend.destroyCustomCircle(ctx, circle1.getCircleId()));
        assertTrue(friend.destroyCustomCircle(ctx, circle2.getCircleId()));
        assertCircleIsBuiltinCircles(friend.getCircles(ctx, ctx.getViewer(), null, false));


        // Test create too many circles
        for (int i = 0; i < Circle.MAX_CUSTOM_CIRCLE_COUNT; i++)
            friend.createCustomCircle(ctx, "circle " + i);

        try {
            friend.createCustomCircle(ctx, "Overloaded circle");
            assertTrue(false);
        } catch (ServerException e) {
            assertEquals(E.TOO_MANY_CIRCLES, e.getCode());
        }

        for (Circle circle : friend.getCircles(ctx, ctx.getViewer(), null, false)) {
            if (Circle.isCustomCircleId(circle.getCircleId())) {
                assertTrue(friend.destroyCustomCircle(ctx, circle.getCircleId()));
            }
        }
        assertCircleIsBuiltinCircles(friend.getCircles(ctx, ctx.getViewer(), null, false));

        // Test destroy a nonexistent circle
        assertFalse(friend.destroyCustomCircle(ctx, Circle.MIN_CUSTOM_CIRCLE_ID + 100));

        // Test destroy a builtin circle
        assertFalse(friend.destroyCustomCircle(ctx, Circle.CIRCLE_BLOCKED));
    }

    public void testRenameCircle() {
        FriendLogic friend = getFriend();
        Context ctx = Context.createForViewer(getFirstUserId());

        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");
        Circle circle1a = friend.getCircles(ctx, ctx.getViewer(), null,false).getCircle(circle1.getCircleId());

        assertEquals("Circle1", circle1a.getCircleName());
        assertEquals(circle1.getCircleName(), circle1a.getCircleName());

        friend.updateCustomCircleName(ctx, circle1.getCircleId(), "Renamed");
        Circle circle1b = friend.getCircles(ctx, ctx.getViewer(), null,false).getCircle(circle1.getCircleId());
        assertEquals("Renamed", circle1b.getCircleName());
    }

    public void testHasCircles() {
        FriendLogic friend = getFriend();
        Context ctx = Context.createForViewer(getFirstUserId());

        for (int i = 0; i < 10 && i < Circle.MAX_CUSTOM_CIRCLE_COUNT; i++) {
            friend.createCustomCircle(ctx, "Circle" + i);
        }

        Circles circles = friend.getCircles(ctx, ctx.getViewer(), null, false);
        for (Circle circle : circles) {
            assertTrue(friend.hasCircle(ctx, ctx.getViewer(), circle.getCircleId()));
        }
        assertFalse(friend.hasCircle(ctx, ctx.getViewer(), Circle.MIN_CUSTOM_CIRCLE_ID + 20));
        assertTrue(friend.hasAllCircles(ctx, ctx.getViewer(), circles.getCircleIds()));
        assertTrue(friend.hasAnyCircles(ctx, ctx.getViewer(), ArrayUtils.add(circles.getCircleIds(), 220)));
    }

    public void testSetFriendIntoCircles() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];
        long userId4 = userIds[3];

        Context ctx = Context.createForViewer(userId1);

        // Test set friend into a nonexistent circle
        try {
            friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId2), Circle.MIN_CUSTOM_CIRCLE_ID);
            assertTrue(false);
        } catch (ServerException e) {
            assertEquals(E.INVALID_CIRCLE, e.getCode());
        }

        // Test set friend into some circles
        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId2), Circle.CIRCLE_DEFAULT, circle1.getCircleId());
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId3), Circle.CIRCLE_DEFAULT);
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId4), circle1.getCircleId());
        assertEquals(2, friend.getFriendCountInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT));
        assertEquals(2, friend.getFriendCountInCircles(ctx, ctx.getViewer(), circle1.getCircleId()));
        assertEquals(3, friend.getFriendCount(ctx, ctx.getViewer()));
        PeopleIds defCircleFriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT);
        PeopleIds circle1FriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), circle1.getCircleId());
        PeopleIds allFriendIds = friend.getFriends(ctx, ctx.getViewer());
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2, userId3), defCircleFriendIds));
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId4, userId2), circle1FriendIds));
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2, userId4, userId3), allFriendIds));

        // Test remove friend in all circles
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId2));
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId3));
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId4));
        defCircleFriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT);
        circle1FriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), circle1.getCircleId());
        allFriendIds = friend.getFriends(ctx, ctx.getViewer());
        assertTrue(defCircleFriendIds.isEmpty());
        assertTrue(circle1FriendIds.isEmpty());
        assertTrue(allFriendIds.isEmpty());
    }

    public void testAddFriendsIntoCircle() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];
        long userId4 = userIds[3];
        long userId5 = userIds[4];

        Context ctx = Context.createForViewer(userId1);

        // Test set friend into a nonexistent circle
        try {
            friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2, userId3), Circle.MIN_CUSTOM_CIRCLE_ID);
            assertTrue(false);
        } catch (ServerException e) {
            assertEquals(E.INVALID_CIRCLE, e.getCode());
        }

        // Test add friends into a circle
        // add 2, 3 into default circle
        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2, userId3), Circle.CIRCLE_DEFAULT);
        assertEquals(2, friend.getFriendCountInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT));
        PeopleIds defCircleFriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT);
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2, userId3), defCircleFriendIds));

        // add 2, 4 into default circle
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2, userId4), Circle.CIRCLE_DEFAULT);
        assertEquals(3, friend.getFriendCountInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT));
        defCircleFriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT);
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2, userId4, userId3), defCircleFriendIds));

        // add 3, 5 into circle1
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId3, userId5), circle1.getCircleId());
        assertEquals(2, friend.getFriendCountInCircles(ctx, ctx.getViewer(), circle1.getCircleId()));
        PeopleIds circle1FriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), circle1.getCircleId());
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId3, userId5), circle1FriendIds));

        // check all friends in circles
        assertEquals(4, friend.getFriendCount(ctx, ctx.getViewer()));
        PeopleIds allFriendIds = friend.getFriends(ctx, ctx.getViewer());
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2, userId4, userId3, userId5), allFriendIds));

        // Test remove friends from a circle
        // remove 2, 4, 5 from default circle
        friend.removeFriendsInCircle(ctx, PeopleIds.forUserIds(userId5, userId2, userId4), Circle.CIRCLE_DEFAULT);
        assertEquals(1, friend.getFriendCountInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT));
        defCircleFriendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT);
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId3), defCircleFriendIds));

        // remove 3, 5 from circle1
        friend.removeFriendsInCircle(ctx, PeopleIds.forUserIds(userId5, userId3), circle1.getCircleId());
        assertEquals(0, friend.getFriendCountInCircles(ctx, ctx.getViewer(), circle1.getCircleId()));

        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, PeopleId.user(userId3));
        assertEquals(0, friend.getFriendCountInCircles(ctx, ctx.getViewer(), Circle.CIRCLE_DEFAULT));
    }

    public void testHasFriends() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];
        long userId4 = userIds[3];
        long userId5 = userIds[4];
        long userId6 = userIds[5];

        Context ctx = Context.createForViewer(userId1);
        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");

        // Add 2, 3, 4 into default circle
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2, userId3, userId4), Circle.CIRCLE_DEFAULT);
        // Add 3, 4, 5 into circle1
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId5, userId3, userId4), circle1.getCircleId());

        // Test hasFriendInCircles
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleId.user(userId2)));
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleId.user(userId3)));
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleId.user(userId4)));
        assertFalse(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleId.user(userId5)));

        assertFalse(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleId.user(userId2)));
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleId.user(userId3)));
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleId.user(userId4)));
        assertTrue(friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleId.user(userId5)));

        // Test hasAllFriendsInCircles
        assertTrue(friend.hasAllFriendsInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleIds.forUserIds(userId2, userId3, userId4).toIdArray()));
        assertFalse(friend.hasAllFriendsInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleIds.forUserIds(userId2, userId3, userId4, userId5).toIdArray()));

        assertTrue(friend.hasAllFriendsInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleIds.forUserIds(userId5, userId3, userId4).toIdArray()));
        assertFalse(friend.hasAllFriendsInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleIds.forUserIds(userId2, userId3, userId4, userId5).toIdArray()));

        // Test hasAnyFriendsInCircles
        assertTrue(friend.hasAnyFriendsInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleIds.forUserIds(userId2, userId5).toIdArray()));
        assertFalse(friend.hasAnyFriendsInCircles(ctx, ctx.getViewer(), new int[]{Circle.CIRCLE_DEFAULT}, PeopleIds.forUserIds(userId5).toIdArray()));

        assertTrue(friend.hasAnyFriendsInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleIds.forUserIds(userId2, userId5).toIdArray()));
        assertFalse(friend.hasAnyFriendsInCircles(ctx, ctx.getViewer(), new int[]{circle1.getCircleId()}, PeopleIds.forUserIds(userId2).toIdArray()));

        // Test hasFriend
        assertTrue(friend.hasFriend(ctx, ctx.getViewer(), PeopleId.user(userId2)));
        assertTrue(friend.hasFriend(ctx, ctx.getViewer(), PeopleId.user(userId5)));
        assertFalse(friend.hasFriend(ctx, ctx.getViewer(), PeopleId.user(userId6)));

        // Test hasAllFriends
        assertTrue(friend.hasAllFriends(ctx, ctx.getViewer(), PeopleIds.forUserIds(userId2, userId3, userId4, userId5).toIdArray()));
        assertFalse(friend.hasAllFriends(ctx, ctx.getViewer(), PeopleIds.forUserIds(userId2, userId6, userId3, userId4, userId5).toIdArray()));

        // Test hasAnyFriends
        assertTrue(friend.hasAnyFriends(ctx, ctx.getViewer(), PeopleIds.forUserIds(userId6, userId5).toIdArray()));
        assertFalse(friend.hasAnyFriends(ctx, ctx.getViewer(), PeopleIds.forUserIds(userId6).toIdArray()));
    }

    public void testDestroyCircle() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];


        Context ctx = Context.createForViewer(userId1);
        Circle circle1 = friend.createCustomCircle(ctx, "Circle1");

        // Add 2 into default circle
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2), Circle.CIRCLE_DEFAULT);
        // Add 2, 3 into circle1
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.forUserIds(userId2, userId3), circle1.getCircleId());

        // remove circle1
        friend.destroyCustomCircle(ctx, circle1.getCircleId());

        assertEquals(1, friend.getFriendCount(ctx, ctx.getViewer()));
        assertTrue(PeopleIds.isEquals(PeopleIds.forUserIds(userId2), friend.getFriends(ctx, ctx.getViewer())));
    }

    public void testGetFollowers() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];

        long followerId = userIds[9];

        Context ctx1 = Context.createForViewer(userId1);
        Context ctx2 = Context.createForViewer(userId2);
        Context ctx3 = Context.createForViewer(userId3);

        Context followerCtx = Context.createForViewer(followerId);

        Circle user1Circle1 = friend.createCustomCircle(ctx1, "Circle1");
        Circle user2Circle1 = friend.createCustomCircle(ctx2, "Circle1");

        friend.addFriendsIntoCircle(ctx1, FriendReasons.USER_ACTION, PeopleIds.forUserIds(followerId), Circle.CIRCLE_DEFAULT);
        friend.addFriendsIntoCircle(ctx2, FriendReasons.USER_ACTION, PeopleIds.forUserIds(followerId), Circle.CIRCLE_DEFAULT);
        friend.addFriendsIntoCircle(ctx3, FriendReasons.USER_ACTION, PeopleIds.forUserIds(followerId), Circle.CIRCLE_DEFAULT);

        friend.addFriendsIntoCircle(ctx1, FriendReasons.USER_ACTION, PeopleIds.forUserIds(followerId), user1Circle1.getCircleId());
        friend.addFriendsIntoCircle(ctx2, FriendReasons.USER_ACTION, PeopleIds.forUserIds(followerId), user2Circle1.getCircleId());

        assertEquals(3, friend.getFollowersCount(followerCtx, PeopleId.user(followerId)));
        assertTrue(ArrayHelper.equalsAsSet(new long[]{userId1, userId2, userId3}, friend.getFollowers(followerCtx, PeopleId.user(followerId), null)));

        friend.setFriendIntoCircles(ctx1, FriendReasons.USER_ACTION, PeopleId.user(followerId));
        assertEquals(2, friend.getFollowersCount(followerCtx, PeopleId.user(followerId)));
        assertTrue(ArrayHelper.equalsAsSet(new long[]{userId2, userId3}, friend.getFollowers(followerCtx, PeopleId.user(followerId), null)));
    }

    public void testRelationship() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];


        long userId3 = userIds[2];
        long userId4 = userIds[3];
        long userId5 = userIds[4];

        Context ctx1 = Context.createForViewer(userId1);
        Context ctx2 = Context.createForViewer(userId2);

        // user 1: default circle {2, 3}, acquaintance circle {3, 4}
        friend.setFriendIntoCircles(ctx1, FriendReasons.USER_ACTION, PeopleId.user(userId2), Circle.CIRCLE_DEFAULT);
        friend.setFriendIntoCircles(ctx1, FriendReasons.USER_ACTION, PeopleId.user(userId3), Circle.CIRCLE_DEFAULT, Circle.CIRCLE_ACQUAINTANCE);
        friend.setFriendIntoCircles(ctx1, FriendReasons.USER_ACTION, PeopleId.user(userId4), Circle.CIRCLE_ACQUAINTANCE);

        // user 2: default circle {1, 3}, acquaintance circle {1, 4}
        friend.setFriendIntoCircles(ctx2, FriendReasons.USER_ACTION, PeopleId.user(userId3), Circle.CIRCLE_DEFAULT);
        friend.setFriendIntoCircles(ctx2, FriendReasons.USER_ACTION, PeopleId.user(userId1), Circle.CIRCLE_DEFAULT, Circle.CIRCLE_ACQUAINTANCE);
        friend.setFriendIntoCircles(ctx2, FriendReasons.USER_ACTION, PeopleId.user(userId4), Circle.CIRCLE_ACQUAINTANCE);

        // Test getRelationship
        Relationship rel = friend.getRelationship(ctx1, PeopleId.user(ctx1.getViewer()), PeopleId.user(userId3));
        assertTrue(ArrayHelper.equalsAsSet(rel.getTargetInViewerCircleIds(), new int[]{Circle.CIRCLE_DEFAULT, Circle.CIRCLE_ACQUAINTANCE}));
        assertTrue(ArrayHelper.equalsAsSet(rel.getViewerInTargetCircleIds(), new int[]{}));

        rel = friend.getRelationship(ctx1, PeopleId.user(ctx1.getViewer()), PeopleId.user(userId2));
        assertTrue(ArrayHelper.equalsAsSet(rel.getTargetInViewerCircleIds(), new int[]{Circle.CIRCLE_DEFAULT}));
        assertTrue(ArrayHelper.equalsAsSet(rel.getViewerInTargetCircleIds(), new int[]{Circle.CIRCLE_ACQUAINTANCE, Circle.CIRCLE_DEFAULT}));
        assertTrue(rel.isMutual());

        rel = friend.getRelationship(ctx1, PeopleId.user(ctx1.getViewer()), PeopleId.user(userId5));
        assertTrue(rel.isDisrelated());

        rel = friend.getRelationship(ctx1, PeopleId.contact("contact1"), PeopleId.contact("contact2"));
        assertTrue(rel.isDisrelated());

        // Test getRelationships
        Relationships rels = friend.getRelationships(ctx2, PeopleId.user(ctx2.getViewer()), PeopleIds.forUserIds(userId1, userId3, userId4, userId5).toIdArray());

        rel = rels.getRelation(PeopleId.user(userId2), PeopleId.user(userId1));
        assertTrue(ArrayHelper.equalsAsSet(rel.getTargetInViewerCircleIds(), new int[]{Circle.CIRCLE_DEFAULT, Circle.CIRCLE_ACQUAINTANCE}));
        assertTrue(ArrayHelper.equalsAsSet(rel.getViewerInTargetCircleIds(), new int[]{Circle.CIRCLE_DEFAULT}));
        assertTrue(rel.isMutual());

        rel = rels.getRelation(PeopleId.user(userId2), PeopleId.user(userId3));
        assertTrue(ArrayHelper.equalsAsSet(rel.getTargetInViewerCircleIds(), new int[]{Circle.CIRCLE_DEFAULT}));
        assertTrue(ArrayHelper.equalsAsSet(rel.getViewerInTargetCircleIds(), new int[]{}));

        rel = rels.getRelation(PeopleId.user(userId2), PeopleId.user(userId4));
        assertTrue(ArrayHelper.equalsAsSet(rel.getTargetInViewerCircleIds(), new int[]{Circle.CIRCLE_ACQUAINTANCE}));
        assertTrue(ArrayHelper.equalsAsSet(rel.getViewerInTargetCircleIds(), new int[]{}));

        rel = rels.getRelation(PeopleId.user(userId2), PeopleId.user(userId5));
        assertTrue(rel.isDisrelated());
    }

    public void testRemark() {
        FriendLogic friend = getFriend();

        long[] userIds = getAllUserIds();
        long userId1 = userIds[0];
        long userId2 = userIds[1];
        long userId3 = userIds[2];

        Context ctx = Context.createForViewer(userId1);
        friend.setRemark(ctx, PeopleId.user(userId2), "remark2");
        friend.setRemark(ctx, PeopleId.user(userId3), "remark3");
        assertEquals("remark2", friend.getRemark(ctx, userId1, PeopleId.user(userId2)));
        assertEquals("remark3", friend.getRemark(ctx, userId1, PeopleId.user(userId3)));

        Map<PeopleId, String> remarks = friend.getRemarks(ctx, userId1, PeopleId.user(userId2), PeopleId.user(userId3));
        assertEquals("remark2", remarks.get(PeopleId.user(userId2)));
        assertEquals("remark3", remarks.get(PeopleId.user(userId3)));
    }

}
