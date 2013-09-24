package com.borqs.server.platform.suggesteduser;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.SuggestedUser;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class SuggestedUserBase extends RPCService implements SuggestedUser {
    protected final Schema suggestedUserSchema = Schema.loadClassPath(SuggestedUserBase.class, "suggestedUser.schema");
    protected SuggestedUserBase() {
    }

    @Override
    public final Class getInterface() {
        return SuggestedUser.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        suggestedUserSchema.loadAliases(getConfig().getString("schema.suggestedUser.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean refuseSuggestUser0(String userId, String suggested);

    @Override
    public boolean refuseSuggestUser(CharSequence userId, CharSequence suggested) throws AvroRemoteException, ResponseError {
        try {
            return refuseSuggestUser0(toStr(userId), toStr(suggested));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean create0(String userId, String suggested,int type, String reason);
    
    @Override
    public boolean createSuggestUser(CharSequence userId, CharSequence suggestedUsers,int type, CharSequence reason) throws AvroRemoteException, ResponseError {
        try {
            String[] suggestedUserIds = StringUtils2.splitArray(toStr(suggestedUsers), ",", true);
            for (String suggestedUserId : suggestedUserIds) {
                if (!toStr(userId).equals(toStr(suggestedUserId)))
                    create0(toStr(userId), toStr(suggestedUserId),type, toStr(reason));
            }
            return 1>0;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean backSuggestUser0(String userId,long dateDiff);

     @Override
    public boolean backSuggestUser(CharSequence userId,long dateDiff) throws AvroRemoteException, ResponseError {
        try {
            return backSuggestUser0(toStr(userId),dateDiff);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteSuggestUser0(String userId, String suggested);

    @Override
    public boolean deleteSuggestUser(CharSequence userId, CharSequence suggested) throws AvroRemoteException, ResponseError {
        try {
            return deleteSuggestUser0(toStr(userId), toStr(suggested));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSuggestUser0(String userId, int limit);

    @Override
    public ByteBuffer getSuggestUser(CharSequence userId, int limit) throws AvroRemoteException, ResponseError {
        try {
            return getSuggestUser0(toStr(userId), limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getIfExistSuggestUser0(String meId, String suggestUserId);
    
    @Override
    public ByteBuffer getIfExistSuggestUser(CharSequence userId, CharSequence suggestUserId) throws AvroRemoteException, ResponseError {
        try {
            return getIfExistSuggestUser0(toStr(userId), toStr(suggestUserId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean updateSuggestUser0(String userId, String suggested,int type,String reason);
            
            
   @Override
    public boolean updateSuggestUser(CharSequence userId, CharSequence suggestUserId,int type,CharSequence reason) throws AvroRemoteException, ResponseError {
        try {
            return updateSuggestUser0(toStr(userId), toStr(suggestUserId),type,toStr(reason));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
   
   protected abstract String getWhoSuggest0(String to, String beSuggested);
   
   @Override
   public CharSequence getWhoSuggest(CharSequence to, CharSequence beSuggested) throws AvroRemoteException, ResponseError
   {
	   try {
		   return getWhoSuggest0(toStr(to), toStr(beSuggested));
	   } catch(Throwable t) {
		   throw Errors.wrapResponseError(t);
	   }
   }

    protected abstract RecordSet getSuggestFromBothFriend0(String userId);

    @Override
    public ByteBuffer getSuggestFromBothFriend(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getSuggestFromBothFriend0(toStr(userId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSuggestFromHasMyContactinfo0(String userId);

     @Override
    public ByteBuffer getSuggestFromHasMyContactinfo(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getSuggestFromHasMyContactinfo0(toStr(userId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSuggestUserHistory0(String userId, int limit);

    @Override
    public ByteBuffer getSuggestUserHistory(CharSequence userId, int limit) throws AvroRemoteException, ResponseError {
        try {
            return getSuggestUserHistory0(toStr(userId),limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getWhoSuggestedHim0(String userId, String beSuggested);

    @Override
    public ByteBuffer getWhoSuggestedHim(CharSequence userId, CharSequence beSuggested) throws AvroRemoteException, ResponseError {
        try {
            return getWhoSuggestedHim0(toStr(userId),toStr(beSuggested)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
