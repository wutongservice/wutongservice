package com.borqs.notifications.test.dao;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MongoDBTestDao implements IDataStoreTestDao {
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
	private static final int READ = 0;
	private static int IMPORTANCE = 30;
	private static String TITLE_HTML = "<div>this is a html title</div>";
	private static String BODY = "this is a body";
	private static String BODY_HTML = "<div>this is a body</div>";
	private static String GUID = "com.borqs.notification.test.guid";

	private static long total = 1000000;
	private static long increase = 100000;
	private static long current = 0;

	private static final String HOST = "127.0.0.1";
	private static final int PORT = 27017;
	private static final String DB_NAME = "informations";
	private static final String COLLECTION = "informations";
	private Mongo mongo;
	private DB db;
	private DBCollection collection;
	
	private BufferedWriter writer;
	
	private String RECORD_FILE_NAME = "notifications_mongodb_test_"+total+".txt";
	private int total_rows;
	
	public void init() throws Exception {
		try {
			mongo = new Mongo(HOST, PORT);
			db = mongo.getDB(DB_NAME);
			collection = db.getCollection(COLLECTION);
			
			// open file
			writer = new BufferedWriter(new FileWriter(RECORD_FILE_NAME));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void clearDatabase() {
		if(null != collection) {
			collection.drop();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IDataStoreTestDao dao = new MongoDBTestDao();
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

	public void endPhase() {
		try {
			writer.write("\n\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void count(DBObject query) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			collection.count(query);
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
	
	private DBCursor query(DBObject query) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			DBCursor cursor = collection.find(query);
			endTime = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
			
			return cursor;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void testCount() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");

		try {
			// count with status
			DBObject query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("COUNT_WITH_STATUS: ");
			count(query);
			
			// count without status
			query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			writer.write("COUNT_WITHOUT_STATUS: ");
			count(query);
			
			// count without GUID
			query = new BasicDBObject();
			query.put("guid", GUID);
			writer.write("COUNT_WITH_GUID: ");
			count(query);
			
//		COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$lt", Long.MAX_VALUE));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("COUNT_PRE_WITH_STATUS_BY_POS: ");
			count(query);
			
//		COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$lt", Long.MAX_VALUE));
			query.put("receiverId", RECEIVER_ID);
			writer.write("COUNT_PRE_WITHOUT_STATUS_BY_POS: ");
			count(query);
			
//		COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gt", Long.MIN_VALUE));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("COUNT_FWD_WITH_STATUS_BY_POS: ");
			count(query);
			
//		COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gt", Long.MIN_VALUE));
			query.put("receiverId", RECEIVER_ID);
			writer.write("COUNT_FWD_WITHOUT_STATUS_BY_POS: ");
			count(query);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.write("-----------Test Count "+current+" End -------------\n");
		}
	}
	
	public void testQuery() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		try {
//			SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
			DBObject query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_ALL_WITH_STATUS: ");
			query(query);
			
//			SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";
			query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			writer.write("SQL_LIST_ALL_WITHOUT_STATUS: ");
			query(query);
//			
//			SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
			query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(query);
			
//			SQL_LIST_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			query = new BasicDBObject();
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(query).limit(Integer.MAX_VALUE);
			
//			SQL_LIST_BY_MID_LASTEST = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gte", Long.MIN_VALUE ));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_MID_LASTEST: ");
			query(query);
			
//			SQL_LIST_BY_MID_PRE = "SELECT * FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$lt", Long.MAX_VALUE));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_MID_PRE: ");
			query(query).limit(Integer.MAX_VALUE);
			
//			SQL_LIST_BY_MID_FWD = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gte", Long.MIN_VALUE));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_MID_FWD: ");
			query(query).limit(Integer.MAX_VALUE);
			
			long minT = System.currentTimeMillis() - 7 * 24 * 60 * 3600000;
//			SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gte", minT));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_TIME_LASTEST: ");
			query(query);
			
//			SQL_LIST_BY_TIME_PRE = "SELECT * FROM `informations` WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$lt", System.currentTimeMillis()));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_TIME_PRE: ");
			query(query).limit(Integer.MAX_VALUE);
			
//			SQL_LIST_BY_TIME_FWD = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			query = new BasicDBObject();
			query.put("id", new BasicDBObject("$gte", minT));
			query.put("receiverId", RECEIVER_ID);
			query.put("processed", PROCESSED);
			writer.write("SQL_LIST_BY_TIME_FWD: ");
			query(query).limit(Integer.MAX_VALUE);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			writer.write("-----------Test List "+current+" End -------------\n");
		}
	}
	
	public void testInsert(long count) throws Exception {
		writer.write("-----------Test Insert "+current+" Begin -------------\n");
		
		long startTime = System.currentTimeMillis();
		long endTime = startTime;
		
		try {
			startTime = System.currentTimeMillis();
			for(int i=0; i<count; i++) {
				total_rows++;
				DBObject dbObj = new BasicDBObject();
				dbObj.put("id", total_rows);
				dbObj.put("appId", APP_ID);
				dbObj.put("senderId", SENDER_ID);
				dbObj.put("receiverId", RECEIVER_ID);
				dbObj.put("type", TYPE);
				dbObj.put("action", ACTION);
				dbObj.put("title", TITLE);
				dbObj.put("data", DATA);
				dbObj.put("uri", URI);
				dbObj.put("processed", PROCESSED);
				dbObj.put("process_method", PROCESSED_METHOD);
				dbObj.put("importance", IMPORTANCE);
				dbObj.put("title_html", TITLE_HTML);
				dbObj.put("body", BODY);
				dbObj.put("body_html", BODY_HTML);
				dbObj.put("objectId", total_rows);
				dbObj.put("guid", GUID);
				dbObj.put("read", READ);
				dbObj.put("last_modified", System.currentTimeMillis());
				
				collection.insert(dbObj);
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
			writer.write("-----------Test Insert "+current+" End -------------\n");
		}
	}
	
	private void update(DBObject q, DBObject o) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			collection.update(q, o);
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
	
	public void testUpdate() throws Exception {
		writer.write("-----------Test Update "+current+" Begin -------------\n");

		try {
			
//			SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET processed=TRUE,last_modified=NOW()";
			writer.write("SQL_UPDATE_PROCESSED_STATUS: ");
			DBObject q = new BasicDBObject();
			DBObject o = new BasicDBObject();
			o.put("processed", 1);
			o.put("last_modified", System.currentTimeMillis());
			update(q, o);
			
//			SQL_UPDATE_READ_STATUS = "UPDATE informations SET `read`=TRUE,last_modified=NOW()";
			o = new BasicDBObject();
			o.put("reqd", 1);
			o.put("last_modified", System.currentTimeMillis());
			writer.write("SQL_UPDATE_READ_STATUS: ");
			update(q, o);
			
//			SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW() WHERE guid=%s";
			q = new BasicDBObject();
			o = new BasicDBObject();
			o.put("senderId", SENDER_ID);
			o.put("receiverId", RECEIVER_ID);
			o.put("type", TYPE);
			o.put("action", ACTION);
			o.put("title", TITLE);
			o.put("data", DATA);
			o.put("uri", URI);
			o.put("processed", PROCESSED);
			o.put("process_method", PROCESSED_METHOD);
			o.put("importance", IMPORTANCE);
			o.put("title_html", TITLE_HTML);
			o.put("body", BODY);
			o.put("body_html", BODY_HTML);
			o.put("objectId", total_rows);
			o.put("guid", GUID);
			writer.write("SQL_UPDATE_BY_GUID: ");
			update(q, o);

//			// update by appId, type, receiverId, objectId
//			SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW(),`read`=FALSE " +
//					"WHERE appId=%s and type=%s and receiverId=%s";
			q = new BasicDBObject();
			q.put("appId", APP_ID);
			q.put("type", TYPE);
			q.put("receiverId", RECEIVER_ID);
			o = new BasicDBObject();
			o.put("senderId", SENDER_ID);
			o.put("action", ACTION);
			o.put("title", TITLE);
			o.put("data", DATA);
			o.put("uri", URI);
			o.put("processed", PROCESSED);
			o.put("process_method", PROCESSED_METHOD);
			o.put("importance", IMPORTANCE);
			o.put("title_html", TITLE_HTML);
			o.put("body", BODY);
			o.put("body_html", BODY_HTML);
			o.put("objectId", total_rows);
			o.put("guid", GUID);
			writer.write("SQL_UPDATE_BY_ATRO: ");
			update(q, o);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			writer.write("-----------Test Update "+current+" End -------------\n");
		}
	}
	
	public void destroy() {
		clearDatabase();
		
		if(null != mongo) {
			mongo.close();
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
