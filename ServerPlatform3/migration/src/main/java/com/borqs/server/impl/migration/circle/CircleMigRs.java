package com.borqs.server.impl.migration.circle;


import java.sql.ResultSet;
import java.sql.SQLException;

public class CircleMigRs {
    public static MigrationCircle readCircle(ResultSet rs, MigrationCircle circle0) throws SQLException {
        MigrationCircle circle = new MigrationCircle();
        int circleId = rs.getInt("circle");
        if(circleId<100)
            return null;
        circle.setCircle(circleId);
        circle.setName(rs.getString("name"));
        circle.setUpdated_time(rs.getLong("updated_time"));
        circle.setUser(rs.getLong("user"));

        return circle;
    }

}
