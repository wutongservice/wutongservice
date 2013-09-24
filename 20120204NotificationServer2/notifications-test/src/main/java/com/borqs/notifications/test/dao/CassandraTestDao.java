package com.borqs.notifications.test.dao;

import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CassandraTestDao implements IDataStoreTestDao {
	private static final String COUNT_WITH_STATUS = "SELECT count(*)  FROM informations WHERE receiverId=%s AND processed=%s";
	private static final String COUNT_WITHOUT_STATUS = "SELECT count(*)  FROM informations WHERE receiverId=%s";
	private static final String COUNT_WITH_GUID = "SELECT count(*)  FROM informations WHERE guid='%s'";

	private static final String COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id<%s AND receiverId=%s AND processed=%s";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id<%s AND receiverId=%s";
	
	private static final String COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id>=%s AND receiverId=%s";
	
	private static final String SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
	private static final String SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";

	private static final String SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
	private static final String SQL_LIST_BY_RECEIVER = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	
	private static final String SQL_LIST_BY_MID_LASTEST = "SELECT * FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
	private static final String SQL_LIST_BY_MID_PRE = "SELECT * FROM informations WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	private static final String SQL_LIST_BY_MID_FWD = "SELECT * FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
	
//	private static final String SQL_QUERY_BY_APPID_TYPE_RECEIVERID_OBJECTID = "SELECT * FROM informations WHERE appId=%s AND type=%s AND receiverId=%s AND object_id=%s ORDER BY ID DESC";
//	private static final String SQL_DELETE_BY_APPID_TYPE_RECEIVERID_OBJECTID = "DELETE FROM informations WHERE appId=%s AND type=%s AND receiverId=%s AND object_id=%s";
//
//	private static final String SQL_EXIST_BY_GUID = "SELECT id FROM informations WHERE guid=%s";
	
	private static final String SQL_INSERT = "INSERT INTO informations(" +
			"appId,senderId,receiverId,type,action,date,title,data,uri,processed,process_method,importance,title_html,body,body_html,object_id,last_modified,guid) " +
			"VALUES(%s,%s,%s,'%s','%s',NOW(),'%s','%s','%s',%s,%s,%s,'%s','%s','%s','%s',NOW(),'%s')";
	private static final String SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET 'processed'='1','last_modified'='";
	private static final String SQL_UPDATE_READ_STATUS = "UPDATE informations SET read=1,last_modified=";
	
	private static final String SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type='%s',action='%s'" +
			",title='%s',data='%s',uri='%s',processed=%s,process_method=%s,importance=%s,title_html='%s',body='%s',body_html='%s',last_modified=NOW() WHERE guid='%s'";
	
	// update by appId, type, receiverId, objectId
	private static final String SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action='%s'" +
			",title='%s',data='%s',uri='%s',processed=%s,process_method=%s,importance=%s,title_html='%s',body='%s',body_html='%s',last_modified=NOW(),read=FALSE " +
			"WHERE appId=%s and type='%s' and receiverId=%s";
	
	private static final String SQL_DELETE_BY_ID = "DELETE FROM informations WHERE id=%s";

	private static final String SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM informations WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String SQL_LIST_BY_TIME_PRE = "SELECT * FROM informations WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
	private static final String SQL_LIST_BY_TIME_FWD = "SELECT * FROM informations WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";

	private static final String SQL_UPDATE_READ_BY_TIME = "UPDATE informations SET read=1 WHERE id IN (SELECT id FROM ( %s ) temp) AND process_method=1";

	private static final String COUNT_FWD_WITH_STATUS_BY_TIME = "SELECT count(*)  FROM informations WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_TIME = "SELECT count(*)  FROM informations WHERE last_modified>=%s AND receiverId=%s ORDER BY last_modified DESC";

	private static final String COUNT_PRE_WITH_STATUS_BY_TIME = "SELECT count(*)  FROM informations WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_TIME = "SELECT count(*)  FROM informations WHERE last_modified<%s AND receiverId=%s ORDER BY last_modified DESC";
	
	private static final String CLEAR_DATABASE = "DELETE FROM informations";
	
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
	
	private static long total = 100000;
	private static long increase = 10000;
	private static long current = 0;

	private static final String COLUMN_FAMILY = "informations";
	private static final String KEYSPACE = COLUMN_FAMILY;
	private static final String HOST = "127.0.0.1";
	private static final int PORT = 9160;
	private static final ConsistencyLevel CONSISTENCY_LEVEL = ConsistencyLevel.ONE;
	private static final int LIMIT = 100;
	
	private long total_rows = 0;
	
	private TTransport tr;
	private Cassandra.Client client;

	private BufferedWriter writer;
	private String RECORD_FILE_NAME = "notifications_cassandra_test_"+total+".txt";
	
	public void init() throws Exception {
		tr = new TFramedTransport(new TSocket(HOST, PORT));
		TProtocol proto = new TBinaryProtocol(tr);
		client = new Cassandra.Client(proto);
		tr.open();
		client.set_keyspace(KEYSPACE);
		
		// open file
		writer = new BufferedWriter(new FileWriter(RECORD_FILE_NAME));
	}

	public void clearDatabase() {
		try {
			client.truncate(COLUMN_FAMILY);
		} catch (InvalidRequestException e) {
			e.printStackTrace();
		} catch (UnavailableException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CassandraTestDao dao = new CassandraTestDao();
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
	
	private void count(IndexClause index_clause) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);
			SlicePredicate predicate = new SlicePredicate();
			predicate.addToColumn_names(ByteBuffer.wrap("id".getBytes()));
			List<KeySlice> slices = client.get_indexed_slices(columnParent, index_clause , predicate, CONSISTENCY_LEVEL);
			int count = slices.size();
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
	
	private void queryBySQL(String sql) throws Exception {
//		System.out.println(sql);
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			CqlResult res = client.execute_cql_query(ByteBuffer.wrap(sql.getBytes()), Compression.NONE);
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
	
	public void testCountByCQL() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		
		Statement stmt = null;
		String sql = null;
		try {
			// count with status
			sql = String.format(COUNT_WITH_STATUS, RECEIVER_ID, 1);
			writer.write("COUNT_WITH_STATUS: ");
			queryBySQL(sql);
			
			// count without status
			sql = String.format(COUNT_WITHOUT_STATUS, RECEIVER_ID);
			writer.write("COUNT_WITHOUT_STATUS: ");
			queryBySQL(sql);
			
			// count with QUID
			sql = String.format(COUNT_WITH_GUID, GUID);
			writer.write("COUNT_WITH_GUID: ");
			queryBySQL(sql);
			
//			COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*)  FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s";
			sql = String.format(COUNT_PRE_WITH_STATUS_BY_POS, Long.MAX_VALUE, RECEIVER_ID, 1);
			writer.write("COUNT_PRE_WITH_STATUS_BY_POS: ");
			queryBySQL(sql);
			
//			COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM `informations` WHERE id<%s AND receiverId=%s";
			sql = String.format(COUNT_PRE_WITHOUT_STATUS_BY_POS, Long.MAX_VALUE, RECEIVER_ID);
			writer.write("COUNT_PRE_WITHOUT_STATUS_BY_POS: ");
			queryBySQL(sql);
			
//			COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*)  FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s";
			sql = String.format(COUNT_FWD_WITH_STATUS_BY_POS, Long.MIN_VALUE, RECEIVER_ID, 1);
			writer.write("COUNT_FWD_WITH_STATUS_BY_POS: ");
			queryBySQL(sql);
			
//			COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM `informations` WHERE id>=%s AND receiverId=%s";
			sql = String.format(COUNT_FWD_WITHOUT_STATUS_BY_POS, Long.MIN_VALUE, RECEIVER_ID);
			writer.write("COUNT_FWD_WITHOUT_STATUS_BY_POS: ");
			queryBySQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			writer.write("-----------Test Count "+current+" End -------------\n");
		}
	}
	
	public void testCount() throws Exception {
		testCountByCQL();
		
//		countByThrift();
	}

	private void countByThrift() throws IOException {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		
		try {
			// count with status
			IndexClause clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("COUNT_WITH_STATUS: ");
			count(clause);
			
			// count without status
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			writer.write("COUNT_WITHOUT_STATUS: ");
			count(clause);
			
			// count without GUID
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("guid".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(GUID.getBytes())));
			writer.write("COUNT_WITH_GUID: ");
			count(clause);
			
//		COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id<%s AND receiverId=%s AND processed=%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.LT, ByteBuffer.allocate(8).putLong(Integer.MAX_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("COUNT_PRE_WITH_STATUS_BY_POS: ");
			count(clause);
			
//		COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id<%s AND receiverId=%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.LT, ByteBuffer.allocate(8).putLong(Integer.MAX_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			writer.write("COUNT_PRE_WITHOUT_STATUS_BY_POS: ");
			count(clause);
			
//		COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GT, ByteBuffer.allocate(8).putLong(Integer.MIN_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("COUNT_FWD_WITH_STATUS_BY_POS: ");
			count(clause);
			
//		COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*)  FROM informations WHERE id>=%s AND receiverId=%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GT, ByteBuffer.allocate(8).putLong(Integer.MIN_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			writer.write("COUNT_FWD_WITHOUT_STATUS_BY_POS: ");
			count(clause);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.write("-----------Test Count "+current+" End -------------\n");
		}
	}
	
	private void query(IndexClause index_clause) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);
			SlicePredicate predicate = new SlicePredicate();
			List<ByteBuffer> column_names = new ArrayList<ByteBuffer>();
			column_names.add(ByteBuffer.wrap("id".getBytes()));
			column_names.add(ByteBuffer.wrap("appId".getBytes()));
			column_names.add(ByteBuffer.wrap("senderId".getBytes()));
			column_names.add(ByteBuffer.wrap("receiverId".getBytes()));
			column_names.add(ByteBuffer.wrap("type".getBytes()));
			column_names.add(ByteBuffer.wrap("action".getBytes()));
			column_names.add(ByteBuffer.wrap("title".getBytes()));
			column_names.add(ByteBuffer.wrap("data".getBytes()));
			column_names.add(ByteBuffer.wrap("uri".getBytes()));
			column_names.add(ByteBuffer.wrap("processed".getBytes()));
			column_names.add(ByteBuffer.wrap("process_method".getBytes()));
			column_names.add(ByteBuffer.wrap("importance".getBytes()));
			column_names.add(ByteBuffer.wrap("title_html".getBytes()));
			column_names.add(ByteBuffer.wrap("body".getBytes()));
			column_names.add(ByteBuffer.wrap("body_html".getBytes()));
			column_names.add(ByteBuffer.wrap("object_id".getBytes()));
			column_names.add(ByteBuffer.wrap("guid".getBytes()));
			column_names.add(ByteBuffer.wrap("read".getBytes()));
			column_names.add(ByteBuffer.wrap("last_modified".getBytes()));
			predicate.setColumn_names(column_names );
			List<KeySlice> slices = client.get_indexed_slices(columnParent, index_clause , predicate, CONSISTENCY_LEVEL);
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

	public void testQuery() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		try {
//			SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
			IndexClause clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_ALL_WITH_STATUS: ");
			query(clause);
			
//			SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			writer.write("SQL_LIST_ALL_WITHOUT_STATUS: ");
			query(clause);
//			
//			SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(clause);
			
//			SQL_LIST_BY_RECEIVER = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			clause = new IndexClause();
			clause.setCount(LIMIT);
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(clause);
			
//			SQL_LIST_BY_MID_LASTEST = "SELECT * FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GT, ByteBuffer.allocate(8).putLong(Integer.MIN_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_MID_LASTEST: ");
			query(clause);
			
//			SQL_LIST_BY_MID_PRE = "SELECT * FROM informations WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			clause = new IndexClause();
			clause.setCount(LIMIT);
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.LT, ByteBuffer.allocate(8).putLong(Integer.MAX_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_MID_PRE: ");
			query(clause);
			
//			SQL_LIST_BY_MID_FWD = "SELECT * FROM informations WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			clause = new IndexClause();
			clause.setCount(LIMIT);
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GTE, ByteBuffer.allocate(8).putLong(Integer.MIN_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_MID_FWD: ");
			query(clause);
			
			long minT = System.currentTimeMillis() - 7 * 24 * 60 * 3600000;
//			SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM informations WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GTE, ByteBuffer.allocate(8).putLong(minT)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_TIME_LASTEST: ");
			query(clause);
			
//			SQL_LIST_BY_TIME_PRE = "SELECT * FROM informations WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			clause = new IndexClause();
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.LT, ByteBuffer.allocate(8).putLong(System.currentTimeMillis())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_TIME_PRE: ");
			query(clause);
			
//			SQL_LIST_BY_TIME_FWD = "SELECT * FROM informations WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			clause = new IndexClause();
			clause.setCount(LIMIT);
			clause.setStart_key("0".getBytes());
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("id".getBytes()), 
					IndexOperator.GTE, ByteBuffer.allocate(8).putLong(Integer.MIN_VALUE)));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("receiverId".getBytes()), 
					IndexOperator.EQ, ByteBuffer.wrap(RECEIVER_ID.getBytes())));
			clause.addToExpressions(new IndexExpression(
					ByteBuffer.wrap("processed".getBytes()), 
					IndexOperator.EQ, ByteBuffer.allocate(8).putLong(PROCESSED)));
			writer.write("SQL_LIST_BY_TIME_FWD: ");
			query(clause);
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
				
				ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);

				String nid = String.valueOf(total_rows);
				long timestamp = System.currentTimeMillis();
				
				Column idCol = new Column(ByteBuffer.wrap("id".getBytes()));
				ByteBuffer allocate = ByteBuffer.allocate(8);
				allocate.putLong(total_rows);
				idCol.setValue(allocate.array());
				idCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, idCol, CONSISTENCY_LEVEL) ;
				
				Column appIdCol = new Column(ByteBuffer.wrap("appId".getBytes()));
				appIdCol.setValue(APP_ID.getBytes());
				appIdCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, appIdCol, CONSISTENCY_LEVEL) ;
				
				Column senderIdCol = new Column(ByteBuffer.wrap("senderId".getBytes()));
				senderIdCol.setValue(SENDER_ID.getBytes());
				senderIdCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, senderIdCol, CONSISTENCY_LEVEL);
				
				Column receverIdCol = new Column(ByteBuffer.wrap("receiverId".getBytes()));
				receverIdCol.setValue(RECEIVER_ID.getBytes());
				receverIdCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, receverIdCol, CONSISTENCY_LEVEL);
				
				Column typeCold = new Column(ByteBuffer.wrap("type".getBytes()));
				typeCold.setValue(TYPE.getBytes());
				typeCold.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, typeCold, CONSISTENCY_LEVEL);
				
				Column actionCol = new Column(ByteBuffer.wrap("action".getBytes()));
				actionCol.setValue(ACTION.getBytes());
				actionCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, actionCol, CONSISTENCY_LEVEL);
				
				Column dateCol = new Column(ByteBuffer.wrap("date".getBytes()));
				allocate = ByteBuffer.allocate(8);
				allocate.putLong(timestamp);
				dateCol.setValue(allocate.array());
				dateCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, dateCol, CONSISTENCY_LEVEL);
				
				Column titleCol = new Column(ByteBuffer.wrap("title".getBytes()));
				titleCol.setValue(TITLE.getBytes());
				titleCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, titleCol, CONSISTENCY_LEVEL);
				
				Column dataCol = new Column(ByteBuffer.wrap("data".getBytes()));
				dataCol.setValue(DATA.getBytes());
				dataCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, dataCol, CONSISTENCY_LEVEL);
				
				Column uriCol = new Column(ByteBuffer.wrap("uri".getBytes()));
				uriCol.setValue(URI.getBytes());
				uriCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, uriCol, CONSISTENCY_LEVEL);
				
				Column processedCol = new Column(ByteBuffer.wrap("processed".getBytes()));
				allocate.clear();
				allocate.putLong(PROCESSED);
				processedCol.setValue(allocate.array());
				processedCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, processedCol, CONSISTENCY_LEVEL);
				
				Column readCol = new Column(ByteBuffer.wrap("read".getBytes()));
				allocate.clear();
				allocate.putLong(READ);
				readCol.setValue(allocate.array());
				readCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, readCol, CONSISTENCY_LEVEL);
				
				Column processMethodCol = new Column(ByteBuffer.wrap("process_method".getBytes()));
				allocate.clear();
				allocate.putLong(PROCESSED_METHOD);
				processMethodCol.setValue(allocate.array());
				processMethodCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, processMethodCol, CONSISTENCY_LEVEL);
				
				Column importanceCol = new Column(ByteBuffer.wrap("importance".getBytes()));
				allocate.clear();
				allocate.putLong(IMPORTANCE);
				importanceCol.setValue(allocate.array());
				importanceCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, importanceCol, CONSISTENCY_LEVEL);
				
				Column bodyCol = new Column(ByteBuffer.wrap("body".getBytes()));
				bodyCol.setValue(BODY.getBytes());
				bodyCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, bodyCol, CONSISTENCY_LEVEL);
				
				Column bodyHtmlCol = new Column(ByteBuffer.wrap("body_html".getBytes()));
				bodyHtmlCol.setValue(BODY_HTML.getBytes());
				bodyHtmlCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, bodyHtmlCol, CONSISTENCY_LEVEL);
				
				Column titleHtmlCol = new Column(ByteBuffer.wrap("title_html".getBytes()));
				titleHtmlCol.setValue(TITLE_HTML.getBytes());
				titleHtmlCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, titleHtmlCol, CONSISTENCY_LEVEL);
				
				Column guidCol = new Column(ByteBuffer.wrap("guid".getBytes()));
				guidCol.setValue(GUID.getBytes());
				guidCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, guidCol, CONSISTENCY_LEVEL);
				
				Column objectIdCol = new Column(ByteBuffer.wrap("object_id".getBytes()));
				allocate.clear();
				allocate.putLong(total_rows);
				objectIdCol.setValue(allocate.array());
				objectIdCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, objectIdCol, CONSISTENCY_LEVEL);
				
				Column lastModifiedCol = new Column(ByteBuffer.wrap("last_modified".getBytes()));
				allocate.clear();
				allocate.putLong(timestamp);
				lastModifiedCol.setValue(allocate.array());
				lastModifiedCol.setTimestamp(timestamp);
				client.insert(ByteBuffer.wrap(nid.getBytes()), columnParent, lastModifiedCol, CONSISTENCY_LEVEL);
				
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
	
	private void update(String sql) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			System.out.println(sql);
			CqlResult res = client.execute_cql_query(ByteBuffer.wrap(sql.getBytes()), Compression.NONE );
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

		String sql = null;
		try {
////			SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET 'processed'=1,'last_modified'="+now;;
//			writer.write("SQL_UPDATE_PROCESSED_STATUS: ");
//			update( SQL_UPDATE_PROCESSED_STATUS+System.currentTimeMillis()+"'");
//			
////			SQL_UPDATE_READ_STATUS = "UPDATE informations SET read=1,last_modified="+now;;
//			writer.write("SQL_UPDATE_READ_STATUS: ");
//			update(SQL_UPDATE_READ_STATUS+System.currentTimeMillis());
//			
////			SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type=%s,action=%s" +
////					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW() WHERE guid=%s";
//			sql = String.format(SQL_UPDATE_BY_GUID,
//					APP_ID, SENDER_ID, RECEIVER_ID, TYPE, ACTION,
//					TITLE, DATA, URI, PROCESSED, PROCESSED_METHOD, IMPORTANCE,
//					TITLE_HTML, BODY, BODY_HTML, GUID);
//			writer.write("SQL_UPDATE_BY_GUID: ");
//			update(sql);
//
////			// update by appId, type, receiverId, objectId
////			SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action=%s" +
////					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=%s,read=0 " +
////					"WHERE appId=%s and type=%s and receiverId=%s";
//			sql = String.format(SQL_UPDATE_BY_ATRO,
//					SENDER_ID, ACTION, TITLE, DATA, URI, 
//					PROCESSED, PROCESSED_METHOD, IMPORTANCE,
//					TITLE_HTML, BODY, BODY_HTML, System.currentTimeMillis(), APP_ID, TYPE, RECEIVER_ID);
//			writer.write("SQL_UPDATE_BY_ATRO: ");
//			update(sql);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			writer.write("-----------Test Update "+current+" End -------------\n");
		}
	}

	public void destroy() {
//		clearDatabase();
		
		if(null!=tr) {
			tr.close();
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
