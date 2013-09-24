package com.borqs.server.platform.feature.maker;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.util.ClassHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompositeMaker<T> extends AbstractMaker<T> {

    private final List<Maker<T>> makers = new ArrayList<Maker<T>>();

    public CompositeMaker() {
    }

    public CompositeMaker(Maker<T>... makers) {
        Collections.addAll(this.makers, makers);
    }

    public void setMakers(List<Maker<T>> makers) {
        this.makers.clear();
        if (CollectionUtils.isNotEmpty(makers))
            this.makers.addAll(makers);
    }

    public List<Maker<T>> getMakers() {
        return Collections.unmodifiableList(makers);
    }

    @SuppressWarnings("unchecked")
    public void setMakerClasses(List<String> classNames) {
        ArrayList<Maker<T>> makers = new ArrayList<Maker<T>>();
        for (String className : classNames)
            makers.add((Maker<T>)ClassHelper.newInstance(className));
        setMakers(makers);
    }

    @Override
    public String[] getTemplates() {
        ArrayList<String> l = new ArrayList<String>();
        for (Maker<T> maker : makers) {
            if (maker != null) {
                String[] templates = maker.getTemplates();
                if (ArrayUtils.isNotEmpty(templates))
                    Collections.addAll(l, templates);
            }
        }
        return l.toArray(new String[l.size()]);
    }

    @Override
    public T make(Context ctx, String template, Record opts) {
        for (Maker<T> maker : makers) {
            if (maker == null)
                continue;

            String[] templates = maker.getTemplates();
            if (ArrayUtils.contains(templates, template))
                return maker.make(ctx, template, opts);
        }
        throw new IllegalArgumentException("Can't find maker template " + template);
    }
}
