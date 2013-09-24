package com.borqs.server.test.login.test1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class CibindMock implements CibindLogic {
    public CibindMock() {
    }

    @Override
    public long whoBinding(Context ctx, String info) {
        if (StringUtils.equals(info, LoginLogicTest1.USER1_EMAIL) || StringUtils.equals(info, LoginLogicTest1.USER1_PHONE))
            return LoginLogicTest1.USER1_ID;

        return 0;
    }

    @Override
    public Map<String, Long> whoBinding(Context ctx, String... infos) {
        LinkedHashMap<String, Long> m = new LinkedHashMap<String, Long>();
        if (ArrayUtils.isEmpty(infos))
            return m;

        for (String info : infos)
            m.put(info, whoBinding(ctx, info));
        return m;
    }

    @Override
    public boolean hasBinding(Context ctx, String info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasBinding(Context ctx, long userId, String info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(Context ctx, BindingInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(Context ctx, String type, String info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unbind(Context ctx, String info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, BindingInfo[]> getBindings(Context ctx, long[] userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BindingInfo[] getBindings(Context ctx, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getBindings(Context ctx, long userId, String type) {
        throw new UnsupportedOperationException();
    }
}
