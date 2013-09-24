package com.borqs.notifications.test.dao;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

public class HBaseTestDao implements IDataStoreTestDao {
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

	private static final String HOST = "127.0.0.1";
	private static final int PORT = 2181;
	private static final String TABLE_NAME = "informations";

	private Configuration conf;
	private HTable table;
	
	private BufferedWriter writer;
	
	private String RECORD_FILE_NAME = "notifications_hbase_test_"+total+".txt";
	private int total_rows;
	
	public void init() throws Exception {
		
		try {
			conf = HBaseConfiguration.create();
//			conf.set("hbase.zookeeper.quorum", HOST);
//			conf.set("hbase.zookeeper.property.clientPort", String.valueOf(PORT));
			table = new HTable(conf, TABLE_NAME);
			
			// open file
			writer = new BufferedWriter(new FileWriter(RECORD_FILE_NAME));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	public void clearDatabase() {
		try {
//			Delete delete = new Delete("");
//			delete.deleteFamily("id".getBytes());
//			delete.deleteFamily("appId".getBytes());
//			delete.deleteFamily("senderId".getBytes());
//			delete.deleteFamily("receiverId".getBytes());
//			delete.deleteFamily("type".getBytes());
//			delete.deleteFamily("action".getBytes());
//			delete.deleteFamily("date".getBytes());
//			delete.deleteFamily("title".getBytes());
//			delete.deleteFamily("data".getBytes());
//			delete.deleteFamily("uri".getBytes());
//			delete.deleteFamily("processed".getBytes());
//			delete.deleteFamily("read".getBytes());
//			delete.deleteFamily("process_method".getBytes());
//			delete.deleteFamily("importance".getBytes());
//			delete.deleteFamily("body".getBytes());
//			delete.deleteFamily("body_html".getBytes());
//			delete.deleteFamily("title_html".getBytes());
//			delete.deleteFamily("guid".getBytes());
//			delete.deleteFamily("object_id".getBytes());
//			delete.deleteFamily("last_modified".getBytes());
			
			for(long i=1; i<=total_rows; i++) {
				Delete delete = new Delete(String.valueOf(i).getBytes());
				table.delete(delete);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IDataStoreTestDao dao = new HBaseTestDao();
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
//				dao.endPhase();
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
	
	private void count(Scan scan) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			ResultScanner res = table.getScanner(scan);
			Iterator<Result> iterator = res.iterator();
			int count = 0;
			while(iterator.hasNext()) {
				count++;
				iterator.next();
			}
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

	public void testCount() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		
		try {
			// count with status
			Scan scan = new Scan();
			FilterList filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_WITH_STATUS: ");
			count(scan);
			
			// count without status
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_WITHOUT_STATUS: ");
			count(scan);
			
			// count without GUID
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("guid".getBytes(), "".getBytes(), CompareOp.EQUAL, GUID.getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_WITH_GUID: ");
			count(scan);
			
//		COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.LESS, String.valueOf(Long.MAX_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_PRE_WITH_STATUS_BY_POS: ");
			count(scan);
			
//		COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<%s AND receiverId=%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.LESS, String.valueOf(Long.MAX_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_PRE_WITHOUT_STATUS_BY_POS: ");
			count(scan);
			
//		COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER_OR_EQUAL, String.valueOf(Long.MIN_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_FWD_WITH_STATUS_BY_POS: ");
			count(scan);
			
//		COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=%s AND receiverId=%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER, String.valueOf(Long.MIN_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			scan.setFilter(filterList);
			writer.write("COUNT_FWD_WITHOUT_STATUS_BY_POS: ");
			count(scan);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.write("-----------Test Count "+current+" End -------------\n");
		}
	}
	
	private ResultScanner query(Scan scan, boolean noprint) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			ResultScanner res = table.getScanner(scan);
			endTime = System.currentTimeMillis();
			if(!noprint) {
				StringBuffer sb = new StringBuffer();
				sb.append(startTime).append("\t").append(endTime)
						.append("\t").append(endTime - startTime)
						.append("\n");
				writer.write(sb.toString());
			}
			
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void testQuery() throws Exception {
		writer.write("-----------Test Count "+current+" Begin -------------\n");
		try {
//			SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=%s AND processed=%s";
			Scan scan = new Scan();
			FilterList filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_ALL_WITH_STATUS: ");
			query(scan, false);
			
//			SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_ALL_WITHOUT_STATUS: ");
			query(scan, false);
//			
//			SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(scan, false);
			
//			SQL_LIST_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_ALL_BY_RECEIVER: ");
			query(scan, false).next(total_rows);
			
//			SQL_LIST_BY_MID_LASTEST = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER_OR_EQUAL, String.valueOf(Long.MIN_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_MID_LASTEST: ");
			query(scan, false);
			
//			SQL_LIST_BY_MID_PRE = "SELECT * FROM `informations` WHERE id<%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.LESS, String.valueOf(Long.MAX_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_MID_PRE: ");
			query(scan, false).next(total_rows);
			
//			SQL_LIST_BY_MID_FWD = "SELECT * FROM `informations` WHERE id>=%s AND receiverId=%s AND processed=%s ORDER BY ID DESC LIMIT %s,%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER_OR_EQUAL, String.valueOf(Long.MIN_VALUE).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_MID_FWD: ");
			query(scan, false).next(total_rows);
			
			long minT = System.currentTimeMillis() - 7 * 24 * 60 * 3600000;
//			SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER_OR_EQUAL, String.valueOf(minT).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_TIME_LASTEST: ");
			query(scan, false);
			
//			SQL_LIST_BY_TIME_PRE = "SELECT * FROM `informations` WHERE last_modified<%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.LESS, String.valueOf(System.currentTimeMillis()).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_TIME_PRE: ");
			query(scan, false).next(total_rows);
			
//			SQL_LIST_BY_TIME_FWD = "SELECT * FROM `informations` WHERE last_modified>=%s AND receiverId=%s AND processed=%s ORDER BY last_modified DESC LIMIT %s,%s";
			filterList = new FilterList();
			filterList.addFilter(new SingleColumnValueFilter("id".getBytes(), "".getBytes(), CompareOp.GREATER_OR_EQUAL, String.valueOf(minT).getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			filterList.addFilter(new SingleColumnValueFilter("processed".getBytes(), "".getBytes(), CompareOp.EQUAL, String.valueOf(PROCESSED).getBytes()));
			scan.setFilter(filterList);
			writer.write("SQL_LIST_BY_TIME_FWD: ");
			query(scan, false).next(total_rows);
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
				
				byte[] row = String.valueOf(total_rows).getBytes();
				byte[] qualifier = "".getBytes();
				
				Put put = new Put(row);
				put.add("id".getBytes(), qualifier, row);
				put.add("appId".getBytes(), qualifier, APP_ID.getBytes());
				put.add("senderId".getBytes(), qualifier, SENDER_ID.getBytes());
				put.add("receiverId".getBytes(), qualifier, RECEIVER_ID.getBytes());
				put.add("type".getBytes(), qualifier, TYPE.getBytes());
				put.add("action".getBytes(), qualifier, ACTION.getBytes());
				put.add("date".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
				put.add("title".getBytes(), qualifier, TITLE.getBytes());
				put.add("data".getBytes(), qualifier, DATA.getBytes());
				put.add("uri".getBytes(), qualifier, URI.getBytes());
				put.add("processed".getBytes(), qualifier, String.valueOf(PROCESSED).getBytes());
				put.add("read".getBytes(), qualifier, String.valueOf(READ).getBytes());
				put.add("process_method".getBytes(), qualifier, String.valueOf(PROCESSED_METHOD).getBytes());
				put.add("importance".getBytes(), qualifier, String.valueOf(IMPORTANCE).getBytes());
				put.add("body".getBytes(), qualifier, BODY.getBytes());
				put.add("body_html".getBytes(), qualifier, BODY_HTML.getBytes());
				put.add("title_html".getBytes(), qualifier, TITLE_HTML.getBytes());
				put.add("guid".getBytes(), qualifier, GUID.getBytes());
				put.add("object_id".getBytes(), qualifier, String.valueOf(total_rows).getBytes());
				put.add("last_modified".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
				
				table.put(put);
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
	
//	private void update(long rownum) throws Exception {
//		try {
//			long startTime = System.currentTimeMillis();
//			long endTime = startTime;
//			startTime = System.currentTimeMillis();
//			byte[] row = String.valueOf(rownum).getBytes();
//			byte[] qualifier = "".getBytes();
//			
//			Put put = new Put(row);
//			put.add("id".getBytes(), qualifier, row);
//			put.add("appId".getBytes(), qualifier, APP_ID.getBytes());
//			put.add("senderId".getBytes(), qualifier, SENDER_ID.getBytes());
//			put.add("receiverId".getBytes(), qualifier, RECEIVER_ID.getBytes());
//			put.add("type".getBytes(), qualifier, TYPE.getBytes());
//			put.add("action".getBytes(), qualifier, ACTION.getBytes());
//			put.add("date".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
//			put.add("title".getBytes(), qualifier, TITLE.getBytes());
//			put.add("data".getBytes(), qualifier, DATA.getBytes());
//			put.add("uri".getBytes(), qualifier, URI.getBytes());
//			put.add("processed".getBytes(), qualifier, String.valueOf(PROCESSED).getBytes());
//			put.add("read".getBytes(), qualifier, String.valueOf(READ).getBytes());
//			put.add("process_method".getBytes(), qualifier, String.valueOf(PROCESSED_METHOD).getBytes());
//			put.add("importance".getBytes(), qualifier, String.valueOf(IMPORTANCE).getBytes());
//			put.add("body".getBytes(), qualifier, BODY.getBytes());
//			put.add("body_html".getBytes(), qualifier, BODY_HTML.getBytes());
//			put.add("title_html".getBytes(), qualifier, TITLE_HTML.getBytes());
//			put.add("guid".getBytes(), qualifier, GUID.getBytes());
//			put.add("object_id".getBytes(), qualifier, String.valueOf(total_rows).getBytes());
//			put.add("last_modified".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
//			
//			table.put(put);
//			
//			endTime = System.currentTimeMillis();
//			StringBuffer sb = new StringBuffer();
//			sb.append(startTime).append("\t").append(endTime)
//					.append("\t").append(endTime - startTime)
//					.append("\n");
//			writer.write(sb.toString());
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw e;
//		}
//	}

	public void testUpdate() throws Exception {
		writer.write("-----------Test Update "+current+" Begin -------------\n");
		Scan scan;
		FilterList filters = null;
		ResultScanner resultScan;
		Iterator<Result> iter;
		byte[] qualifier = "".getBytes();
		StringBuffer sb;
		try {
			long startTime = System.currentTimeMillis();
			long endTime = startTime;
			startTime = System.currentTimeMillis();
			
//			SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET processed=TRUE,last_modified=NOW()";
			writer.write("SQL_UPDATE_PROCESSED_STATUS: ");
			scan = new Scan();
			resultScan = query(scan, true);
			iter = resultScan.iterator();
			while(iter.hasNext()) {
				Put put = new Put(iter.next().getRow());
				put.add("processed".getBytes(), qualifier, String.valueOf(PROCESSED).getBytes());
				put.add("last_modified".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
				table.put(put);
			}
			endTime = System.currentTimeMillis();
			sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
			
//			SQL_UPDATE_READ_STATUS = "UPDATE informations SET `read`=TRUE,last_modified=NOW()";
			writer.write("SQL_UPDATE_READ_STATUS: ");
			scan = new Scan();
			resultScan = query(scan, true);
			iter = resultScan.iterator();
			while(iter.hasNext()) {
				Put put = new Put(iter.next().getRow());
				put.add("read".getBytes(), qualifier, String.valueOf(READ).getBytes());
				put.add("last_modified".getBytes(), qualifier, String.valueOf(System.currentTimeMillis()).getBytes());
				table.put(put);
			}
			endTime = System.currentTimeMillis();
			sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
			
//			SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=%s,senderId=%s,receiverId=%s,type=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW() WHERE guid=%s";
			writer.write("SQL_UPDATE_BY_GUID: ");
			scan = new Scan();
			resultScan = query(scan, true);
			iter = resultScan.iterator();
			while(iter.hasNext()) {
				byte[] row = iter.next().getRow();
				Put put = new Put(row);
				put.add("senderId".getBytes(), qualifier, SENDER_ID.getBytes());
				put.add("receiverId".getBytes(), qualifier, RECEIVER_ID.getBytes());
				put.add("type".getBytes(), qualifier, TYPE.getBytes());
				put.add("action".getBytes(), qualifier, ACTION.getBytes());
				put.add("title".getBytes(), qualifier, TITLE.getBytes());
				put.add("data".getBytes(), qualifier, DATA.getBytes());
				put.add("uri".getBytes(), qualifier, URI.getBytes());
				put.add("processed".getBytes(), qualifier, String.valueOf(PROCESSED).getBytes());
				put.add("process_method".getBytes(), qualifier, String.valueOf(PROCESSED_METHOD).getBytes());
				put.add("importance".getBytes(), qualifier, String.valueOf(IMPORTANCE).getBytes());
				put.add("title_html".getBytes(), qualifier, TITLE_HTML.getBytes());
				put.add("body".getBytes(), qualifier, BODY.getBytes());
				put.add("body_html".getBytes(), qualifier, BODY_HTML.getBytes());
				put.add("object_id".getBytes(), qualifier, row);
				put.add("guid".getBytes(), qualifier, GUID.getBytes());
				table.put(put);
			}
			endTime = System.currentTimeMillis();
			sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());

//			// update by appId, type, receiverId, objectId
//			SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=%s,action=%s" +
//					",title=%s,data=%s,uri=%s,processed=%s,process_method=%s,importance=%s,title_html=%s,body=%s,body_html=%s,last_modified=NOW(),`read`=FALSE " +
//					"WHERE appId=%s and type=%s and receiverId=%s";
			writer.write("SQL_UPDATE_BY_ATRO: ");
			scan = new Scan();
			filters = new FilterList();
			filters.addFilter(new SingleColumnValueFilter("appId".getBytes(), "".getBytes(), CompareOp.EQUAL, APP_ID.getBytes()));
			filters.addFilter(new SingleColumnValueFilter("type".getBytes(), "".getBytes(), CompareOp.EQUAL, TYPE.getBytes()));
			filters.addFilter(new SingleColumnValueFilter("receiverId".getBytes(), "".getBytes(), CompareOp.EQUAL, RECEIVER_ID.getBytes()));
			scan.setFilter(filters);
			resultScan = query(scan, true);
			iter = resultScan.iterator();
			while(iter.hasNext()) {
				byte[] row = iter.next().getRow();
				Put put = new Put(row);
				put.add("senderId".getBytes(), qualifier, SENDER_ID.getBytes());
				put.add("action".getBytes(), qualifier, ACTION.getBytes());
				put.add("title".getBytes(), qualifier, TITLE.getBytes());
				put.add("data".getBytes(), qualifier, DATA.getBytes());
				put.add("uri".getBytes(), qualifier, URI.getBytes());
				put.add("processed".getBytes(), qualifier, String.valueOf(PROCESSED).getBytes());
				put.add("process_method".getBytes(), qualifier, String.valueOf(PROCESSED_METHOD).getBytes());
				put.add("importance".getBytes(), qualifier, String.valueOf(IMPORTANCE).getBytes());
				put.add("title_html".getBytes(), qualifier, TITLE_HTML.getBytes());
				put.add("body".getBytes(), qualifier, BODY.getBytes());
				put.add("body_html".getBytes(), qualifier, BODY_HTML.getBytes());
				put.add("object_id".getBytes(), qualifier, row);
				put.add("guid".getBytes(), qualifier, GUID.getBytes());
				table.put(put);
			}
			endTime = System.currentTimeMillis();
			sb = new StringBuffer();
			sb.append(startTime).append("\t").append(endTime)
					.append("\t").append(endTime - startTime)
					.append("\n");
			writer.write(sb.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			writer.write("-----------Test Update "+current+" End -------------\n");
		}
	}
	
	public void destroy() {
		clearDatabase();
		
		if(null != table) {
			try {
				table.close();
			} catch (IOException e) {
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
