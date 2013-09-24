package com.borqs.server.platform.feature.contact;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface ContactLogic extends Logic {
    void destroy(Context ctx, Contact... contacts);
    void mergeUpdate(Context ctx, Contact... contacts);
    void fullUpdate(Context ctx, Contact... contacts);

    Contact getContact(Context ctx, int reason, String contactId);
    Contacts getContacts(Context ctx, int reason, String... contactIds);
    Contacts getContacts(Context ctx, int reason, long userId);

    Contacts getCommonContacts(Context ctx, int reason, long userId1, long userId2);
    Contacts searchContacts(Context ctx, int reason, String content);
}
