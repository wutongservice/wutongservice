package com.borqs.server.base.util;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TextFormatter {
    private final Map<String, Provider> providers = new HashMap<String, Provider>();

    public TextFormatter() {
    }

    public TextFormatter addProviders(Collection<Provider> providers) {
        if (CollectionUtils.isNotEmpty(providers)) {
            for (Provider p : providers)
                this.providers.put(p.getProvider(), p);
        }
        return this;
    }

    public TextFormatter addProviders(Provider... providers) {
        return addProviders(Arrays.asList(providers));
    }

    public void clearProviders() {
        providers.clear();
    }

    public List<Provider> getProviders() {
        return new ArrayList<Provider>(providers.values());
    }

    private static boolean isTagChar(char c) {
        return c == '_' || c == ':' || c == '<' || c == '>' || CharUtils.isAsciiAlphanumeric(c);
    }

    public String format(String pattern, String viewer, Map<String, Object> data) {
        if (pattern == null)
            return null;

        int len = pattern.length();
        int state = 0;
        StringBuilder tagBuff = new StringBuilder();

        // 1. search grouped names
        HashMap<String, Set<String>> groupedNames = new HashMap<String, Set<String>>();
        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            switch (state) {
                case 0: {
                    if (c == '$')
                        state = 1;
                }
                break;
                case 1: {
                    if (c == '$') {
                        state = 0;
                    } else if (isTagChar(c)) {
                        state = 2;
                        tagBuff.setLength(0);
                        tagBuff.append(c);
                    } else {
                        throw new IllegalArgumentException("Illegal pattern");
                    }
                }
                break;
                case 2: {
                    if (isTagChar(c)) {
                        tagBuff.append(c);
                    } else if (c == '$')  {
                        addTag(tagBuff.toString(), groupedNames);
                        tagBuff.setLength(0);
                        state = 1;
                    } else {
                        addTag(tagBuff.toString(), groupedNames);
                        tagBuff.setLength(0);
                        state = 0;
                    }
                }
                break;
            }
        }
        if (state != 0 && tagBuff.length() > 0)
            addTag(tagBuff.toString(), groupedNames);



        // 2.Get formatted
        HashMap<String, String> m = new HashMap<String, String>();
        for (Map.Entry<String, Set<String>> e : groupedNames.entrySet()) {
            String provider = e.getKey();
            Set<String> names = e.getValue();
            Provider p = providers.get(provider);
            if (p == null)
                throw new IllegalArgumentException("Can't find provider " + provider);
            Map<String, String> r = p.format(viewer, names, ""); // TODO: add display
            for (String name : names) {
                String s = r.get(name);
                m.put(new StringBuilder(provider).append(':').append(name).toString(), StringUtils.trimToEmpty(s));
            }
        }


        // 3.Replace tags
        tagBuff.setLength(0);
        state = 0;
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            switch (state) {
                case 0: {
                    if (c == '$')
                        state = 1;
                }
                break;
                case 1: {
                    if (c == '$') {
                        state = 0;
                    } else if (isTagChar(c)) {
                        state = 2;
                        tagBuff.setLength(0);
                        tagBuff.append(c);
                    } else {
                        throw new IllegalArgumentException("Illegal pattern");
                    }
                }
                break;
                case 2: {
                    if (isTagChar(c)) {
                        tagBuff.append(c);
                    } else if (c == '$')  {
                        buff.append(StringUtils.trimToEmpty(m.get(tagBuff.toString())));
                        tagBuff.setLength(0);
                        state = 1;
                    } else {
                        buff.append(StringUtils.trimToEmpty(m.get(tagBuff.toString())));
                        tagBuff.setLength(0);
                        state = 0;
                    }
                }
                break;
            }
        }

        if (state != 0 && tagBuff.length() > 0)
            buff.append(StringUtils.trimToEmpty(m.get(tagBuff.toString())));

        return buff.toString();
    }

    private static final Pattern TAG_PATTERN = Pattern.compile("^(\\w+):(\\w+)(<([^>]*)>)?$");


    private String[] getProviderAndNameAndDisplay(String tag) {
        Matcher matcher = TAG_PATTERN.matcher(tag);
        if (matcher.find()) {
            String provider = matcher.group(1);
            String name = matcher.group(2);
            String display = matcher.groupCount() > 2 ? matcher.group(3) : "";
            return new String[] {provider, name, display};
        } else {
            throw new TextFormatException("Invalid tag '%s'", tag);
        }

    }

    private void addTag(String tag, Map<String, Set<String>> groupedNames) {
        String[] ss = getProviderAndNameAndDisplay(tag);
        String provider = ss[0];
        String name = ss[1];
        Set<String> names = groupedNames.get(provider);
        if (names == null) {
            names = new HashSet<String>();
            groupedNames.put(provider, names);
        }
        names.add(name);
    }


    public static interface Provider {
        String getProvider();
        Map<String, String> format(String viewer, Set<String> names, String display);
    }

    private static class NameAndDisplay {

    }
}
