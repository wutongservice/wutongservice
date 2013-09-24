package com.borqs.server.test.platform.util;



import com.borqs.server.platform.util.image.ImageMagickHelper;
import junit.framework.TestCase;

public class ImageMagickTest extends TestCase {
    public void testResize() throws Exception {
        String input = "E:\\Documents\\My Pictures\\Wallpapers\\01998_routetocastlemountain_1280x1024.jpg";
        ImageMagickHelper.resize4(input,
                "E:\\Documents\\My Pictures\\Wallpapers\\01998_routetocastlemountain_S.jpg", "100x100",
                "E:\\Documents\\My Pictures\\Wallpapers\\01998_routetocastlemountain_M.jpg", "200x200",
                "E:\\Documents\\My Pictures\\Wallpapers\\01998_routetocastlemountain_L.jpg", "300x300",null,null);
    }
}
