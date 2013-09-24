package com.borqs.server.platform.poll;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.*;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Poll;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ObjectUtils;

import java.nio.ByteBuffer;

public abstract class PollBase extends RPCService implements Poll {
    protected final Schema pollSchema = Schema.loadClassPath(PollBase.class, "poll.schema");
    protected final Schema itemSchema = Schema.loadClassPath(PollBase.class, "item.schema");

    protected abstract boolean savePoll(Record poll, RecordSet items);
    protected abstract boolean vote0(String userId, long pollId, Record items);
    protected abstract RecordSet getPolls0(String pollIds);
    protected abstract RecordSet getItemsByPollId0(long pollId);
    protected abstract long hasVoted0(String userId, long pollId);
    protected abstract RecordSet getItemsByItemIds0(String itemIds);
    protected abstract Record getCounts0(String pollIds);
    protected abstract String getCreatedPolls0(String viewerId, String userId, int page, int count);
    protected abstract String getParticipatedPolls0(String viewerId, String userId, int page, int count);
    protected abstract String getInvolvedPolls0(String viewerId, String userId, int page, int count);
    protected abstract String getFriendsPolls0(String viewerId, String userId, int sort, int page, int count);
    protected abstract String getPublicPolls0(String viewerId, String userId, int sort, int page, int count);
    protected abstract boolean deletePolls0(String viewerId, String pollIds);

    @Override
    public void init() {
        pollSchema.loadAliases(getConfig().getString("schema.poll.alias", null));
    }

    @Override
    public void destroy() {

    }

    @Override
    public long createPoll(ByteBuffer poll, ByteBuffer items) throws AvroRemoteException, ResponseError {
        Record rec = Record.fromByteBuffer(poll);
        boolean r = savePoll(rec, RecordSet.fromByteBuffer(items));
        if (!r)
            throw new PollException("save poll error");
        return rec.getInt("id");
    }

    @Override
    public ByteBuffer getPolls(CharSequence pollIds) throws AvroRemoteException, ResponseError {
        return getPolls0(toStr(pollIds)).toByteBuffer();
    }

    @Override
    public ByteBuffer getItemsByPollId(long pollId) throws AvroRemoteException, ResponseError {
        return getItemsByPollId0(pollId).toByteBuffer();
    }

    @Override
    public boolean vote(CharSequence userId, long pollId, ByteBuffer items) throws AvroRemoteException, ResponseError {
        return vote0(toStr(userId), pollId, Record.fromByteBuffer(items));
    }

    @Override
    public long hasVoted(CharSequence userId, long pollId) throws AvroRemoteException, ResponseError {
        return hasVoted0(toStr(userId), pollId);
    }

    @Override
    public ByteBuffer getItemsByItemIds(CharSequence itemIds) throws AvroRemoteException, ResponseError {
        return getItemsByItemIds0(toStr(itemIds)).toByteBuffer();
    }

    @Override
    public ByteBuffer getCounts(CharSequence pollIds) throws AvroRemoteException, ResponseError {
        return getCounts0(toStr(pollIds)).toByteBuffer();
    }

    @Override
    public CharSequence getCreatedPolls(CharSequence viewerId, CharSequence userId, int page, int count) throws AvroRemoteException, ResponseError {
        return getCreatedPolls0(toStr(viewerId), toStr(userId), page, count);
    }

    @Override
    public CharSequence getParticipatedPolls(CharSequence viewerId, CharSequence userId, int page, int count) throws AvroRemoteException, ResponseError {
        return getParticipatedPolls0(toStr(viewerId), toStr(userId), page, count);
    }

    @Override
    public CharSequence getInvolvedPolls(CharSequence viewerId, CharSequence userId, int page, int count) throws AvroRemoteException, ResponseError {
        return getInvolvedPolls0(toStr(viewerId), toStr(userId), page, count);
    }

    @Override
    public CharSequence getFriendsPolls(CharSequence viewerId, CharSequence userId, int sort, int page, int count) throws AvroRemoteException, ResponseError {
        return getFriendsPolls0(toStr(viewerId), toStr(userId), sort, page, count);
    }

    @Override
    public CharSequence getPublicPolls(CharSequence viewerId, CharSequence userId, int sort, int page, int count) throws AvroRemoteException, ResponseError {
        return getPublicPolls0(toStr(viewerId), toStr(userId), sort, page, count);
    }

    @Override
    public boolean deletePolls(CharSequence viewerId, CharSequence pollIds) throws AvroRemoteException, ResponseError {
        return deletePolls0(toStr(viewerId), toStr(pollIds));
    }

    @Override
    public Class getInterface() {
        return Poll.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }
}
