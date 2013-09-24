package com.borqs.server.test.migration.test1;

import com.borqs.server.impl.migration.MigrationDb;
import com.borqs.server.impl.migration.MigrationEx;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.migration.circle.CircleMigImpl;
import com.borqs.server.impl.migration.comment.CommentMigImpl;
import com.borqs.server.impl.migration.contact.ContactMigImpl;
import com.borqs.server.impl.migration.conversation.ConversationMigImpl;
import com.borqs.server.impl.migration.friend.FriendMigImpl;
import com.borqs.server.impl.migration.photo.PhotoMigImpl;
import com.borqs.server.impl.migration.request.RequestMigImpl;
import com.borqs.server.impl.migration.setting.SettingMigImpl;
import com.borqs.server.impl.migration.stream.StreamMigImpl;
import com.borqs.server.impl.migration.suggest.SuggestMigImpl;
import com.borqs.server.impl.migration.ticket.TicketMigImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;

public class MigrationTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(MigrationDb.class);
    }

    private MigrationEx getStreamMigrationLogic() {
        return (MigrationEx) getBean("logic.migration");
    }

    private AccountMigImpl getAccountMig() {
        return (AccountMigImpl) getBean("account.mig");
    }

    private CircleMigImpl getCircleMig() {
        return (CircleMigImpl) getBean("circle.mig");
    }

    private FriendMigImpl getFriendMig() {
        return (FriendMigImpl) getBean("friend.mig");
    }

    private StreamMigImpl getStreamMig() {
        return (StreamMigImpl) getBean("stream.mig");
    }

    private CommentMigImpl getCommentMig() {
        return (CommentMigImpl) getBean("comment.mig");
    }

    private ConversationMigImpl getConversationMig() {
        return (ConversationMigImpl) getBean("conversation.mig");
    }

    private SettingMigImpl getSettingMig() {
        return (SettingMigImpl) getBean("setting.mig");
    }

    private TicketMigImpl getTicketMig() {
        return (TicketMigImpl) getBean("ticket.mig");
    }

    private ContactMigImpl getContactMig() {
        return (ContactMigImpl) getBean("contact.mig");
    }

    private PhotoMigImpl getPhotoMig() {
        return (PhotoMigImpl) getBean("photo.mig");
    }

    private SuggestMigImpl getSuggestMig() {
        return (SuggestMigImpl) getBean("suggest.mig");
    }

    private RequestMigImpl getRequestMig() {
        return (RequestMigImpl) getBean("request.mig");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    /*public void testStreamMigration() {
        MigrationEx migrationEx = this.getStreamMigrationLogic();
        migrationEx.streamMigration(ctx);

    }

    public void testFriendMigration() {
        MigrationEx migrationEx = this.getStreamMigrationLogic();
        migrationEx.friendMigration(ctx);

    }

    public void testCircleMigration() {
        MigrationEx migrationEx = this.getStreamMigrationLogic();
        migrationEx.circleMigration(ctx);

    }

    public void testAccountMigration() {
        MigrationEx migrationEx = this.getStreamMigrationLogic();
        migrationEx.accountMigration(ctx);
    }*/
    //______________________update SORT_KEY________________________
    /*public void testUpdateSortKey() {
        AccountMigImpl accountMig = getAccountMig();
        accountMig.accountSortKeyMigration(ctx);
    }*/

    //______________________update SORT_KEY________________________

    //-------------------test the new migration-------------------
    public void testAccountMig() {
        AccountMigImpl accountMig = getAccountMig();
        accountMig.accountMigration(ctx);
    }

    public void testCircleMig() {
        CircleMigImpl circleMig = getCircleMig();
        circleMig.circleMigration(ctx);
    }

    public void testFriendMig() {
        FriendMigImpl friendMig = getFriendMig();
        friendMig.friendMigration(ctx);
    }

    public void testStream() {
        StreamMigImpl streamMig = getStreamMig();
        streamMig.streamMigration(ctx);
    }

    public void testComment() {
        CommentMigImpl commentMig = getCommentMig();
        commentMig.commentMigration(ctx);
    }

    public void testConversation() {
        ConversationMigImpl conversationMig = getConversationMig();
        conversationMig.conversationMigration(ctx);
    }

    public void testSetting() {
        SettingMigImpl settingMig = getSettingMig();
        settingMig.settingMigration(ctx);
    }

    public void testTicket() {
        TicketMigImpl ticketMig = getTicketMig();
        ticketMig.ticketMigration(ctx);
    }

    public void testContact() {
        ContactMigImpl contactMig = getContactMig();
        contactMig.contactMigration(ctx);
    }

    public void testPhoto() {
        PhotoMigImpl photoMig = getPhotoMig();
        photoMig.photoMigration(ctx);
    }

    public void testSuggest() {
        SuggestMigImpl suggestMig = getSuggestMig();
        suggestMig.suggestMigration(ctx);
    }

    public void testRequest() {
        RequestMigImpl suggestMig = getRequestMig();
        suggestMig.requestMigration(ctx);
    }
}
