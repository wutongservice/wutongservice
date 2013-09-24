package com.borqs.server.platform.util.image;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class ImageMagickHelper {

    public static final String DEFAULT_SMALL_SIZE = "100x100";
    public static final String DEFAULT_MIDDLE_SIZE = "200x200";
    public static final String DEFAULT_LARGE_SIZE = "300x300";
    public static final String DEFAULT_THUMBNAIL_SIZE = "120x120";

    public static void resize4(String input,
                               String smallOutput, String smallSize,
                               String middleOutput, String middleSize,
                               String largeOutput, String largeSize, String thumbnailOutput, String thumbnailSize) {
        if (!StringUtils.isEmpty(smallOutput))
            new ImageMagickCommand(input, smallOutput).resize(ObjectUtils.toString(smallSize, DEFAULT_SMALL_SIZE)).checkRun();
        if (!StringUtils.isEmpty(middleOutput))
            new ImageMagickCommand(input, middleOutput).resize(ObjectUtils.toString(middleSize, DEFAULT_MIDDLE_SIZE)).checkRun();
        if (!StringUtils.isEmpty(largeOutput))
            new ImageMagickCommand(input, largeOutput).resize(ObjectUtils.toString(largeSize, DEFAULT_LARGE_SIZE)).checkRun();
        if (!StringUtils.isEmpty(thumbnailOutput))
            new ImageMagickCommand(input, thumbnailOutput).resize(ObjectUtils.toString(thumbnailSize, DEFAULT_THUMBNAIL_SIZE)).checkRun();
    }
}
