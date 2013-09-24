package com.borqs.server.test.video.test1;

import com.borqs.server.impl.video.VideoDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.video.Video;
import com.borqs.server.platform.feature.video.VideoLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.RandomHelper;

import java.util.List;

public class VideoLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(VideoDb.class);
    }

    private VideoLogic getVideoLogic() {
        return (VideoLogic) getBean("logic.video");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreate() {
        VideoLogic audioLogic = this.getVideoLogic();
        Video video = new Video();
        video.setVideoId(RandomHelper.generateId());
        video.setTitle("test video");
        video.setExpName("test");
        video.setUserId(ctx.getViewer());
        audioLogic.saveVideo(ctx, video);
    }

    public void testAudioGet() {
        VideoLogic audioLogic = this.getVideoLogic();
        List<Video> audio = audioLogic.getVideoById(ctx, 2824502543610779380l);
        assertNotNull(audio);
    }

    public void testDestoryAudio() {
        VideoLogic audioLogic = this.getVideoLogic();
        audioLogic.deleteVideo(ctx, 2824502543610779380l);
    }

    public void testAudioGets() {
        VideoLogic audioLogic = this.getVideoLogic();
        List<Video> audio = audioLogic.getVideo(ctx, ctx.getViewer(), false, new Page(0, 20));
    }


}
