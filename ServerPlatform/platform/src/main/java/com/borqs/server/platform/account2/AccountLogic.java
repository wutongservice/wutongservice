package com.borqs.server.platform.account2;


import java.sql.SQLException;
import java.util.List;

public interface AccountLogic{
    User createUser( User user);

    boolean destroyUser(CharSequence userId);

    boolean recoverUser(CharSequence userId);

    boolean update( User user);

    String resetRandomPassword(CharSequence userId);

    void updatePassword(CharSequence userId, String oldPwd, String newPwd, boolean verify);

    List<User> getUsers( long... userIds);

    User getUser( long userId);

    String getPassword( long userId);

    boolean hasAllUser( long... userIds);

    boolean hasAnyUser( long... userIds);

    boolean hasUser( long userId);

    long[] getExistsIds( long... userIds) throws SQLException;
}
