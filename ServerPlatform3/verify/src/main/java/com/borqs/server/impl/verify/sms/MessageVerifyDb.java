package com.borqs.server.impl.verify.sms;


import com.borqs.server.platform.sql.SqlSupport;
import com.borqs.server.platform.sql.Table;

public class MessageVerifyDb extends SqlSupport {
    private Table verificationTable;



    public MessageVerifyDb() {
    }

    public Table getVerificationTable() {
        return verificationTable;
    }

    public void setVerificationTable(Table verificationTable) {
        this.verificationTable = verificationTable;
    }


}
