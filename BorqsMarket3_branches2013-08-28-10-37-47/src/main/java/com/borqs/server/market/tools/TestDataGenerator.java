package com.borqs.server.market.tools;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.DateTimeUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TestDataGenerator {
    public static void main(String[] args) throws Exception {
        Options cliOpts = makeCommandOptions();
        CommandLineParser parser = new PosixParser();
        if (args.length == 0) {
            printUsage(cliOpts);
            return;
        }

        CommandLine cli = parser.parse(cliOpts, args);
        try {
            String host = checkOption(cli, "h");
            String user = checkOption(cli, "u");
            String pwd = checkOption(cli, "p");
            String db = checkOption(cli, "d");

            Class.forName("com.mysql.jdbc.Driver");
            String url = String.format("jdbc:mysql://%s/%s?allowMultiQueries=true&characterEncoding=UTF-8", host, db);
            Connection conn = DriverManager.getConnection(url, user, pwd);
            conn.setAutoCommit(false);
            //conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Statement st = conn.createStatement();
            PreparedStatement pst;
            try {
                generateData(st);
                conn.commit();
            } catch (Exception e) {
                System.err.println("!!!ERROR");
                System.err.println(e.getMessage());
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
                st.close();
                conn.close();
            }
        } catch (ArgumentException e) {
            printUsage(cliOpts);
        } catch (ClassNotFoundException e) {
            System.err.println("Can't find mysql driver");
        }

    }

    private static String checkOption(CommandLine cli, String opt) throws ArgumentException {
        String val = cli.getOptionValue(opt);
        if (val == null)
            throw new ArgumentException();
        return val;
    }

    private static class ArgumentException extends Exception {
    }

    private static Options makeCommandOptions() {
        Options opts = new Options();
        opts.addOption("h", "host", true, "mysql host with port");
        opts.addOption("u", "user", true, "username");
        opts.addOption("p", "pwd", true, "password");
        opts.addOption("d", "db", true, "db name");
        return opts;
    }

    private static void printUsage(Options opts) {
        String usage = String.format("java -cp ... %s [opts]", TestDataGenerator.class.getName());
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, opts);
    }

    public static void generateData(Statement st) throws SQLException, IOException {
        String sql = loadTextInClasspath("com/borqs/server/market/resources/sqls/testData.sql");
        if (StringUtils.isBlank(sql))
            return;

        HashMap<String, String> vars = new HashMap<String, String>();
        vars.put(":now", Long.toString(DateTimeUtils.nowMillis()));
        for (Map.Entry<String, String> e : vars.entrySet()) {
            sql = StringUtils.replace(sql, e.getKey(), e.getValue());
        }

        System.out.println(sql);
        st.execute(sql);
    }

    private static String loadTextInClasspath(String res) throws IOException {
        InputStream in = TestDataGenerator.class.getClassLoader().getResourceAsStream(res);
        return IOUtils.toString(in, "UTF-8");
    }
}
