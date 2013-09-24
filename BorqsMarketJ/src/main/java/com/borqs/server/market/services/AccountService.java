package com.borqs.server.market.services;


import com.borqs.server.market.ServiceException;
import static com.borqs.server.market.Errors.*;

import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.EncryptUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AccountService {

    private AccountProviderService publisherProvider;
    private AccountProviderService purchaserProvider;
    private AccountProviderService developerProvider;
    private AccountProviderService adminProvider;

    public AccountService() {
    }

    public AccountProviderService getPublisherProvider() {
        return publisherProvider;
    }

    @Autowired
    @Qualifier("service.publisherAccountProvider")
    public void setPublisherProvider(AccountProviderService publisherProvider) {
        this.publisherProvider = publisherProvider;
    }

    public AccountProviderService getPurchaserProvider() {
        return purchaserProvider;
    }

    @Autowired
    @Qualifier("service.purchaserAccountProvider")
    public void setPurchaserProvider(AccountProviderService purchaserProvider) {
        this.purchaserProvider = purchaserProvider;
    }

    public AccountProviderService getDeveloperProvider() {
        return developerProvider;
    }

    @Autowired
    @Qualifier("service.developerAccountProvider")
    public void setDeveloperProvider(AccountProviderService developerProvider) {
        this.developerProvider = developerProvider;
    }

    public AccountProviderService getAdminProvider() {
        return adminProvider;
    }

    @Autowired
    @Qualifier("service.adminAccountProvider")
    public void setAdminProvider(AccountProviderService adminProvider) {
        this.adminProvider = adminProvider;
    }

    public AccountProviderService getProviderByRole(int role) {
        if (role == UserId.ROLE_PURCHASER) {
            return purchaserProvider;
        } else if (role == UserId.ROLE_PUBLISHER) {
            return publisherProvider;
        } else if (role == UserId.ROLE_DEVELOPER) {
            return developerProvider;
        } else if (role == UserId.ROLE_ADMIN) {
            return adminProvider;
        } else {
            throw new IllegalArgumentException("Illegal role");
        }
    }

    public UserId signUp(ServiceContext ctx, UserId uid, String password) throws ServiceException {
        Validate.notNull(uid);
        password = ObjectUtils.toString(password);
        int role = uid.getRole();
        String id = getProviderByRole(role).signUp(ctx, uid.getId(), password);
        return new UserId(role, id);
    }

    public UserId deleteUser(ServiceContext ctx, UserId uid) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String signIn(ServiceContext ctx, UserId uid, String password) throws ServiceException {
        Validate.notNull(uid);
        Validate.notNull(password);

        long now = DateTimeUtils.nowMillis();
        int role = uid.getRole();
        String providerTicket = getProviderByRole(role).signIn(ctx, uid.getId(), password);
        if (providerTicket == null)
            throw new ServiceException(E_ACCOUNT, "Id or password error");
        return new Ticket(role, providerTicket, now).toString();
    }

    public UserId signOut(ServiceContext ctx, String ticket) throws ServiceException {
        Validate.notNull(ticket);
        Ticket t = Ticket.parse(ticket);
        int role = t.role;
        String providerUserId = getProviderByRole(role).signOut(ctx, t.providerTicket);
        if (providerUserId == null)
            throw new ServiceException(E_ACCOUNT, "Illegal ticket");
        return new UserId(role, providerUserId);
    }

    public UserId getUserId(ServiceContext ctx, String ticket) throws ServiceException {
        Validate.notNull(ticket);
        Ticket t = Ticket.parse(ticket);
        int role = t.role;
        String providerUserId = getProviderByRole(role).getUserId(ctx, t.providerTicket);
        if (ticket == null)
            throw new ServiceException(E_ACCOUNT, "Illegal ticket");
        return new UserId(role, providerUserId);
    }

    private static final Map<String, String> encryptKeys = makeEncryptKeys();
    private static Map<String, String> makeEncryptKeys() {
        HashMap<String, String> versions = new HashMap<String, String>();
        versions.put("01", "d9ad4CSlhROdI59e");
        return versions;
    }

    public static class Ticket {
        public final int role;
        public final String providerTicket;
        public final long signInAt;

        public Ticket(int role, String providerTicket, long signInAt) {
            this.role = role;
            this.providerTicket = providerTicket;
            this.signInAt = signInAt;
        }

        @Override
        public String toString() {
            String raw = String.format("%s\t%s\t%s", role, providerTicket, signInAt);
            final String keyVersion = "01";
            return keyVersion + EncryptUtils.desEncryptBase64(raw, encryptKeys.get(keyVersion));
        }

        public static Ticket parse(String ticket) {
            String keyVersion = ticket.substring(0, 2);
            String key = encryptKeys.get(keyVersion);
            if (key == null)
                throw new IllegalArgumentException("Unknown key version");
            String raw = EncryptUtils.desDecryptBase64(ticket.substring(2), key);
            String[] ss = StringUtils.split(raw, '\t');
            return new Ticket(Integer.parseInt(ss[0]), ss[1], Long.parseLong(ss[2]));
        }
    }
}
