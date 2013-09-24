package com.borqs.server.test.privacy.test1;


import com.borqs.server.impl.privacy.PrivacyControlImpl;
import com.borqs.server.impl.privacy.PrivacyDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.EmailInfo;
import com.borqs.server.platform.feature.account.TelInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.privacy.*;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.test.mock.SteveAndBill;

import java.util.ArrayList;
import java.util.List;

public class PrivacyLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(PrivacyDb.class);
    }

    private PrivacyControlLogic getPrivacyControlLogic() {
        return (PrivacyControlLogic) getBean("logic.privacy");
    }

    private TestAccount getAccountLogic() {
        return (TestAccount)getBean("logic.account");
    }

    private void setPrivacy0(Context ctx, PrivacyControlImpl privacy)
    {
        PrivacyEntry entry0 = PrivacyEntry.of(ServerTeam.GRX_ID, PrivacyResources.RES_VCARD,
                PrivacyTarget.all(), true);
        PrivacyEntry entry1 = PrivacyEntry.of(ServerTeam.GRX_ID, PrivacyResources.RES_VCARD,
                PrivacyTarget.parse(String.valueOf(ServerTeam.JCS_ID)), false);
        privacy.setPrivacy(ctx, entry0, entry1);
    }

    private void setPrivacy1(Context ctx, PrivacyControlImpl privacy) {
        PrivacyEntry entry0 = PrivacyEntry.of(ServerTeam.WP_ID, PrivacyResources.RES_VCARD,
                PrivacyTarget.circle(Circle.CIRCLE_DEFAULT), true);
        PrivacyEntry entry1 = PrivacyEntry.of(ServerTeam.WP_ID, PrivacyResources.RES_VCARD,
                PrivacyTarget.user(ServerTeam.JCS_ID), false);
        privacy.setPrivacy(ctx, entry0, entry1);
    }
    
    public void testSetGet() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);
        setPrivacy0(ctx, privacy);

        List<PrivacyEntry> entries = privacy.getPrivacy(ctx, ServerTeam.GRX_ID);
        for(PrivacyEntry entry : entries) {
            if(entry.target.scope == PrivacyTarget.SCOPE_ALL) {
                assertEquals(entry.allow, true);
            }
            else if(entry.target.scope == PrivacyTarget.SCOPE_USER) {
                assertEquals(entry.target.id, String.valueOf(ServerTeam.JCS_ID));
                assertEquals(entry.allow, false);
            }
        }
    }

    public void testGetAllowIds() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);
        setPrivacy0(ctx, privacy);

        AllowedIds allowedIds = privacy.getAllowIds(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD);

        assertTrue(allowedIds.isExclusionMode());
        assertEquals(allowedIds.idCount(), 1);
        assertEquals(allowedIds.ids[0], ServerTeam.JCS_ID);
    }

    public void testClear() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);
        setPrivacy0(ctx, privacy);

        privacy.clearPrivacy(ctx, PrivacyResources.RES_VCARD);

        List<PrivacyEntry> entries = privacy.getPrivacy(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD);
        assertEquals(entries.size(), 1);
        PrivacyEntry entry = entries.get(0);
        PrivacyEntry def = PrivacyPolicies.getDefault(PrivacyResources.RES_VCARD);
        assertTrue(entry.equals(def));
    }

    public void testMutual() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.GRX_ID);

        privacy.mutualAllow(ctx, PrivacyResources.RES_VCARD, ServerTeam.JCS_ID);
        
        PrivacyEntry entry0 = privacy.getPrivacy(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD).get(0);
        assertEquals(entry0.target.id, String.valueOf(ServerTeam.JCS_ID));
        assertEquals(entry0.allow, true);

        PrivacyEntry entry1 = privacy.getPrivacy(ctx, ServerTeam.JCS_ID, PrivacyResources.RES_VCARD).get(0);
        assertEquals(entry1.target.id, String.valueOf(ServerTeam.GRX_ID));
        assertEquals(entry1.allow, true);
    }
    
    public void testCheck0() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.WP_ID);

        boolean allow = privacy.check(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, false);

        setPrivacy1(ctx, privacy);
        allow = privacy.check(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, true);
        allow = privacy.check(ctx, ServerTeam.JCS_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, false);
        allow = privacy.check(ctx, ServerTeam.CG_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, false);
    }

    public void testCheck1() {
        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = Context.createForViewer(ServerTeam.WP_ID);

        boolean allow = privacy.check(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, false);

        PrivacyEntry entry = PrivacyEntry.of(SteveAndBill.STEVE_ID, PrivacyResources.RES_VCARD,
                PrivacyTarget.all(), true);
        privacy.setPrivacy(ctx, entry);
        allow = privacy.check(ctx, ServerTeam.GRX_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, true);
        allow = privacy.check(ctx, ServerTeam.JCS_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, true);
        allow = privacy.check(ctx, ServerTeam.CG_ID, PrivacyResources.RES_VCARD, ServerTeam.WP_ID);
        assertEquals(allow, false);
    }

    private Context createContextForViewer(long userId) {
        Context ctx = Context.createForViewer(userId);
        ctx.setPrivacyEnabled(true);
        ctx.setInternal(true);
        return ctx;
    }
    
    public void testUserVcardExpansion() {
        User wp = ServerTeam.wp();
        // tel
        wp.setTel(
                new TelInfo(TelInfo.TYPE_HOME, "82500001"),
                new TelInfo(TelInfo.TYPE_MOBILE, "13800000001", true, "")
        );

        // email
        wp.setEmail(
                new EmailInfo(EmailInfo.TYPE_HOME, "home@abc.com", true, ""),
                new EmailInfo(EmailInfo.TYPE_WORK, "work@abc.com")
        );

        PrivacyControlImpl privacy = (PrivacyControlImpl)getPrivacyControlLogic();
        Context ctx = createContextForViewer(ServerTeam.WP_ID);
        TestAccount account = getAccountLogic();
        account.update(ctx, wp);
        setPrivacy1(ctx, privacy);

        ctx = createContextForViewer(ServerTeam.GRX_ID);
        User user = account.getUser(ctx, null, ServerTeam.WP_ID);
        assertEquals(user.getTel(), wp.getTel());
        assertEquals(user.getEmail(), wp.getEmail());

        ctx = createContextForViewer(ServerTeam.JCS_ID);
        user = account.getUser(ctx, null, ServerTeam.WP_ID);
        assertEquals(user.getTel(), new ArrayList<TelInfo>());
        assertEquals(user.getEmail(), new ArrayList<EmailInfo>());

        ctx = createContextForViewer(ServerTeam.CG_ID);
        user = account.getUser(ctx, null, ServerTeam.WP_ID);
        assertEquals(user.getTel(), new ArrayList<TelInfo>());
        assertEquals(user.getEmail(), new ArrayList<EmailInfo>());
    }
}
