package com.borqs.server.impl.opline;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.opline.Operation;
import com.borqs.server.platform.feature.opline.Operations;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OpLineRs {

    public static Operations readOperations(ResultSet rs, Operations reuse) throws SQLException {
        if (reuse == null)
            reuse = new Operations();

        while (rs.next()) {
            Operation op = readOperationHelper(rs);
            reuse.add(op);
        }
        return reuse;
    }

    public static Operation readOperation(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return readOperationHelper(rs);
        } else {
            return null;
        }
    }

    private static Operation readOperationHelper(ResultSet rs) throws SQLException {
        Operation op = new Operation();
        op.setOperId(rs.getLong("oper_id"));
        op.setUserId(rs.getLong("user"));
        op.setAsId(rs.getString("as_"));
        op.setAction(rs.getInt("action"));
        op.setFlag(rs.getInt("flag"));
        op.setInfo(rs.getString("info"));
        String s = rs.getString("targets");
        op.setTargets(Target.parseCompatibleStringToArray(s, ","));
        return op;
    }
}
