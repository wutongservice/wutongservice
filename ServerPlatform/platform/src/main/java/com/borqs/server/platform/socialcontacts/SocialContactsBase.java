package com.borqs.server.platform.socialcontacts;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.SocialContacts;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class SocialContactsBase extends RPCService implements SocialContacts {
    protected final Schema socialContactsSchema = Schema.loadClassPath(SocialContactsBase.class, "socialcontacts.schema");
    protected SocialContactsBase() {
    }

    @Override
    public final Class getInterface() {
        return SocialContacts.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        socialContactsSchema.loadAliases(getConfig().getString("schema.socialContacts.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean createSocialContacts0(String owner, String username, int type, String content, String uid);

    @Override
    public boolean createSocialContacts(CharSequence owner, CharSequence username, int type, CharSequence content, CharSequence uid) throws AvroRemoteException, ResponseError {
        try {
            return createSocialContacts0(toStr(owner), toStr(username), type, toStr(content), toStr(uid));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSocialContacts0(String owner, int type, int page, int count);
    
    @Override
    public ByteBuffer getSocialContacts(CharSequence owner, int type, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getSocialContacts0(toStr(owner), type,page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getSocialContacts1(String owner);

    @Override
    public ByteBuffer getSocialContactsUid(CharSequence owner) throws AvroRemoteException, ResponseError {
        try {
            return getSocialContacts1(toStr(owner)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSocialContacts2(String duserId,String uids);

    @Override
    public ByteBuffer getSocialContactsOwner(CharSequence duserId,CharSequence uids) throws AvroRemoteException, ResponseError {
        try {
            return getSocialContacts2(toStr(duserId),toStr(uids)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getCommSocialContactsM2(String userId);
    
    @Override
    public ByteBuffer getCommSocialContactsM(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getCommSocialContactsM2(toStr(userId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getCommSocialContactsU3(String userId,String friendId);

    @Override
    public ByteBuffer getCommSocialContactsU(CharSequence userId,CharSequence friendId) throws AvroRemoteException, ResponseError {
        try {
            return getCommSocialContactsU3(toStr(userId),toStr(friendId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getWhohasMyContacts0(String userId, String email, String phone);

    @Override
    public ByteBuffer getWhohasMyContacts(CharSequence userId,CharSequence email,CharSequence phone) throws AvroRemoteException, ResponseError {
        try {
            return getWhohasMyContacts0(toStr(userId),toStr(email),toStr(phone)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getDistinctUsername0(String uid);

    @Override
    public ByteBuffer getDistinctUsername(CharSequence uid) throws AvroRemoteException, ResponseError {
        try {
            return getDistinctUsername0(toStr(uid)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getDistinctOwner0(String uid,String username);

    @Override
    public ByteBuffer getDistinctOwner(CharSequence uid,CharSequence username) throws AvroRemoteException, ResponseError {
        try {
            return getDistinctOwner0(toStr(uid),toStr(username)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUserName0(String owner,String uid);

    @Override
    public ByteBuffer getUserName(CharSequence owner,CharSequence uid) throws AvroRemoteException, ResponseError {
        try {
            return getUserName0(toStr(owner),toStr(uid)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getCommSocialContactsByUid0(String owner,String uid);

    @Override
    public ByteBuffer getCommSocialContactsByUid(CharSequence owner,CharSequence uid) throws AvroRemoteException, ResponseError {
        try {
            return getCommSocialContactsByUid0(toStr(owner),toStr(uid)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
