package com.borqs.server.impl.link;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.cache.memcached.Memcached;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.feature.link.LinkEntity;
import com.borqs.server.platform.feature.link.LinkLogic;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.util.LinkedHashSet;

public class LinkImpl implements LinkLogic {

    private Memcached cache;
    private long expirySeconds;

    private final Sharelink sharelink = new Sharelink();

    public LinkImpl() {
    }

    public Memcached getCache() {
        return cache;
    }

    public void setCache(Memcached cache) {
        this.cache = cache;
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public long getLinkImageScale() {
        return sharelink.getLinkImageScale();
    }

    public void setLinkImageScale(long linkImageScale) {
        sharelink.setLinkImageScale(linkImageScale);
    }

    public String getLinkImgUrlPattern() {
        return sharelink.getLinkImgUrlPattern();
    }

    public void setLinkImgUrlPattern(String linkImgUrlPattern) {
        sharelink.setLinkImgUrlPattern(linkImgUrlPattern);
    }

    public SFS getLinkSFS() {
        return sharelink.getLinkSFS();
    }

    public void setLinkSFS(SFS linkSFS) {
        sharelink.setLinkSFS(linkSFS);
    }

    @Override
    public LinkEntity get(Context ctx, String url) {
        CacheElement ce = cache.get(url);
        if (ce.getValue() != null)
            return (LinkEntity) ce.getValue();

        try {
            RecordSet recs = sharelink.shareLink(url);
            Record rec = recs.getFirstRecord();
            LinkEntity le = new LinkEntity(url, rec.getString("title"), rec.getString("description"), "", readImageUrls(rec));
            //cache.put(url, le, expirySeconds);
            return le;
        } catch (Exception e) {
            throw new ServerException(E.FETCH_LINK, e, "fetch url '%s' error", url);
        }
    }

    private static String[] readImageUrls(Record rec) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();

        if (rec.has("img_url")) {
            l.add(fileName(rec.getString("img_url")));
        }

        if (rec.has("many_img_url")) {
            JsonNode arr = JsonHelper.parse(rec.getString("many_img_url"));
            if (arr.isArray()) {
                for (int i = 0; i < arr.size(); i++)
                    l.add(fileName(arr.path(i).getValueAsText()));
            }
        }

        return l.toArray(new String[l.size()]);
    }

    private static String fileName(String url) {
        if (StringUtils.isBlank(url))
            return url;
        else
            return StringUtils.substringAfterLast(url, "/");
    }
}
