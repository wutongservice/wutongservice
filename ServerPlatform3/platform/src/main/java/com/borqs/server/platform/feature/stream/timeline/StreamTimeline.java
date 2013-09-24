package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;

import java.util.LinkedHashSet;
import java.util.List;

public abstract class StreamTimeline {

    public static final long DEFAULT_PAGE_SIZE = 50;

    protected StreamLogic stream;

    protected StreamTimeline() {
    }

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public abstract void removeTimeline(Context ctx, PeopleId user);

    public abstract void add(Context ctx, PeopleId user, TimelineEntry entry);

    public abstract TimelineResult get(Context ctx, PeopleId user, PostFilter filter, Page page);

    public static Page regulatePage(Page page) {
        return page != null ? page : new Page(0, DEFAULT_PAGE_SIZE);
    }

    public static PostFilter regulateFilter(PostFilter filter) {
        if (filter == null) {
            return new PostFilter(Post.ALL_POST_TYPES, App.APP_NONE, 0, Long.MAX_VALUE, null);
        } else {
            if (filter.min < 0)
                filter.min = 0;
            if (filter.max <= 0)
                filter.max = Long.MAX_VALUE;

            return filter;
        }
    }


    public static boolean filterIgnorePrivateFlag(TimelineEntry entry, PostFilter filter) {
        long min = filter.min < 0 ? 0 : filter.min;
        long max = filter.max <= 0 ? Long.MAX_VALUE : filter.max;

        if (entry.postId < min || entry.postId > max)
            return false;

        if ((entry.postType & filter.types) == 0)
            return false;

        if (filter.app != App.APP_NONE) {
            if (filter.app != entry.appId)
                return false;
        }

        return true;
    }


    public static final String[] FILTER_COLUMNS = {
            Post.COL_POST_ID,
            Post.COL_TYPE,
            Post.COL_APP
    };

    protected boolean filterEntry(TimelineEntry entry, Context ctx, PeopleId user, String[] expCols, PostFilter filter) {
        boolean b = false;
        if (filterIgnorePrivateFlag(entry, filter)) {
            if (entry.isPrivatePost()) {
                Post post = stream.getPost(ctx, FILTER_COLUMNS, entry.postId);
                if (post != null) {
                    PrivateFilter privateFilter = PrivateFilter.create(ctx, user);
                    b = privateFilter.filter(post);
                }
            }
        }
        return b;
    }


    protected TimelineEntries fullFilter(TimelineEntries result, List<TimelineEntry> timeline, Context ctx, PeopleId user, PostFilter filter) {
        if (result == null)
            result = new TimelineEntries();

        LinkedHashSet<Long> needCheckPrivacyPostIds = new LinkedHashSet<Long>();
        for (TimelineEntry entry : timeline) {
            if (filterIgnorePrivateFlag(entry, filter)) {
                if (entry.isPrivatePost()) {
                    result.add(entry);
                    needCheckPrivacyPostIds.add(entry.postId);
                } else {
                    result.add(entry);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(needCheckPrivacyPostIds)) {
            Posts posts = stream.getPosts(ctx, FILTER_COLUMNS, CollectionsHelper.toLongArray(needCheckPrivacyPostIds));
            PrivateFilter privateFilter = PrivateFilter.create(ctx, user);
            for (Post post : posts) {
                if (privateFilter.filter(post))
                    needCheckPrivacyPostIds.remove(post.getPostId());
            }

            result.removeSpecific(needCheckPrivacyPostIds);
        }

        return result;
    }

    public static abstract class PrivateFilter {
        public abstract boolean filter(Post post);

        private static final PrivateFilter SHOW_ALL = new PrivateFilter() {
            @Override
            public boolean filter(Post post) {
                return true;
            }
        };

        private static final PrivateFilter SHOW_NOT_PRIVATE = new PrivateFilter() {
            @Override
            public boolean filter(Post post) {
                return !BooleanUtils.isTrue(post.getPrivate());
            }
        };


        public static PrivateFilter showAll() {
            return SHOW_ALL;
        }


        public static PrivateFilter showIncludeTo(final PeopleId user) {
            return new PrivateFilter() {
                @Override
                public boolean filter(Post post) {
                    if (BooleanUtils.isTrue(post.getPrivate())) {
                        PeopleIds toIds = post.getTo();
                        return toIds.contains(user);
                    } else {
                        return true;
                    }
                }
            };
        }

        public static PrivateFilter showNotPrivate() {
            return SHOW_NOT_PRIVATE;
        }

        public static PrivateFilter create(Context ctx, PeopleId user) {
            long viewer = ctx.getViewer();
            if (viewer > 0 && user.isUser()) {
                return viewer == user.getIdAsLong() ? showAll() : showIncludeTo(PeopleId.user(viewer));
            } else {
                return showNotPrivate();
            }
        }
    }
//
//    public static abstract class TimelineFilter {
//
//        public final Context ctx;
//        public final StreamLogic logic;
//        public final PeopleId user;
//        public final PostFilter filter;
//
//        protected TimelineFilter(Context ctx, StreamLogic logic, PeopleId user, PostFilter filter) {
//            this.ctx = ctx;
//            this.logic = logic;
//            this.user = user;
//            this.filter = filter;
//        }
//
//        public abstract Posts filter(long[] timeline);
//    }
//
//    public static class FullTimelineFilter extends TimelineFilter {
//        public FullTimelineFilter(Context ctx, StreamLogic logic, PeopleId user, PostFilter filter) {
//            super(ctx, logic, user, filter);
//        }
//
//        @Override
//        public Posts filter(long[] timeline) {
//            PrivateFilter privateFilter = PrivateFilter.create(ctx, user);
//
//            final int BATCH_FETCH_COUNT = 100;
//            Posts r = new Posts();
//
//            try {
//                int beginIndex = 0;
//                for (; ; ) {
//                    long[] subTimeline = ArrayUtils.subarray(timeline, beginIndex, beginIndex + BATCH_FETCH_COUNT);
//                    if (ArrayUtils.isEmpty(subTimeline))
//                        break;
//
//                    Posts posts = logic.getPosts(ctx, subTimeline);
//                    if (CollectionUtils.isNotEmpty(posts)) {
//                        for (Post post : posts) {
//                            if (post.getCreatedTime() < filter.min)
//                                throw new StopException();
//
//                            if (filterPost(post, filter, privateFilter))
//                                r.add(post);
//                        }
//                    }
//                }
//            } catch (StopException ignored) {
//            }
//
//            return r;
//        }
//
//        private static class StopException extends RuntimeException {
//        }
//    }
//
//    public static class LimitedTimelineFilter extends TimelineFilter {
//        public final int limit;
//
//        public LimitedTimelineFilter(Context ctx, StreamLogic logic, PeopleId user, PostFilter filter, int limit) {
//            super(ctx, logic, user, filter);
//            this.limit = limit;
//        }
//
//        @Override
//        public Posts filter(long[] timeline) {
//            PrivateFilter privateFilter = PrivateFilter.create(ctx, user);
//
//            final int BATCH_FETCH_COUNT = 100;
//            Posts r = new Posts();
//
//            try {
//                int beginIndex = 0;
//                for (; ; ) {
//                    long[] subTimeline = ArrayUtils.subarray(timeline, beginIndex, beginIndex + BATCH_FETCH_COUNT);
//                    if (ArrayUtils.isEmpty(subTimeline))
//                        break;
//
//                    Posts posts = logic.getPosts(ctx, subTimeline);
//                    if (CollectionUtils.isNotEmpty(posts)) {
//                        for (Post post : posts) {
//                            if (post.getCreatedTime() < filter.min)
//                                throw new StopException();
//
//                            if (filterPost(post, filter, privateFilter)) {
//                                r.add(post);
//                                if (r.size() > limit)
//                                    throw new StopException();
//                            }
//                        }
//                    }
//                }
//            } catch (StopException ignored) {
//            }
//
//            return r;
//        }
//
//        private static class StopException extends RuntimeException {
//        }
//    }
}
