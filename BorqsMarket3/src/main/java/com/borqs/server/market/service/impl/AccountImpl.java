package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.FileStorageUtils;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Component("service.account")
public class AccountImpl extends ServiceSupport implements AccountService {
    private FileStorage accountStorage;

    public AccountImpl() {
    }

    public FileStorage getAccountStorage() {
        return accountStorage;
    }

    @Autowired
    @Qualifier("storage.account")
    public void setAccountStorage(FileStorage accountStorage) {
        this.accountStorage = accountStorage;
    }

    private String generateAccountId(long now) {
        return "a_" + StringUtils2.longToBase64(RandomUtils2.randomLongWith(now));
    }

    private static String generateTicket(String accountId, long now) {
        final String ticketEncryptKey = "Hvsd4nDmQwtxw5lT";
        String s = accountId + "|" + RandomUtils2.randomString(24) + "|" + now;
        return EncryptUtils.desEncryptBase64(s, ticketEncryptKey);
    }

    public static boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' || c <= 'f')))
                return false;
        }
        return true;
    }

    Record signup(RecordSession session, ServiceContext ctx, Record account) throws IOException {
        long now = DateTimeUtils.nowMillis();
        String id = account.asString("id");
        if (id == null) {
            id = generateAccountId(now);
            account.set("id", id);
        }

        String pwd = account.asString("password");
        if (pwd != null) {
            if (pwd.length() != 32 || !isHex(pwd))
                pwd = EncryptUtils.md5Hex(pwd);
        }

        String avatarImage = saveAvatarImage(account.get("avatar_image"), id);

        session.insert("market.createAccount", CC.map(
                "id=>", id,
                "now=>", now,
                "borqs=>", account.asBoolean("borqs", false),
                "name=>", account.asString("name"),
                "password=>", pwd,
                "avatar_image=>", avatarImage,
                "email=>", account.asString("email"),
                "phone=>", account.asString("phone"),
                "website=>", account.asString("website"),
                "google_id=>", account.asString("google_id"),
                "facebook_id=>", account.asString("facebook_id"),
                "twitter_id=>", account.asString("twitter_id"),
                "qq_id=>", account.asString("qq_id"),
                "weibo_id=>", account.asString("weibo_id")
        ));
        return account;
    }

    @Override
    public Record signup(final ServiceContext ctx, final Record account) {
        Validate.notNull(ctx);
        Validate.notNull(account);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return signup(session, ctx, account);
            }
        });
    }

    Record setPassword(RecordSession session, ServiceContext ctx, String id, String password, boolean includeDisabled) {
        long now = DateTimeUtils.nowMillis();
        session.update("market.updateAccount", CC.map(
                "id=>", id,
                "password=>", EncryptUtils.md5Hex(password),
                "include_disabled=>", includeDisabled,
                "now=>", now
        ));
        return Record.of("id=>", id, "updated_at=>", now);
    }

    @Override
    public Record setPassword(final ServiceContext ctx, final String id, final String password, final boolean includeDisabled) {
        Validate.notNull(ctx);
        Validate.notNull(id);
        Validate.notNull(password);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return setPassword(session, ctx, id, password, includeDisabled);
            }
        });
    }

    Record updateAccount(RecordSession session, ServiceContext ctx, Record account, boolean includeDisabled) {
        String id = account.asString("id");
        session.update("market.updateAccount", CC.map(
                "borqs=>", account.asBooleanWithNull("borqs"),
                "name=>", account.asString("name"),
                "avatar_image=>", account.asString("avatar_image"),
                "email=>", account.asString("email"),
                "phone=>", account.asString("phone"),
                "website=>", account.asString("website"),
                "google_id=>", account.asString("google_id"),
                "facebook_id=>", account.asString("facebook_id"),
                "twitter_id=>", account.asString("twitter_id"),
                "qq_id=>", account.asString("qq_id"),
                "weibo_id=>", account.asString("weibo_id")
        ));
        return getAccount(session, ctx, id, includeDisabled);
    }

    @Override
    public Record updateAccount(final ServiceContext ctx, final Record account, final boolean includeDisabled) {
        Validate.notNull(ctx);
        Validate.notNull(account);
        Validate.isTrue(account.hasField("id"));

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return updateAccount(session, ctx, account, includeDisabled);
            }
        });
    }

    Record disableAccount(RecordSession session, ServiceContext ctx, String id, boolean disabled) {
        long now = DateTimeUtils.nowMillis();
        long disabledAt = session.selectLongValue("market.getAccountDisabledAt", CC.map("id=>", id), 0L);
        if (disabled) {
            if (disabledAt == 0) {
                session.update("market.disableAccount", CC.map(
                        "now=>", now,
                        "id=>", id
                ));
                disabledAt = now;
            }
        } else {
            session.update("market.disableAccount", CC.map(
                    "now=>", 0L,
                    "id=>", id
            ));
            disabledAt = 0L;
        }
        return Record.of("id=>", id, "disabled_at", disabledAt);
    }

    @Override
    public Record disableAccount(final ServiceContext ctx, final String id, final boolean disabled) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return disableAccount(session, ctx, id, disabled);
            }
        });
    }

    Record findAccountIdAndPasswordBySigninId(RecordSession session, ServiceContext ctx, String signinId) {
        return session.selectOne("market.findAccountIdAndPasswordBySigninId", CC.map(
                "signin_id=>", signinId,
                "include_disabled=>", false
        ));
    }

    Record signin(RecordSession session, ServiceContext ctx, String signinId, String password) {
        long now = DateTimeUtils.nowMillis();

        Record account = findAccountIdAndPasswordBySigninId(session, ctx, signinId);
        if (account == null)
            throw new ServiceException(Errors.E_ACCOUNT, "signin id or password error");

        String currentPassword = account.asString("password");
        if (StringUtils.isEmpty(currentPassword))
            throw new ServiceException(Errors.E_ACCOUNT, "Missing password for the account");

        if (!currentPassword.equals(password) && !currentPassword.equalsIgnoreCase(EncryptUtils.md5Hex(password)))
            throw new ServiceException(Errors.E_ACCOUNT, "signin id or password error");

        String accountId = account.asString("id");
        String ticket = generateTicket(accountId, now);
        session.insert("market.createAccountTicket", CC.map(
                "ticket=>", ticket,
                "account_id=>", accountId,
                "now=>", now
        ));

        return Record.of(
                "signin_id=>", signinId,
                "id=>", accountId,
                "signin_at=>", now,
                "ticket=>", ticket
        );
    }

    @Override
    public Record signin(final ServiceContext ctx, final String signinId, final String password) {
        Validate.notNull(ctx);
        Validate.notNull(signinId);
        Validate.notNull(password);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return signin(session, ctx, signinId, password);
            }
        });
    }

    private String saveAvatarImage(Object o, String accountId) throws IOException {
        if (o == null) {
            return null;
        } else if (o instanceof String) {
            return (String) o;
        } else if (o instanceof FileItem) {
            return FileStorageUtils.saveAvatarImageWithFileItem(accountStorage, accountId, (FileItem) o);
        } else {
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal param avatar_image");
        }
    }

    Record activeAccount(RecordSession session, ServiceContext ctx, Params params) throws IOException {
        long now = DateTimeUtils.nowMillis();

        String thirdPartyIdColumn;
        String thirdPartyId;
        if (params.hasParam("google_id")) {
            thirdPartyIdColumn = "google_id";
            thirdPartyId = params.param("google_id").asString();
        } else if (params.hasParam("phone")) {
            thirdPartyIdColumn = "phone";
            thirdPartyId = params.param("phone").asString();
        } else {
            throw new ServiceException(Errors.E_ACCOUNT, "Missing third-party id");
        }
        Record account = session.selectOne("market.findAccountIdByThirdPartyId", CC.map(
                "third_party_id_column=>", thirdPartyIdColumn,
                "third_party_id=>", thirdPartyId
        ));

        boolean hasPassword;
        String accountId;
        if (account == null) {
            hasPassword = false;
            accountId = generateAccountId(now);
            String avatarImage = saveAvatarImage(params.get("avatar_image"), accountId);
            if (params.hasParam("google_id")) {
                session.insert("market.createAccount", CC.map(
                        "id=>", accountId,
                        "now=>", now,
                        "name=>", params.param("name").asString(),
                        "email=>", params.param("email").asString(),
                        "avatar_image=>", avatarImage,
                        "google_id=>", thirdPartyId
                ));
            } else if (params.hasParam("phone")) {
                session.insert("market.createAccount", CC.map(
                        "id=>", accountId,
                        "now=>", now,
                        "phone=>", thirdPartyId,
                        "borqs=>", params.param("_borqs_id_").asBoolean(false)
                ));
            }
        } else {
            if (account.asLong("disabled_at") != 0)
                throw new ServiceException(Errors.E_ACCOUNT, "The account is disabled");

            hasPassword = StringUtils.isNotEmpty(account.asString("password", null));
            accountId = account.asString("id");
        }

        String ticket = generateTicket(accountId, now);
        session.insert("market.createAccountTicket", CC.map(
                "ticket=>", ticket,
                "account_id=>", accountId,
                "now=>", now
        ));

        return Record.of(
                thirdPartyIdColumn + "=>", thirdPartyId,
                "id=>", accountId,
                "signin_at=>", now,
                "ticket=>", ticket,
                "has_password=>", hasPassword
        );
    }

    @Override
    public Record activeAccount(final ServiceContext ctx, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(params);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return activeAccount(session, ctx, params);
            }
        });
    }

    Record signout(RecordSession session, ServiceContext ctx, String ticket) {
        String accountId = session.selectStringValue("market.findAccountIdByTicket", CC.map("ticket=>", ticket), null);
        if (accountId != null) {
            session.delete("market.deleteAccountTicket", CC.map("ticket=>", ticket));
        }
        return Record.of("id=>", accountId, "ticket=>", ticket);
    }

    @Override
    public Record signout(final ServiceContext ctx, final String ticket) {
        Validate.notNull(ctx);
        Validate.notNull(ticket);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return signout(session, ctx, ticket);
            }
        });
    }

    Record getAccount(RecordSession session, ServiceContext ctx, String id, boolean includeDisabled) {
        return session.selectOne("market.findAccountById", CC.map(
                "id=>", id,
                "include_disabled=>", includeDisabled
        ));
    }

    @Override
    public Record getAccount(final ServiceContext ctx, final String id, final boolean includeDisabled) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getAccount(session, ctx, id, includeDisabled);
            }
        });
    }


    Record getAccountIdByTicket(RecordSession session, ServiceContext ctx, String ticket, boolean includeDisabled) {
        return session.selectOne("market.findAccountIdByTicket", CC.map(
                "ticket=>", ticket,
                "include_disabled=>", includeDisabled
        ), null);
    }

    @Override
    public Record getAccountIdByTicket(final ServiceContext ctx, final String ticket, final boolean includeDisabled) {
        Validate.notNull(ctx);
        Validate.notNull(ticket);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getAccountIdByTicket(session, ctx, ticket, includeDisabled);
            }
        });
    }


    Record getAccountByEmail(RecordSession session, ServiceContext ctx, String email, boolean includeDisabled) {
        return session.selectOne("market.findAccountByEmail", CC.map(
                "email=>", email,
                "include_disabled=>", includeDisabled
        ));
    }

    @Override
    public Record getAccountByEmail(final ServiceContext ctx, final String email, final boolean includeDisabled) {
        Validate.notNull(ctx);
        Validate.notNull(email);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getAccountByEmail(session, ctx, email, includeDisabled);
            }
        });
    }

    private boolean accountIsOperator(final String accountId) {
        return openSession(new RecordSessionHandler<Boolean>() {
            @Override
            public Boolean handle(RecordSession session) throws Exception {
                return session.selectBooleanValue("market.accountIsOperator", CC.map("account_id=>", accountId), false);
            }
        });
    }

    @Override
    public int getRoles(ServiceContext ctx) {
        int roles = ROLE_PURCHASER;
        String accountId = ctx.getAccountId();
        if (StringUtils.isNotEmpty(accountId)) {
            roles |= ROLE_PUBLISHER;
            if (ctx.isBorqs()) {
                roles |= ROLE_DEVELOPER;
            }
            if (ctx.isBorqs() && ctx.isBoss()) {
                roles |= ROLE_BOSS;
            }
            if (accountIsOperator(accountId)) {
                roles |= ROLE_OPERATOR;
            }
        }
        return roles;
    }

    Map<String, String> getAccountDisplayNames(RecordSession session, ServiceContext ctx, String[] ids, String defaultName) {
        HashMap<String, String> r = new HashMap<String, String>();
        if (ArrayUtils.isNotEmpty(ids)) {
            Records accounts = session.selectList("market.getAccountDisplayNames", CC.map("ids=>", Arrays.asList(ids)));
            for (Record account : accounts) {
                String id = account.asString("id");
                String name = account.asString("name");
                if (StringUtils.isEmpty(name)) {
                    name = account.asString("email");
                }
                if (StringUtils.isEmpty(name)) {
                    name = defaultName != null ? defaultName : "";
                }
                r.put(id, name);
            }
        }
        return r;
    }

    @Override
    public Map<String, String> getAccountDisplayNames(final ServiceContext ctx, final String[] ids, final String defaultName) {
        Validate.notNull(ctx);
        Validate.notNull(ids);

        return openSession(new RecordSessionHandler<Map<String, String>>() {
            @Override
            public Map<String, String> handle(RecordSession session) throws Exception {
                return getAccountDisplayNames(session, ctx, ids, defaultName);
            }
        });
    }

    void fillDisplayName(RecordSession session, ServiceContext ctx, Record rec, String accountIdCol, String displayNameCol) {
        fillDisplayName(session, ctx, new Records().append(rec), accountIdCol, displayNameCol);
    }

    void fillDisplayName(RecordSession session, ServiceContext ctx, Records recs, String accountIdCol, String displayNameCol) {
        String[] ids = recs.asStringArray(accountIdCol);
        Map<String, String> authorDisplayNames = getAccountDisplayNames(session, ctx, ids, "");
        for (Record rec : recs) {
            String authorName = MapUtils.getString(authorDisplayNames, rec.asString(accountIdCol), "");
            rec.put(displayNameCol, authorName);
        }
    }

    long getAccountsCount(RecordSession session, ServiceContext ctx) {
        return session.selectLongValue("market.getAccountsCount", 0);
    }

    @Override
    public long getAccountsCount(final ServiceContext ctx) {
        return openSession(new RecordSessionHandler<Long>() {
            @Override
            public Long handle(RecordSession session) throws Exception {
                return getAccountsCount(session, ctx);
            }
        });
    }

    //---------------code below is query in wutong_user table----------------------
    private Record getWutongUser(RecordSession session, ServiceContext ctx, String id, String email, boolean includeDisabled) {
        if (StringUtils.isNotBlank(id))
            return session.selectOne("wutong_user.findAccountById", CC.map(
                    "id=>", id,
                    "include_disabled=>", includeDisabled
            ));
        if (StringUtils.isNotBlank(email))
            return session.selectOne("wutong_user.findAccountByEmail", CC.map(
                    "email=>", email,
                    "include_disabled=>", includeDisabled
            ));
        return new Record();
    }

    @Override
    public Record getWutongUserByEmail(final ServiceContext ctx, final String email, final boolean includeDisabled) {
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getWutongUser(session, ctx, null, email, includeDisabled);
            }
        });
    }

    @Override
    public Record getWutongUserByUserId(final ServiceContext ctx, final String user_id, final boolean includeDisabled) {
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getWutongUser(session, ctx, user_id, null, includeDisabled);
            }
        });
    }
}
