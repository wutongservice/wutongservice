package com.borqs.server.platform.conversation;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Conversation;
import com.borqs.server.service.platform.Stream;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class ConversationBase extends RPCService implements Conversation {
    public final Schema conversationSchema = Schema.loadClassPath(ConversationBase.class, "conversation.schema");

    protected ConversationBase() {
    }

    @Override
    public final Class getInterface() {
        return Stream.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        conversationSchema.loadAliases(getConfig().getString("schema.conversation.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean saveConversation0(Record conversation);

    @Override
    public boolean createConversation(ByteBuffer conversation) throws AvroRemoteException, ResponseError {
        try {
            Record conversation0 = Record.fromByteBuffer(conversation);
            Schemas.checkRecordIncludeColumns(conversation0, "target_type", "target_id", "reason", "from_");

            long now = DateUtils.nowMillis();
            conversation0.put("created_time", now);
            Schemas.standardize(conversationSchema, conversation0);
            boolean b = false;
            if (!ifExistConversation((int)conversation0.getInt("target_type"), conversation0.getString("target_id"), (int) conversation0.getInt("reason"), conversation0.getInt("from_"))) {
                b = saveConversation0(conversation0);
            }
            else
            {
                b = updateConversation((int)conversation0.getInt("target_type"), conversation0.getString("target_id"), (int) conversation0.getInt("reason"), conversation0.getInt("from_"));
            }

            return b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteConversation0(int target_type,String target_id,int reason,long from);

    @Override
    public boolean deleteConversation(int target_type, CharSequence target_id, int reason, long from) throws AvroRemoteException, ResponseError {
        try {
            boolean b = deleteConversation0(target_type, toStr(target_id), reason, from);
            return b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateConversation0(int target_type,String target_id,int reason,long from);

    public boolean updateConversation(int target_type, CharSequence target_id, int reason, long from) throws AvroRemoteException, ResponseError {
        try {
            boolean b = updateConversation0(target_type, toStr(target_id), reason, from);
            return b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getConversation0(int target_type, String target_id, List<String> reasons,long from, int page, int count);

    @Override
    public ByteBuffer getConversation(int target_type, CharSequence target_id, CharSequence reasons,long from, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            List<String> reasons0 = StringUtils2.splitList(toStr(reasons), ",", true);
            return getConversation0(target_type, toStr(target_id), reasons0,from, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean ifExistConversation0(int target_type,String target_id,int reason,long from);

    public boolean ifExistConversation(int target_type, CharSequence target_id, int reason, long from) throws AvroRemoteException, ResponseError {
        try {
            return ifExistConversation0(target_type, toStr(target_id), reason, from);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateConversationTarget0(String old_target_id, String new_target_id);

    @Override
    public boolean updateConversationTarget(CharSequence old_target_id, CharSequence new_target_id) throws AvroRemoteException, ResponseError {
        try {
            return updateConversationTarget0(toStr(old_target_id), toStr(new_target_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
