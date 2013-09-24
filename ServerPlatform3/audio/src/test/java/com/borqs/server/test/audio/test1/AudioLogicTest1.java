package com.borqs.server.test.audio.test1;

import com.borqs.server.impl.audio.AudioDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.audio.Audio;
import com.borqs.server.platform.feature.audio.AudioLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.RandomHelper;

import java.util.List;

public class AudioLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(AudioDb.class);
    }

    private AudioLogic getAudioLogic() {
        return (AudioLogic) getBean("logic.audio");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreate() {
        AudioLogic audioLogic = this.getAudioLogic();
        Audio audio = new Audio();
        audio.setAudioId(RandomHelper.generateId());
        audio.setTitle("test audio");
        audio.setExpName("test");
        audio.setUserId(ctx.getViewer());
        audioLogic.saveAudio(ctx, audio);
    }

    public void testAudioGet() {
        AudioLogic audioLogic = this.getAudioLogic();
        List<Audio> audio = audioLogic.getAudiosById(ctx, 2824329746871145860l);
        assertNotNull(audio);
    }

    public void testDestoryAudio(){
        AudioLogic audioLogic = this.getAudioLogic();
        audioLogic.deleteAudio(ctx,2824329247543105344l);
    }

    public void testAudioGets(){
        AudioLogic audioLogic = this.getAudioLogic();
                List<Audio> audio = audioLogic.getAudio(ctx,ctx.getViewer() , false, new Page(0,20));
    }


}
