package com.borqs.server.platform.nuser.setting;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.NUserSetting;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.io.ValidatingEncoder;
import org.apache.commons.lang.Validate;
import org.codehaus.plexus.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class NUserSettingBase extends RPCService implements NUserSetting {

    protected final Schema settingSchema = Schema.loadClassPath(NUserSettingBase.class, "nuser_setting.schema");

    @Override
    public Class getInterface() {
        return NUserSetting.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        settingSchema.loadAliases(getConfig().getString("schema.nuser.setting.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean set0(String userId, Record values);

    @Override
    public boolean set(CharSequence userId, ByteBuffer values) throws AvroRemoteException, ResponseError {
        try {
            return set0(toStr(userId), Record.fromByteBuffer(values));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record gets0(String userId, List<String> keys) throws ResponseError, AvroRemoteException;

    @Override
    public ByteBuffer gets(CharSequence userId, CharSequence keys) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(keys), ",", true);
            return gets0(toStr(userId), l).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record gets0(String userId, String startsWith) throws ResponseError, AvroRemoteException;

    @Override
    public ByteBuffer getsByStartsWith(CharSequence userId, CharSequence startsWith) throws AvroRemoteException, ResponseError {
        try {
            return gets0(toStr(userId), toStr(startsWith)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record get(String key, List<String> users) throws ResponseError, AvroRemoteException;

    @Override
    public ByteBuffer getByUsers(CharSequence key, CharSequence users) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(users), ",", true);
            return get(toStr(key), l).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getDefault(CharSequence userId, CharSequence key) throws AvroRemoteException, ResponseError {
        String sKey = toStr(key);
        if(StringUtils.equals(sKey, Constants.SOCIALCONTACT_AUTO_ADD))
        {
            return "1";
        }
//    	else if(StringUtils.equals(sKey, Constants.NTF_PROFILE_UPDATE))
//    	{
//    		return "1";
//    	}
        else
        {
            return "0";
        }
    }
}
