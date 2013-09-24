package com.borqs.server.test.psuggest.test1;

import com.borqs.server.impl.psuggest.PeopleSuggestDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.StringHelper;

public class PSuggestLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(PeopleSuggestDb.class);
    }

    private PeopleSuggestLogic getPeopleSuggestLogic() {
        return (PeopleSuggestLogic) getBean("logic.psuggest");
    }

    private TestAccount getAccountLogic() {
        return (TestAccount) getBean("logic.account");
    }

    private void createPeopleSuggests() {
        PeopleSuggestLogic psuggest = getPeopleSuggestLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);
        PeopleSuggest suggest0 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));
        psuggest.create(ctx, suggest0);

        ctx = Context.createForViewer(ServerTeam.GRX_ID);
        suggest0 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));
        PeopleSuggest suggest1 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.GRX_ID)),
                SuggestionReasons.REQUEST_ATTENTION, String.valueOf(ctx.getViewer()));
        PeopleSuggest suggest2 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.JCS_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));

        psuggest.create(ctx, suggest0, suggest1, suggest2);
    }

    public void testCreateGet() {
        PeopleSuggestLogic psuggest = getPeopleSuggestLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);
        PeopleSuggest suggest0 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));
        psuggest.create(ctx, suggest0);

        PeopleSuggests suggests = psuggest.getSuggested(ctx, ServerTeam.CG_ID, 40);
        assertEquals(suggests.size(), 1);
        assertEquals(suggests.get(0).toString(), suggest0.toString());
        assertEquals(suggests.get(0).getSource(), suggest0.getSource());

        ctx = Context.createForViewer(ServerTeam.GRX_ID);
        suggest0 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));
        PeopleSuggest suggest1 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.GRX_ID)),
                SuggestionReasons.REQUEST_ATTENTION, String.valueOf(ctx.getViewer()));
        PeopleSuggest suggest2 = PeopleSuggest.of(ServerTeam.CG_ID, new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.JCS_ID)),
                SuggestionReasons.RECOMMENDER_USER, String.valueOf(ctx.getViewer()));

        psuggest.create(ctx, suggest0, suggest1, suggest2);

        suggests = psuggest.getSuggested(ctx, ServerTeam.CG_ID, 40);
        assertEquals(suggests.size(), 2);
        for (PeopleSuggest suggest : suggests) {
            if (Long.parseLong(suggest.getSuggested().id) == ServerTeam.WP_ID) {
                assertEquals(suggest.getSource(), StringHelper.join(new long[]{ServerTeam.JCS_ID, ServerTeam.GRX_ID}, ","));
            }
        }
    }

    public void testAcceptRejectGet() {
        createPeopleSuggests();
        Context ctx = Context.createForViewer(ServerTeam.CG_ID);
        PeopleSuggestLogic psuggest = getPeopleSuggestLogic();
        PeopleId grx = new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.GRX_ID));
        PeopleId wp = new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID));
        psuggest.accept(ctx, grx);
        psuggest.reject(ctx, wp);

        PeopleSuggests accepts = psuggest.getAccepted(ctx, ServerTeam.CG_ID);
        PeopleSuggests rejects = psuggest.getRejected(ctx, ServerTeam.CG_ID);

        assertEquals(accepts.size(), 1);
        assertEquals(accepts.get(0).getSuggested(), grx);
        assertEquals(rejects.size(), 1);
        assertEquals(rejects.get(0).getSuggested(), wp);
    }

    public void testGetPeopleSource() {
        createPeopleSuggests();
        Context ctx = Context.createForViewer(ServerTeam.CG_ID);
        PeopleSuggestLogic psuggest = getPeopleSuggestLogic();
        PeopleId grx = new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.GRX_ID));
        PeopleId jcs = new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.JCS_ID));
        PeopleId wp = new PeopleId(PeopleId.USER, String.valueOf(ServerTeam.WP_ID));
        psuggest.accept(ctx, grx,jcs);
        PeopleSuggests peopleSuggests = psuggest.getPeopleSource(ctx, ServerTeam.CG_ID,ServerTeam.JCS_ID);
        System.out.println(peopleSuggests);

    }
}
