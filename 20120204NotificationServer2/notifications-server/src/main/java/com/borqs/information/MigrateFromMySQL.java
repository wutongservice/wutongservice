package com.borqs.information;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.borqs.information.dao.InformationsStorageManager;
import com.borqs.information.dao.MongoDBStorage;
import com.borqs.information.rest.bean.Information;

public class MigrateFromMySQL {
	private static String mysqlUser = "syncservice";
	private static String mysqlPwd = "syncservice";
	private static String mysqlUrl = "jdbc:mysql://localhost:3306/syncservice?useUnicode=true&characterEncoding=UTF-8";

	private static String mongoHost = "127.0.0.1";
	private static int mongoPort = 27017;
//	private static String mongoDb = "notification";
//	private static String mongoCollection = "notifications";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i=0; i<args.length; i++) {
			if("--myurl".equalsIgnoreCase(args[i])) {
				mysqlUrl = args[++i];
			} else if("--myuser".equalsIgnoreCase(args[i])) {
				mysqlUser = args[++i];
			} else if("--mypwd".equalsIgnoreCase(args[i])) {
				mysqlPwd = args[++i];
			} else if("--mohost".equalsIgnoreCase(args[i])) {
				mongoHost = args[++i];
			} else if("--moport".equalsIgnoreCase(args[i])) {
				mongoPort = Integer.valueOf(args[++i]);
			} /*else if("--modb".equalsIgnoreCase(args[i])) {
				mongoDb = args[++i];
			} else if("--mocol".equalsIgnoreCase(args[i])) {
				mongoCollection = args[++i];
			}*/
		}
		
		System.out.println("mysql url is "+mysqlUrl);
		System.out.println("mysql user is "+mysqlUser);
		System.out.println("mysql pwd is "+mysqlPwd);
		
		System.out.println("mongodb host is "+mongoHost);
		System.out.println("mongodb port is "+mongoPort);
//		System.out.println("mongodb db is "+mongoDb);
//		System.out.println("mongodb collection is "+mongoCollection);
		
		
		final MongoDBStorage mongoDao = new MongoDBStorage();
		mongoDao.setHost(mongoHost);
		mongoDao.setPort(mongoPort);
		
		BasicDataSource dataSource = null;
		try {
			dataSource = new BasicDataSource();
			dataSource.setUrl(mysqlUrl);
			dataSource.setDriverClassName("com.mysql.jdbc.Driver");
			dataSource.setUsername(mysqlUser);
			dataSource.setPassword(mysqlPwd);

			JdbcTemplate jdbcTemplate = new JdbcTemplate();
			jdbcTemplate.setDataSource(dataSource);
			
			String sql = "SELECT * FROM informations";
			jdbcTemplate.query(sql, new RowCallbackHandler(){

				@Override
				public void processRow(ResultSet rs) throws SQLException {
					Information information = InformationsStorageManager.createInformation(rs);
					mongoDao.save(information);
					System.out.println(information);
				}
			});
			
			System.out.println("succeed to migrate data from mysql to mongodb");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("failed to migrate data from mysql to mongodb");
		} finally {
			try {
				dataSource.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			mongoDao.closeMongo();
		}
		
	}

}
