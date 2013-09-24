package com.borqs.server.platform.web.topaz;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.lang.CharUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlMatcher {

    private static final Map<String, Item> compiledPatterns = new HashMap<String, Item>();
    private static final ReadWriteLock RWL = new ReentrantReadWriteLock();


    public static boolean match(String pat, String str, Map<String, String> captured) {
        Item item = null;
        try {
            RWL.readLock().lock();
            item = compiledPatterns.get(pat);
        } finally {
            RWL.readLock().unlock();
        }

        if (item == null) {
            item = compilePattern(pat);
            try {
                RWL.writeLock().lock();
                compiledPatterns.put(pat, item);
            } finally {
                RWL.writeLock().unlock();
            }
        }

        Matcher m = item.regexPattern.matcher(str);
        boolean b = m.find();
        if (captured != null && item.groupNames != null && b) {
            for (int i = 1; i <= m.groupCount(); i++) {
                String val = m.group(i);
                String name = item.groupNames.get(i);
                if (name != null)
                    captured.put(name, val);
            }
        }
        return b;
    }

    public static boolean match(String pat, String str) {
        return match(pat, str, null);
    }

    private static void appendChar(StringBuilder buff, char c) {
        if (c == '\\'
                || c == '*'
                || c == '?'
                || c == '('
                || c == ')'
                || c == '['
                || c == ']'
                || c == '.'
                || c == '^'
                || c == '$') {
            buff.append('\\').append(c);
        } else {
            buff.append(c);
        }
    }

    private static boolean isWord(char c) {
        return CharUtils.isAsciiAlphanumeric(c) || c == '_';
    }

    private static HashMap<Integer, String> ensureGroupNames(HashMap<Integer, String> groupNames) {
        return groupNames != null ? groupNames : new HashMap<Integer, String>();
    }

    private static Item compilePattern(String pat) {
        StringBuilder regexPat = new StringBuilder();
        StringBuilder groupName = new StringBuilder();
        pat = pat + " ";
        int patLen = pat.length();
        int s = 0;
        int groupN = 1;
        HashMap<Integer, String> groupNames = null;

        regexPat.append("^");
        for (int i = 0; i < patLen; i++) {
            char c = pat.charAt(i);
            switch (s) {
                case 0: {
                    if (c == '*') {
                        s = 1;
                    } else if (c == '?') {
                        s = 2;
                    } else if (c == ':') {
                        s = 3;
                    } else if (c == '\\') {
                        s = 4;
                    } else {
                        appendChar(regexPat, c);
                    }
                }
                break;

                case 1: {
                    if (c == ':') {
                        s = 3;
                    } else {
                        regexPat.append("\\w+");
                        appendChar(regexPat, c);
                        s = 0;
                    }
                }
                break;

                case 2: {
                    if (c == ':') {
                        s = 5;
                    } else {
                        regexPat.append("\\w");
                        appendChar(regexPat, c);
                        s = 0;
                    }
                }
                break;

                case 3: {
                    if (isWord(c)) {
                        groupName.append(c);
                    } else {
                        regexPat.append("(\\w+)");
                        appendChar(regexPat, c);
                        groupNames = ensureGroupNames(groupNames);
                        groupNames.put(groupN++, groupName.toString());
                        groupName.setLength(0);
                        s = 0;
                    }
                }
                break;

                case 4: {
                    if (c == '*' || c == '?' || c == ':' || c == '\\') {
                        appendChar(regexPat, c);
                        s = 0;
                    } else {
                        throw new ServerException(E.URL_PATTERN);
                    }
                }
                break;

                case 5: {
                    if (isWord(c)) {
                        groupName.append(c);
                    } else {
                        regexPat.append("(\\w)");
                        appendChar(regexPat, c);
                        groupNames = ensureGroupNames(groupNames);
                        groupNames.put(groupN++, groupName.toString());
                        groupName.setLength(0);
                        s = 0;
                    }
                }
                break;
            }
        }

        regexPat.setLength(regexPat.length() - 1);
        regexPat.append("$");

        Item item = new Item();
        item.regexPattern = Pattern.compile(regexPat.toString());
        item.groupNames = groupNames;
        return item;
    }

    private static class Item {
        Pattern regexPattern;
        Map<Integer, String> groupNames;
    }
}
