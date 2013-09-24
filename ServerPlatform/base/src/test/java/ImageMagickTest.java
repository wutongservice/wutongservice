import com.borqs.server.base.util.SystemHelper;
import junit.framework.TestCase;

public class ImageMagickTest extends TestCase {
    public static final String TEMP_PHOTO_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_photo_image");

    public void testResize() throws Exception {
        /*String s1 = TEMP_PHOTO_IMAGE_DIR + "/";


        String input = "D:\\IMG_0956.JPG";
        BufferedImage b = ImageIO.read(new File(input));
        ImageHelper.scale(b, String.valueOf(b.getWidth()), String.valueOf(b.getHeight()));
        *//**//*ImageMagickHelper.resize4(input,
                "D:\\01998_routetocastlemountain_S.jpg", "100x100",
                "D:\\01998_routetocastlemountain_M.jpg", "200x200",
                "D:\\01998_routetocastlemountain_L.jpg", "300x300", null, null);*//**//*
        ImageMagickHelper.rotate(input, "D:\\ssssss_90.jpg", "90");
        ImageMagickHelper.rotate(input, "D:\\ssssss_180.jpg", "180");
        ImageMagickHelper.rotate(input, "D:\\ssssss_270.jpg", "270");*/
    }
}
