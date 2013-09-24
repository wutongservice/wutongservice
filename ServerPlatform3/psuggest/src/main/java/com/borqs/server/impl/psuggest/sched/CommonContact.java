package com.borqs.server.impl.psuggest.sched;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;

import java.util.ArrayList;

public class CommonContact extends PeopleSuggestionBuilder {
    // logic
    private ContactLogic contactLogic;
    private PeopleSuggestLogic peopleSuggestLogic;

    public ContactLogic getContactLogic() {
        return contactLogic;
    }

    public void setContactLogic(ContactLogic contactLogic) {
        this.contactLogic = contactLogic;
    }

    public PeopleSuggestLogic getPeopleSuggestLogic() {
        return peopleSuggestLogic;
    }

    public void setPeopleSuggestLogic(PeopleSuggestLogic peopleSuggestLogic) {
        this.peopleSuggestLogic = peopleSuggestLogic;
    }

    @Override
    protected void handle(long userId) {
        Context ctx = Context.create();
        Contacts contacts = contactLogic.getContacts(ctx, Reasons.UPLOAD_CONTACTS, userId);
        String[] contents = contacts.getContents();

        ArrayList<PeopleSuggest> l = new ArrayList<PeopleSuggest>();
        for (String content : contents) {
            Contacts contacts_ = contactLogic.searchContacts(ctx, Reasons.UPLOAD_CONTACTS, content);
            long[] suggests = contacts_.getOwners();
            for (long suggested : suggests)
                l.add(PeopleSuggest.of(userId, PeopleId.fromId(suggested),
                        SuggestionReasons.FROM_ADDRESS_HAVE_COMMON_CONTACT, ""));
        }

        peopleSuggestLogic.create(ctx, l.toArray(new PeopleSuggest[l.size()]));
    }
}
