package com.borqs.server.impl.psuggest.sched;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;

import java.util.ArrayList;
import java.util.Map;

public class BorqsIdFromContact extends PeopleSuggestionBuilder {
    // logic
    private ContactLogic contactLogic;
    private PeopleSuggestLogic peopleSuggestLogic;

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    private CibindLogic cibind;

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
        Map<String, Long> m = cibind.whoBinding(ctx, contents);

        ArrayList<PeopleSuggest> l = new ArrayList<PeopleSuggest>();
        for (Long suggested : m.values()) {
            if (suggested.longValue() != 0L)
                l.add(PeopleSuggest.of(userId, PeopleId.fromId(suggested),
                        SuggestionReasons.FROM_ADDRESS_HAVE_BORQSID, ""));
        }

        peopleSuggestLogic.create(ctx, l.toArray(new PeopleSuggest[l.size()]));
    }
}
