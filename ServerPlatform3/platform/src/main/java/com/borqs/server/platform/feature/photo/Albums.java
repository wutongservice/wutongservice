package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class Albums extends ArrayList<Album> {

    public Long[] geAlbumIds() {
        Set<Long> set = new HashSet<Long>();
        for (Album album : this) {
            long album_id = album.getAlbum_id();
            if (!set.contains(album_id))
                set.add(album_id);
        }
        return set.toArray(new Long[set.size()]);
    }

    public String[] geAlbumTitle() {
        Set<String> set = new HashSet<String>();
        for (Album album : this) {
            String title = album.getTitle();
            if (!set.contains(title))
                set.add(title);
        }
        return set.toArray(new String[set.size()]);
    }

    public Long[] geAlbumCoverPhotoId() {
        Set<Long> set = new HashSet<Long>();
        for (Album album : this) {
            long cover_photo_id = album.getCover_photo_id();
            if (!set.contains(cover_photo_id))
                set.add(cover_photo_id);
        }
        return set.toArray(new Long[set.size()]);
    }

    public String[] geAlbumHtmlPageUrl() {
        Set<String> set = new HashSet<String>();
        for (Album album : this) {
            String html_page_url = album.getHtml_page_url();
            if (!set.contains(html_page_url))
                set.add(html_page_url);
        }
        return set.toArray(new String[set.size()]);
    }

    public String[] geAlbumThumbnailUrl() {
        Set<String> set = new HashSet<String>();
        for (Album album : this) {
            String thumbnail_url = album.getThumbnail_url();
            if (!set.contains(thumbnail_url))
                set.add(thumbnail_url);
        }
        return set.toArray(new String[set.size()]);
    }

}
