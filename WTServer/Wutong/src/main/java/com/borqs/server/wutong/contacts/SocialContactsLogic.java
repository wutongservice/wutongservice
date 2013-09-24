package com.borqs.server.wutong.contacts;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;


public interface SocialContactsLogic  {
    boolean createSocialContacts(Context ctx, String owner, String username, int type, String content, String uid);

    RecordSet getSocialContacts(Context ctx, String owner, int type, int page, int count);

    RecordSet getSocialContactsUid(Context ctx, String owner);

    RecordSet getSocialContactsOwner(Context ctx, String duserId, String uids);

    RecordSet getCommSocialContactsM(Context ctx, String userId);

    RecordSet getCommSocialContactsU(Context ctx, String userId, String friendId);

    RecordSet getWhohasMyContacts(Context ctx, String userId, String email, String phone);

    RecordSet getDistinctUsername(Context ctx, String uide);

    RecordSet getDistinctOwner(Context ctx, String uid, String username);

    RecordSet getUserName(Context ctx, String owner, String uid);

    RecordSet getCommSocialContactsByUid(Context ctx, String owner, String userId);


    RecordSet createSocialContacts(Context ctx, String userId, String updateInfo, String ua, String loc) ;

    RecordSet findBorqsIdFromContactInfo(Context ctx, RecordSet in_contact);

    Record findMyAllPhoneBookP(Context ctx, String userId, String updateInfo);
}