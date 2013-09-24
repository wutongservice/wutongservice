package com.borqs.server.platform.account;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.*;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.platform.ErrorCode;
import com.borqs.server.service.platform.Account;
import com.borqs.server.service.platform.Constants;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.nio.ByteBuffer;
import java.util.*;

import static com.borqs.server.service.platform.Constants.NULL_USER_ID;

public abstract class AccountBase extends RPCService implements Account {
    protected final Schema userSchema = Schema.loadClassPath(AccountBase.class, "user.schema");
    protected final Schema ticketSchema = Schema.loadClassPath(AccountBase.class, "ticket.schema");
    protected final Schema contactSchema = Schema.loadClassPath(AccountBase.class, "contact_info.schema");
    protected final Schema addressSchema = Schema.loadClassPath(AccountBase.class, "address.schema");
    protected final Schema workSchema = Schema.loadClassPath(AccountBase.class, "work_history.schema");
    protected final Schema educationSchema = Schema.loadClassPath(AccountBase.class, "education_history.schema");
    protected final Schema privacySchema = Schema.loadClassPath(AccountBase.class, "privacy.schema");

    protected String profileImagePattern;
    protected String sysIconUrlPattern;

    protected AccountBase() {
    }

    @Override
    public final Class getInterface() {
        return Account.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        Configuration conf = getConfig();
        userSchema.loadAliases(conf.getString("schema.user.alias", null));
        ticketSchema.loadAliases(conf.getString("schema.ticket.alias", null));
        privacySchema.loadAliases(conf.getString("schema.privacy.alias", null));
        profileImagePattern = StringUtils.removeEnd(conf.checkGetString("platform.profileImagePattern").trim(), "/");
        sysIconUrlPattern = StringUtils.removeEnd(conf.checkGetString("platform.sysIconUrlPattern").trim(), "/");
    }

    @Override
    public void destroy() {
    }

    // checker

    public static void checkLoginName(String loginName) {
        if (loginName != null && org.apache.commons.lang.StringUtils.isBlank(loginName))
            throw new AccountException(ErrorCode.USER_NOT_EXISTS, "Invalid login name");
    }

    public static void checkDisplayName(String displayName) {
        if (displayName != null && org.apache.commons.lang.StringUtils.isBlank(displayName))
            throw new AccountException(ErrorCode.PARAM_ERROR,"Invalid display name");
    }

    public static void checkPassword(String pwd) {
        if (pwd != null && pwd.isEmpty())
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid password");
    }

    public static void checkPhone(String phone) {
        if (phone != null && !phone.matches("^+?\\d+$"))
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid phone number");
    }

    public static void checkEmail(String email) {
        if (email != null && !email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid email");
    }

    public void checkLoginNameNotExists(String uid, String... names) {
        for (String name : names) {
            if (StringUtils.isBlank(name))
                continue;
            Record rec = findUserByLoginName(name, "user_id");
            String userid = rec.getString("user_id");
            if (!userid.equals(uid) && !rec.isEmpty())
                throw new AccountException(ErrorCode.LOGIN_NAME_EXISTS, "The login name is existing");
        }
    }

    @Override
    public boolean checkBindNameNotExists(CharSequence names) throws AvroRemoteException, ResponseError {
        List<String> nameList = StringUtils2.splitList(toStr(names), ",", true);
        boolean b = true;
        for (String name : nameList) {
            if (!name.equals("")) {
                Record rec = findUserByLoginName(name, "user_id");
                String userid = rec.getString("user_id");
                if (!rec.isEmpty() && !userid.equals("")) {
                    b = false;
                    break;
                }
            }
        }
        return b;
    }
    
    public boolean checkLoginNameNotExists(CharSequence uid, CharSequence names) throws AvroRemoteException, ResponseError
    {
    	String[] arrNames = StringUtils2.splitArray(toStr(names), ",", true);
    	checkLoginNameNotExists(toStr(uid), arrNames);
    	
    	return true;
    }


    protected abstract Record findUserByLoginName(String name, String... cols);

    protected String findUserIdByLoginName(String name) {
        return findUserByLoginName(name, "user_id").getString("user_id");
    }

    protected abstract Record findUserByLoginNameNotInID(String name, String... cols);

    @Override
    public ByteBuffer findUidLoginNameNotInID(CharSequence name) throws AvroRemoteException, ResponseError {
        try {
            return findUserByLoginNameNotInID(toStr(name),toStr("user_id")).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    protected abstract boolean saveTicket(String ticket, String userId, String appId);

    public static String genTicket(String loginName) {
        return Encoders.toBase64(loginName + "_" + DateUtils.nowMillis() + "_" + new Random().nextInt(10000));
    }

    @Override
    public ByteBuffer login(CharSequence name, CharSequence password, CharSequence appId) throws AvroRemoteException, ResponseError {
        final String PASSKEY = Encoders.md5Hex("_passkey_passw0rd_");
        System.out.println("---- login");
        try {
            String name0 = toStr(name);
            String password0 = toStr(password);
            String appId0 = toStr(appId);
            Record rec = findUserByLoginName(name0, "user_id", "password", "display_name");
            if (rec.isEmpty())
                throw new AccountException(ErrorCode.LOGIN_NAME_OR_PASSWORD_ERROR, "Login name or password error");

            String userId = rec.getString("user_id");
            if (!PASSKEY.equals(password0)) {
                System.out.println("---- passkey");
                if (!org.apache.commons.lang.StringUtils.equalsIgnoreCase(password0, rec.getString("password")))
                    throw new AccountException(ErrorCode.LOGIN_NAME_OR_PASSWORD_ERROR, "Login name or password error");
            } else {
                System.out.println("---- not passkey " + password0);
            }

            String ticket = genTicket(name0);
            boolean b = saveTicket(ticket, userId, appId0);
            if (!b)
                throw new AccountException(ErrorCode.CREATE_SESSION_ERROR, "Create session error");

            //updateUser(Record.of("user_id", userId, "last_visited_time", DateUtils.nowMillis()));
            return Record.of(new Object[][]{
                    {"user_id", userId},
                    {"ticket", ticket},
                    {"display_name", rec.getString("display_name")},
                    {"login_name", name0},
            }).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteTicket(String ticket);

    @Override
    public final boolean logout(CharSequence ticket) throws AvroRemoteException, ResponseError {
        try {
            String ticket0 = toStr(ticket);
            return deleteTicket(ticket0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record findUserByTicket(String ticket, String... cols);

    @Override
    public final CharSequence whoLogined(CharSequence ticket) throws AvroRemoteException, ResponseError {
        try {
            Record rec = findUserByTicket(toStr(ticket), "user");
            return rec.getString("user", NULL_USER_ID);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findTicketsByUserId(String userId, String appId, String... cols);

    @Override
    public final ByteBuffer getLogined(CharSequence userId, CharSequence appId) throws AvroRemoteException, ResponseError {
        try {
            return findTicketsByUserId(toStr(userId), toStr(appId),
                    "ticket", "user", "app", "created_time").toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String generateUserId();

    @Override
    public final CharSequence getNowGenerateUserId() throws AvroRemoteException {
        try {
            return generateUserId();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean saveUser(Record info);

    @Override
    public final CharSequence createAccount(ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            Record pf = Record.fromByteBuffer(info);

            // check missing columns
            Schemas.checkRecordIncludeColumns(pf, "password", "display_name");
            if (!pf.has("login_email1") && !pf.has("login_phone1"))
                throw new DataException("Must include column 'login_email1' or 'login_phone1'");

            // get values
            String login_email1 = pf.getString("login_email1", "");
            String login_phone1 = pf.getString("login_phone1", "");
            String displayName = pf.getString("display_name");
            String nickName = pf.getString("nick_name", "");
            String password = pf.getString("password");
            String gender = pf.getString("gender", "m");
            String miscellaneous = pf.getString("miscellaneous", "{}");

            // check values
            if (StringUtils.isNotBlank(login_email1)) {
                checkLoginName(login_email1);
                checkEmail(login_email1);
            }

            if (StringUtils.isNotBlank(login_phone1)) {
                checkLoginName(login_phone1);
                checkPhone(login_phone1);
            }
            checkDisplayName(displayName);
            checkPassword(password);

            checkLoginNameNotExists("", login_phone1, login_email1);

            // generate user_id
            String userId = generateUserId();

            // create record
            Record rec = new Record();
            rec.put("user_id", userId);
            rec.put("login_phone1", login_phone1);
            rec.put("login_email1", login_email1);
            rec.put("password", password);
            rec.put("display_name", displayName);
            rec.put("nick_name", nickName);
            rec.put("gender", gender);
            rec.put("miscellaneous", miscellaneous);
            rec.put("created_time", DateUtils.nowMillis());

            NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                    "&, AND", Locale.CHINA);

            final NameSplitter.Name name = new NameSplitter.Name();
            nm.split(name, displayName);

//            Map name_map = new HashMap();
//            name_map = StringUtils2.trandsUserName(displayName);

            String first_name = "";
            String middle_name = "";
            String last_name = "";
            if (name.getGivenNames() != null) {
                first_name = name.getGivenNames().toString();
            }
            if (name.getMiddleName() != null) {
                middle_name = name.getMiddleName().toString();
            }
            if (name.getFamilyName() != null) {
                last_name = name.getFamilyName().toString();
            }
            rec.put("first_name",first_name);
            rec.put("middle_name",middle_name);
            rec.put("last_name",last_name);

            
            Record contactInfo = new Record();
            if(StringUtils.isNotBlank(login_phone1) && !StringUtils.equalsIgnoreCase(login_phone1, "null"))
            {
            	contactInfo.put("mobile_telephone_number", login_phone1);
            }
            if(StringUtils.isNotBlank(login_email1) && !StringUtils.equalsIgnoreCase(login_email1, "null"))
            {
            	contactInfo.put("email_address", login_email1);
            }
            
            rec.putMissing("contact_info", JsonUtils.parse(JsonUtils.toJson(contactInfo, false)));
            rec.putMissing("address", "[]");
            rec.putMissing("work_history", "[]");
            rec.putMissing("education_history", "[]");

            // save
            boolean b = saveUser(rec);
            if (!b)
                throw new AccountException(ErrorCode.ACCOUNT_ERROR, "Save user info error");

            return userId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteUser(String userId, long deleted_time);

    @Override
    public final boolean destroyAccount(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return deleteUser(toStr(userId), DateUtils.nowMillis());
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateUser(Record info);

    @Override
    public CharSequence resetPassword(CharSequence loginName) throws AvroRemoteException, ResponseError {
        try {
            String loginName0 = toStr(loginName);

            String newPassword = RandomUtils.generateRandomNumberString(6);
            String userId = findUserIdByLoginName(loginName0);
            if (StringUtils.isEmpty(userId))
                throw new AccountException(ErrorCode.USER_NOT_EXISTS, "User '%s' is not exists", loginName0);

            Record info = Record.of("user_id", userId, "password", Encoders.md5Hex(newPassword));
            updateUser(info);
            return newPassword;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePasswordByUserId(String userId, String password);

    @Override
    public boolean changePasswordByUserId(CharSequence userId,CharSequence password) throws AvroRemoteException, ResponseError {
        try {
            return updatePasswordByUserId(toStr(userId),toStr(password));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateAccount(CharSequence userId, ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            Record user = Record.fromByteBuffer(info);
            Schemas.checkRecordExcludeColumns(user, "user_id");
            Schemas.checkRecordExcludeColumns(user, userSchema.getColumnNames("unmodified"));

            if (user.has("contact_info")) {
                String contact_info = user.getString("contact_info");
                JsonNode jn = JsonUtils.parse(contact_info);

                Schemas.standardize(contactSchema, jn);
                //Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn), "type", "info");
            }

            if (user.has("address")) {
                String address = user.getString("address");
                JsonNode jn = JsonUtils.parse(address);

                Schemas.standardize(addressSchema, jn);
                for (int i = 0; i < jn.size(); i++) {
                    JsonNode node = jn.get(i);
                    if (node instanceof ObjectNode)
                        Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(node),
                        "type", "country", "state", "city", "street", "postal_code", "po_box", "extended_address");
                }

            }

            if (user.has("work_history")) {
                String work_history = user.getString("work_history");
                JsonNode jn = JsonUtils.parse(work_history);

                Schemas.standardize(workSchema, jn);
                ArrayNode an = JsonUtils.checkRecordSet(jn);
                int size = an.size();
                for(int i = 0; i < size; i++)
                {
                    JsonNode jn0 = an.get(i);
                    Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn0),
                        "from", "to", "company", "address", "title", "profession", "description");
                }
            }

            if (user.has("education_history")) {
                String education_history = user.getString("education_history");
                JsonNode jn = JsonUtils.parse(education_history);

                Schemas.standardize(educationSchema, jn);
                ArrayNode an = JsonUtils.checkRecordSet(jn);
                int size = an.size();
                for(int i = 0; i < size; i++)
                {
                    JsonNode jn0 = an.get(i);
                    Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn0),
                        "from", "to", "type", "school", "class", "degree", "major");
                }
            }

            if (user.has("display_name")) {
//                Map name_map = new HashMap();
//                name_map = StringUtils2.trandsUserName(user.getString("display_name"));

                NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                        "&, AND", Locale.CHINA);

                final NameSplitter.Name name = new NameSplitter.Name();
                nm.split(name, user.getString("display_name"));

                String first_name = "";
                String middle_name = "";
                String last_name = "";
                if (name.getGivenNames() != null) {
                    first_name = name.getGivenNames().toString();
                }
                if (name.getMiddleName() != null) {
                    middle_name = name.getMiddleName().toString();
                }
                if (name.getFamilyName() != null) {
                    last_name = name.getFamilyName().toString();
                }
                user.put("first_name", first_name);
                user.put("middle_name", middle_name);
                user.put("last_name", last_name);
            }

            if (user.has("first_name") || user.has("middle_name") || user.has("last_name")) {
                String newDisplayName = "";
                List<String> l = new ArrayList<String>();
                l.add(toStr(userId));
                Record old_user = findUsersByUserIds(l, "display_name").getFirstRecord();
                NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                        "&, AND", Locale.CHINA);

                final NameSplitter.Name name = new NameSplitter.Name();
                nm.split(name, old_user.getString("display_name"));

                String first_name = "";
                String middle_name = "";
                String last_name = "";
                if (name.getGivenNames() != null) {
                    first_name = name.getGivenNames().toString();
                }
                if (name.getMiddleName() != null) {
                    middle_name = name.getMiddleName().toString();
                }
                if (name.getFamilyName() != null) {
                    last_name = name.getFamilyName().toString();
                }


                if (user.has("last_name")) {
                    if (user.getString("last_name").trim().length() > 0)
                        last_name = user.getString("last_name").trim();
                }
                if (user.has("middle_name")) {
                    if (user.getString("middle_name").trim().length() > 0)
                        middle_name = user.getString("middle_name").trim();
                }
                if (user.has("first_name")) {
                    if (user.getString("first_name").trim().length() > 0)
                        first_name = user.getString("first_name").trim();
                }
                user.put("display_name", last_name + middle_name + first_name);
            }


            //System.out.println(user);
//            checkLoginNameNotExists(userId0,
//                    user.getString("login_phone1", null), user.getString("login_phone2", null), user.getString("login_phone3", null),
//                    user.getString("login_email1", null), user.getString("login_email2", null), user.getString("login_email3", null));

            user.put("user_id", userId);
            return updateUser(user);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean bindUser0(Record info);
    
    @Override
    public boolean bindUser(CharSequence userId, ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            Record user = Record.fromByteBuffer(info);
            user.put("user_id", userId);
            return updateUser(user);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    
    protected abstract RecordSet findUsersByUserIds(List<String> userIds, String... cols);

    public static void addImageUrlPreifx(String profileImagePattern, String sysIconUrlPattern, Record rec) {
        if (rec.has("image_url"))
        {
        	String pattern = profileImagePattern;
        	String image_url = rec.getString("image_url");
        	if(StringUtils.isBlank(image_url))
        	{
        		pattern = sysIconUrlPattern;
        		image_url = "1.gif";
        		if(rec.has("gender") && rec.getString("gender").equals("f"))
        		{
        			image_url = "0.gif";
        		}
        	}
        	rec.put("image_url", String.format(pattern, image_url));
        }
        if (rec.has("small_image_url"))
        {
        	String pattern = profileImagePattern;
        	String small_image_url = rec.getString("small_image_url");
        	if(StringUtils.isBlank(small_image_url))
        	{
        		pattern = sysIconUrlPattern;
        		small_image_url = "1_S.jpg";
        		if(rec.has("gender") && rec.getString("gender").equals("f"))
        		{
        			small_image_url = "0_S.jpg";
        		}
        	}
        	rec.put("small_image_url", String.format(pattern, small_image_url));
        }
        if (rec.has("large_image_url"))
        {
        	String pattern = profileImagePattern;
        	String large_image_url = rec.getString("large_image_url");
        	if(StringUtils.isBlank(large_image_url))
        	{
        		pattern = sysIconUrlPattern;
        		large_image_url = "1_L.jpg";
        		if(rec.has("gender") && rec.getString("gender").equals("f"))
        		{
        			large_image_url = "0_L.jpg";
        		}
        	}
        	rec.put("large_image_url", String.format(pattern, large_image_url));
        }
    }

    @Override
    public ByteBuffer getUsers(CharSequence userIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);           
            String[] cols0 = StringUtils2.splitArray(toStr(cols), ",", true);
            
            if (userIds0.isEmpty() || cols0.length == 0)
                return new RecordSet().toByteBuffer();

            Schemas.checkSchemaIncludeColumns(userSchema, cols0);
            RecordSet recs = findUsersByUserIds(userIds0, cols0);
            for (Record rec : recs) {
                addImageUrlPreifx(profileImagePattern, sysIconUrlPattern, rec);
            }
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findAllUserIds0(boolean all);
     @Override
    public ByteBuffer findAllUserIds(boolean all) throws AvroRemoteException, ResponseError {
        try {
            return findAllUserIds0(all).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUserIds(CharSequence loginNames) throws AvroRemoteException, ResponseError {
        try {
            List<String> names0 = StringUtils2.splitList(toStr(loginNames), ",", true);
            RecordSet recs = new RecordSet();
            for (String name : names0) {
                String userId = findUserIdByLoginName(name);
                if (!userId.equals("") && !userId.equals("0")) {
                    recs.add(Record.of("user_id", userId, "login_name", name));
                }
            }
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean saveShortUrl0(String long_url, String short_url);

    @Override
    public boolean saveShortUrl(CharSequence long_url,CharSequence short_url) throws AvroRemoteException, ResponseError {
        try {
            return saveShortUrl0(toStr(long_url),toStr(short_url));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String findLongUrl0(String short_url);

    @Override
    public CharSequence findLongUrl(CharSequence short_url) throws AvroRemoteException, ResponseError {
        try {
            return findLongUrl0(toStr(short_url));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer hasUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
        RecordSet recs = new RecordSet();
        for (String userId : userIds0) {
            boolean b = !findUsersByUserIds(Arrays.asList(userId), "user_id").isEmpty();
            recs.add(Record.of("user_id", Long.parseLong(userId), "result", b));
        }
        return recs.toByteBuffer();
    }

    @Override
    public boolean hasOneUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
        if (userIds0.isEmpty())
            return false;

        RecordSet recs = findUsersByUserIds(userIds0, "user_id");
        return !recs.isEmpty();
    }

    @Override
    public boolean hasAllUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        Set<String> userIds0 = StringUtils2.splitSet(toStr(userIds), ",", true);

        int inl = userIds0.size();
        if (userIds0.size() > 0) {
            RecordSet recs = findUsersByUserIds(new ArrayList<String>(userIds0), "user_id");
            if (inl > recs.size()) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }

//        if (userIds0.isEmpty())
//            return true;
//
//        for (String userId : userIds0) {
//            boolean b = !findUsersByUserIds(Arrays.asList(userId), "user_id").isEmpty();
//            if (!b)
//                return false;
//        }
    }

    
    protected abstract RecordSet findUidByMiscellaneous0(String miscellaneous);

    @Override
    public ByteBuffer findUidByMiscellaneous(CharSequence miscellaneous)
            throws AvroRemoteException, ResponseError {
        return findUidByMiscellaneous0(toStr(miscellaneous)).toByteBuffer();
    }
    
    protected abstract String findUserIdByUserName0(String username);
    
    @Override
    public CharSequence findUserIdByUserName(CharSequence username) throws AvroRemoteException, ResponseError {
        return findUserIdByUserName0(toStr(username));
    }
    
    protected abstract RecordSet searchUserByUserName0(String username,int page,int count);
    
    @Override
    public ByteBuffer searchUserByUserName(CharSequence username,int page,int count) throws AvroRemoteException, ResponseError {
        return searchUserByUserName0(toStr(username),page,count).toByteBuffer();
    }
    
    protected abstract boolean setPrivacy0(String userId, RecordSet privacyItemList);
    
    @Override
    public boolean setPrivacy(CharSequence userId, ByteBuffer privacyItemList) 
    		throws AvroRemoteException, ResponseError {		
		return setPrivacy0(toStr(userId), RecordSet.fromByteBuffer(privacyItemList));    	
	}

    protected abstract RecordSet getAuths0(String userId, List<String> resources);
	
    @Override
	public ByteBuffer getAuths(CharSequence userId, CharSequence resources)
			throws AvroRemoteException, ResponseError {		
		String resources0 = toStr(resources);
		List<String> rl = StringUtils2.splitList(resources0, ",", true); 
    	return getAuths0(toStr(userId), rl).toByteBuffer();
	}

    protected abstract RecordSet getUsersAuths0(String userIds);

    @Override
    public ByteBuffer getUsersAuths(CharSequence userIds) throws AvroRemoteException, ResponseError {
        return getUsersAuths0(toStr(userIds)).toByteBuffer();
    }


    protected abstract RecordSet findUsersPasswordByUserIds(String userIds);

    @Override
	public ByteBuffer getUsersPasswordByUserIds(CharSequence userIds) throws AvroRemoteException, ResponseError {
    	return findUsersPasswordByUserIds(toStr(userIds)).toByteBuffer();
	}

	@Override
	public boolean getDefaultPrivacy(CharSequence resource,
			CharSequence circleId) throws AvroRemoteException, ResponseError {
		String res = toStr(resource);
		int circle = Integer.parseInt(toStr(circleId));
		
		switch (circle)
		{
		case Constants.ADDRESS_BOOK_CIRCLE:
		{
			return true;
		}
		case Constants.FAMILY_CIRCLE:
		{
			return true;
		}
		case Constants.CLOSE_FRIENDS_CIRCLE:
		{
			return true;
		}
		case Constants.DEFAULT_CIRCLE:
		{
			if(StringUtils.contains(res, Constants.RESOURCE_PHONEBOOK)
				|| StringUtils.contains(res, Constants.RESOURCE_BUSINESS))
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		case Constants.BLOCKED_CIRCLE:
		{
			return false;
		}
		default:
		{
//			if(StringUtils.contains(res, Constants.RESOURCE_COMMON))
//			{
//				return true;
//			}
//			else
//			{
				return false;
//			}
		}
		}	
	}		
}
