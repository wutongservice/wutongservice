package com.borqs.notifications.test.dao;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLTestDao implements IDataStoreTestDao {
	private static final String COUNT_WITH_STATUS = "SELECT count(*) c FROM `informations` WHERE receiverId=%s AND processed=%s";
	private static final String COUNT_WITHOUT_STATUS = "SELECT count(*) c FROM `informations` WHERE receiverId=%s";
	private static final String COUNT_WITH_GUID = "SELECT count(*) c FROM `informations` WHERE guid='%s'";

	private static final String COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s";
	
	private static final String COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s";
	
	private static final String SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
	private static final String SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";
	
	private static final String SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
	private static final String SQL_LIST_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	
	private static final String SQL_LIST_BY_MID_LASTEST = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
	private static final String SQL_LIST_BY_MID_PRE = "SELECT * FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	private static final String SQL_LIST_BY_MID_FWD = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	
//	private static final String SQL_QUERY_BY_APPID_TYPE_RECEIVERID_OBJECTID = "SELECT * FROM `informations` WHERE appId=%s AND type=%s AND receiverId=%s AND object_id=%s ORDER BY ID DESC";
//	private static final String SQL_DELETE_BY_APPID_TYPE_RECEIVERID_OBJECTID = "DELETE FROM `informations` WHERE appId=%s AND type=%s AND receiverId=%s AND object_id=%s";
//
//	private static final String SQL_EXIST_BY_GUID = "SELECT id FROM `informations` WHERE guid=%s";
	
	private static final String SQL_INSERT = "INSERT INTO informations(" +
			"appId,senderId,receiverId,type,action,date,title,data,uri,processed,process_method,importance,title_html,body,body_html,object_id,last_modified,guid) " +
			"VALUES(%s,%s,%s,'%s','%s',NOW(),'%s','%s','%s',%s,%s,%s,'%s','%s','%s','%s',NOW(),'%s')";
	private static final String SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET processed=TRUE,last_modified=NOW()";
	private static final String SQL_UPDATE_READ_STATUS = "UPDATE informations SET `read`=TRUE,last_modified=NOW()";
	
	private static final String SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type='%s',action='%s'" +
			",title='%s',data='%s',uri='%s',processed=%s,process_method=%s,importance=%s,title_html='%s',body='%s',body_html='%s',last_modified=NOW() WHERE guid='%s'";
	
	// update by appId, type, receiverId, objectId
	private static final String SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action='%s'" +
			",title='%s',data='%s',uri='%s',processed=%s,process_method=%s,importance=%s,title_html='%s',body='%s',body_html='%s',last_modified=NOW(),`read`=FALSE " +
			"WHERE appId=%s and type='%s' and receiverId=%s";
	
	private static final String SQL_DELETE_BY_ID = "DELETE FROM `informations` WHERE id=%s";

	private static final String SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String SQL_LIST_BY_TIME_PRE = "SELECT * FROM `informations` WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
	private static final String SQL_LIST_BY_TIME_FWD = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";

	private static final String SQL_UPDATE_READ_BY_TIME = "UPDATE `informations` SET `read`=TRUE WHERE id IN (SELECT id FROM ( %s ) temp) AND process_method=1";

	private static final String COUNT_FWD_WITH_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=%s AND receiverId=%s ORDER BY last_modified DESC";

	private static final String COUNT_PRE_WITH_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<%s AND receiverId=%s ORDER BY last_modified DESC";
	
	private static final String CLEAR_DATABASE = "DELETE FROM informations";
	
	private static String URL = "jdbc:mysql://127.0.0.1:3306/informations?useUnicode=true&characterEncoding=UTF-8";
	private static final String USER_NAME = "information";
	private static final String PASSWORD = "information2008";
	
	private static String APP_ID = "111";
	private static String SENDER_ID = "10208";
	private static String RECEIVER_ID = "10208";
	private static String TYPE = "test.mysql";
	private static String ACTION = "test.action";
	private static String TITLE = "this is a title";
	private static String DATA = "this is a data";
	private static String URI = "http://uri";
	private static int PROCESSED = 0;
	private static int PROCESSED_METHOD = 1;
	private static int IMPORTANCE = 30;
	private static String TITLE_HTML = "<div>this is a html title</div>";
	private static String BODY = "this is a body";
	private static String BODY_HTML = "<div>this is a body</div>";
	private static String GUID = "com.borqs.notification.test.guid";
	
	private static long total = 10000;
	private static long increase = 1000;
	private static long current = 0;
	
	private Connection conn;
	private BufferedWriter writer;
	
	private String RECORD_FILE_NAME = "notifications_mysql_test_"+total+".txt";
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#init()
	 */
	public void init() throws Exception {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
			// open file
			writer = new BufferedWriter(new FileWriter(RECORD_FILE_NAME));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#clearDatabase()
	 */
	public void clearDatabase() {
		Statement stmt = null;
		String sql = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(CLEAR_DATABASE);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		IDataStoreTestDao dao = new MySQLTestDao();
		try {
			dao.init();
			dao.clearDatabase();
			while(current<total) {
				current += increase;
				dao.testInsert(increase);
				dao.testCount();
				dao.testQuery();
				dao.testUpdate();
//				dao.testDelete();
				dao.endPhase();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dao.destroy();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#endPhase()
	 */
	public void endPhase() {
		try {
			writer.write("\n\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void query(Statement stmt, String sql) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			ResultSet res = stmt.executeQuery(sql);
			endTime = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#testCount()
	 */
	public void testCount() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		
		Statement stmt = null;
		String sql = null;
		try {
			stmt = conn.createStatement();
			// count with status
			sql = String.format(COUNT_WITH_STATUS, RECEIVER_ID, 1);
			writer.write("COUNT_WITH_STATUS: ");
			query(stmt, sql);
			
			// count without status
			sql = String.format(COUNT_WITHOUT_STATUS, RECEIVER_ID);
			writer.write("COUNT_WITHOUT_STATUS: ");
			query(stmt, sql);
			
			// count with QUID
			sql = String.format(COUNT_WITH_GUID, GUID);
			writer.write("COUNT_WITH_GUID: ");
			query(stmt, sql);
			
//			COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s";
			sql = String.format(COUNT_PRE_WITH_STATUS_BY_POS, Long.MAX_VALUE, RECEIVER_ID, 1);
			writer.write("COUNT_PRE_WITH_STATUS_BY_POS: ");
			query(stmt, sql);
			
//			COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s";
			sql = String.format(COUNT_PRE_WITHOUT_STATUS_BY_POS, Long.MAX_VALUE, RECEIVER_ID);
			writer.write("COUNT_PRE_WITHOUT_STATUS_BY_POS: ");
			query(stmt, sql);
			
//			COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s";
			sql = String.format(COUNT_FWD_WITH_STATUS_BY_POS, Long.MIN_VALUE, RECEIVER_ID, 1);
			writer.write("COUNT_FWD_WITH_STATUS_BY_POS: ");
			query(stmt, sql);
			
//			COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s";
			sql = String.format(COUNT_FWD_WITHOUT_STATUS_BY_POS, Long.MIN_VALUE, RECEIVER_ID);
			writer.write("COUNT_FWD_WITHOUT_STATUS_BY_POS: ");
			query(stmt, sql);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			writer.write("-----------Test Count "+current+" End -------------\n");
		}
	}
	
	public void testQuery() throws Exception {
		writer.write("-----------Test List "+current+" Begin -------------\n");
		
		Statement stmt = null;
		String sql = null;
		try {
			stmt = conn.createStatement();
//			SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
			sql = String.format(SQL_LIST_ALL_WITH_STATUS, RECEIVER_ID, 1);
			writer.write("SQL_LIST_ALL_WITH_STATUS: ");
			query(stmt, sql);
			
//			SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";
			sql = String.format(SQL_LIST_ALL_WITHOUT_STATUS, RECEIVER_ID);
			writer.write("SQL_LIST_ALL_WITHOUT_STATUS: ");
			query(stmt, sql);
//			
//			SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
			sql = String.format(SQL_LIST_ALL_BY_RECEIVER, RECEIVER_ID, 1);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			sql = String.format(SQL_LIST_BY_RECEIVER, RECEIVER_ID, 1, 1, Long.MAX_VALUE);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_MID_LASTEST = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
			sql = String.format(SQL_LIST_BY_MID_LASTEST, Long.MIN_VALUE, RECEIVER_ID, 1);
			writer.write("SQL_LIST_BY_MID_LASTEST: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_MID_PRE = "SELECT * FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			sql = String.format(SQL_LIST_BY_MID_PRE, Long.MAX_VALUE, RECEIVER_ID, 1, 1, Long.MAX_VALUE);
			writer.write("SQL_LIST_BY_MID_PRE: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_MID_FWD = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			sql = String.format(SQL_LIST_BY_MID_FWD, Long.MIN_VALUE, RECEIVER_ID, 1, 1, Long.MAX_VALUE);
			writer.write("SQL_LIST_BY_MID_FWD: ");
			query(stmt, sql);
			
			long minT = System.currentTimeMillis() - 7 * 24 * 60 * 3600000;
//			SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
			sql = String.format(SQL_LIST_BY_TIME_LASTEST, minT, RECEIVER_ID, 1);
			writer.write("SQL_LIST_BY_TIME_LASTEST: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_TIME_PRE = "SELECT * FROM `informations` WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			sql = String.format(SQL_LIST_BY_TIME_PRE, System.currentTimeMillis(), RECEIVER_ID, 1, 1, Long.MAX_VALUE);
			writer.write("SQL_LIST_BY_TIME_PRE: ");
			query(stmt, sql);
			
//			SQL_LIST_BY_TIME_FWD = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			sql = String.format(SQL_LIST_BY_TIME_FWD, minT, RECEIVER_ID, 1, 1, Long.MAX_VALUE);
			writer.write("SQL_LIST_BY_TIME_FWD: ");
			query(stmt, sql);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			writer.write("-----------Test List "+current+" End -------------\n");
		}
	}


	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#testInsert(long)
	 */
	public void testInsert(long count) throws Exception {
		writer.write("-----------Test Insert "+current+" Begin -------------\n");
		
		long startTime = System.currentTimeMillis();
		long endTime = startTime;
		
		long idCount = 1;
		
		Statement stmt = null;
		String sql = null;
		try {
			stmt = conn.createStatement();
			startTime = System.currentTimeMillis();
			for(int i=0; i<count; i++) {
				sql = String.format(SQL_INSERT, 
						APP_ID, SENDER_ID, RECEIVER_ID, TYPE, ACTION,
						TITLE, DATA, URI, PROCESSED, PROCESSED_METHOD, IMPORTANCE,
						TITLE_HTML, BODY, BODY_HTML, idCount, GUID);
				stmt.execute(sql);
			}
			endTime = System.currentTimeMillis();
			
			StringBuffer sb = new StringBuffer();
			sb.append(startTime).append("\t")
				.append(endTime).append("\t")
				.append(endTime-startTime).append("\n");
			writer.write(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			writer.write("-----------Test Insert "+current+" End -------------\n");
		}
	}
	
	private void update(Statement stmt, String sql) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			int res = stmt.executeUpdate(sql);
			endTime = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#testUpdate()
	 */
	public void testUpdate() throws Exception {
		writer.write("-----------Test Update "+current+" Begin -------------\n");

		Statement stmt = null;
		String sql = null;
		try {
			stmt = conn.createStatement();
			
//			SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET processed=TRUE,last_modified=NOW()";
			writer.write("SQL_UPDATE_PROCESSED_STATUS: ");
			update(stmt, SQL_UPDATE_PROCESSED_STATUS);
			
//			SQL_UPDATE_READ_STATUS = "UPDATE informations SET `read`=TRUE,last_modified=NOW()";
			writer.write("SQL_UPDATE_READ_STATUS: ");
			update(stmt, SQL_UPDATE_READ_STATUS);
			
//			SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW() WHERE guid=%s";
			sql = String.format(SQL_UPDATE_BY_GUID,
					APP_ID, SENDER_ID, RECEIVER_ID, TYPE, ACTION,
					TITLE, DATA, URI, PROCESSED, PROCESSED_METHOD, IMPORTANCE,
					TITLE_HTML, BODY, BODY_HTML, GUID);
			writer.write("SQL_UPDATE_BY_GUID: ");
			update(stmt, sql);

//			// update by appId, type, receiverId, objectId
//			SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW(),`read`=FALSE " +
//					"WHERE appId=%s and type=%s and receiverId=%s";
			sql = String.format(SQL_UPDATE_BY_ATRO,
					SENDER_ID, ACTION, TITLE, DATA, URI, 
					PROCESSED, PROCESSED_METHOD, IMPORTANCE,
					TITLE_HTML, BODY, BODY_HTML, APP_ID, TYPE, RECEIVER_ID);
			writer.write("SQL_UPDATE_BY_ATRO: ");
			update(stmt, sql);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			writer.write("-----------Test Update "+current+" End -------------\n");
		}
	}
	
//	public void testDelete() throws Exception {
//		try {
//			writer.write("-----------Test Delete "+current+" Begin -------------\n");
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			writer.write("-----------Test Delete "+current+" End -------------\n");
//		}
//	}
	
	/* (non-Javadoc)
	 * @see com.borqs.notifications.test.dao.DSTestDao#destroy()
	 */
	public void destroy() {
		clearDatabase();
		
		if(null != conn) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if(null != writer) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
