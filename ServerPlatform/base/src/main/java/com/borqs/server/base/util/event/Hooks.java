package com.borqs.server.base.util.event;


import com.borqs.server.base.conf.Configurable;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.ClassUtils2;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Hooks<H extends Hook> {
    protected final List<H> hooks = new ArrayList<H>();

    public Hooks() {
    }

    public Hooks<H> add(H l) {
        if (l != null && !hooks.contains(l)) {
            hooks.add(l);
        }
        return this;
    }

    public void remove(H l) {
        hooks.remove(l);
    }

    public void clear() {
        hooks.clear();
    }

    public boolean isEmpty() {
        return hooks.isEmpty();
    }

    public int size() {
        return hooks.size();
    }

    public Hooks<H> addHooksInConfig(Configuration conf, String key) {
        String s = conf.getString(key, "");
        if (StringUtils.isNotBlank(s)) {
            List<H> ls = (List<H>)ClassUtils2.newInstances(Hook.class, s);
            for (H hook : ls) {
                if (hook instanceof Configurable)
                    ((Configurable) hook).setConfig(conf);
            }
            hooks.addAll(ls);
        }
        return this;
    }
}
