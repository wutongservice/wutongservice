package com.borqs.server.test.staticfile.test1;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.staticfile.video.StaticFile;
import com.borqs.server.platform.feature.staticfile.video.StaticFileLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.RandomHelper;
import com.broqs.server.impl.staticfile.StaticFileDb;

import java.util.List;

public class StaticFileLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(StaticFileDb.class);
    }

    private StaticFileLogic getVideoLogic() {
        return (StaticFileLogic) getBean("logic.staticfile");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreate() {
        StaticFileLogic audioLogic = this.getVideoLogic();
        StaticFile video = new StaticFile();
        video .setFileId(RandomHelper.generateId());
        video.setTitle("test video");
        video.setExpName("test");
        video.setUserId(ctx.getViewer());
        audioLogic.saveStaticFile(ctx, video);
    }

    public void testAudioGet() {
        StaticFileLogic audioLogic = this.getVideoLogic();
        List<StaticFile> audio = audioLogic.getStaticFileById(ctx, 2824494294833471170l);
        assertNotNull(audio);
    }

    public void testDestoryAudio() {
        StaticFileLogic audioLogic = this.getVideoLogic();
        audioLogic.deleteStaticFile(ctx, 2824494294833471170l);
    }

    public void testAudioGets() {
        StaticFileLogic audioLogic = this.getVideoLogic();
        List<StaticFile> audio = audioLogic.getStaticFile(ctx, ctx.getViewer(), false, new Page(0, 20));
    }


}
