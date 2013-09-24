package com.borqs.information.dao;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.util.Date;
import java.util.List;

public class InformationsStorageManager implements IInformationsStorageManager {
	private static Logger logger = LoggerFactory.getLogger(InformationsStorageManager.class);
	
	// count informations
	private static final String COUNT_WITH_STATUS = "SELECT count(*) c FROM `informations` WHERE receiverId=? AND processed=?";
	private static final String COUNT_WITHOUT_STATUS = "SELECT count(*) c FROM `informations` WHERE receiverId=?";
	
	private static final String COUNT_BY_APPID_WITH_STATUS = "SELECT count(*) c FROM `informations` WHERE appId=? AND receiverId=? AND processed=?";
	private static final String COUNT_BY_APPID_WITHOUT_STATUS = "SELECT count(*) c FROM `informations` WHERE appId=? AND receiverId=?";
	
	private static final String COUNT_WITH_GUID = "SELECT count(*) c FROM `informations` WHERE guid=?";

	// count by position
	private static final String COUNT_PRE_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<? AND receiverId=? AND processed=?";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id<? AND receiverId=?";
	private static final String COUNT_FWD_WITH_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=? AND receiverId=? AND processed=?";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_POS = "SELECT count(*) c FROM `informations` WHERE id>=? AND receiverId=?";
	
	private static final String COUNT_PRE_WITH_STATUS_BY_APPID_POS = "SELECT count(*) c FROM `informations` WHERE id<? AND receiverId=? AND processed=? AND appId=?";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_APPID_POS = "SELECT count(*) c FROM `informations` WHERE id<? AND receiverId=? AND appId=?";
	private static final String COUNT_FWD_WITH_STATUS_BY_APPID_POS = "SELECT count(*) c FROM `informations` WHERE id>=? AND receiverId=? AND processed=? AND appId=?";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_APPID_POS = "SELECT count(*) c FROM `informations` WHERE id>=? AND receiverId=? AND appId=?";

	// count by time
	private static final String COUNT_FWD_WITH_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? ORDER BY last_modified DESC";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=? AND receiverId=? ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITH_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<? AND receiverId=? AND processed=? ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<? AND receiverId=? ORDER BY last_modified DESC";
	
	private static final String COUNT_FWD_WITH_STATUS_BY_APPID_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? AND appId=? ORDER BY last_modified DESC";
	private static final String COUNT_FWD_WITHOUT_STATUS_BY_APPID_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified>=? AND receiverId=? AND appId=? ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITH_STATUS_BY_APPID_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<? AND receiverId=? AND processed=? AND appId=? ORDER BY last_modified DESC";
	private static final String COUNT_PRE_WITHOUT_STATUS_BY_APPID_TIME = "SELECT count(*) c FROM `informations` WHERE last_modified<? AND receiverId=? AND appId=? ORDER BY last_modified DESC";
	
	// list informations
	private static final String SQL_LIST_ALL_WITH_STATUS = "SELECT * FROM informations WHERE receiverId=? AND processed=?";
	private static final String SQL_LIST_ALL_WITHOUT_STATUS = "SELECT * FROM informations WHERE receiverId=?";
	private static final String SQL_LIST_ALL_WITH_STATUS_BY_APPID = "SELECT * FROM informations WHERE receiverId=? AND processed=? AND appId=?";
	private static final String SQL_LIST_ALL_WITHOUT_STATUS_BY_APPID = "SELECT * FROM informations WHERE receiverId=? AND appId=?";
	
	private static final String SQL_LIST_ALL_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=? AND processed=? ORDER BY ID DESC";
	private static final String SQL_LIST_BY_RECEIVER = "SELECT * FROM `informations` WHERE receiverId=? AND processed=? ORDER BY ID DESC LIMIT ?,?";
	private static final String SQL_LIST_ALL_BY_RECEIVER_APPID = "SELECT * FROM `informations` WHERE receiverId=? AND processed=? AND appId=? ORDER BY ID DESC";
	private static final String SQL_LIST_BY_RECEIVER_APPID = "SELECT * FROM `informations` WHERE receiverId=? AND processed=? AND appId=? ORDER BY ID DESC LIMIT ?,?";
	
	// list by ID
	private static final String SQL_LIST_BY_MID_LASTEST = "SELECT * FROM `informations` WHERE id>=? AND receiverId=? AND processed=? ORDER BY ID DESC";
	private static final String SQL_LIST_BY_MID_PRE = "SELECT * FROM `informations` WHERE id<? AND receiverId=? AND processed=? ORDER BY ID DESC LIMIT ?,?";
	private static final String SQL_LIST_BY_MID_FWD = "SELECT * FROM `informations` WHERE id>=? AND receiverId=? AND processed=? ORDER BY ID DESC LIMIT ?,?";
	
	private static final String SQL_LIST_BY_MID_LASTEST_APPID = "SELECT * FROM `informations` WHERE id>=? AND receiverId=? AND processed=? AND appId=? ORDER BY ID DESC";
	private static final String SQL_LIST_BY_MID_PRE_APPID = "SELECT * FROM `informations` WHERE id<? AND receiverId=? AND processed=? AND appId=? ORDER BY ID DESC LIMIT ?,?";
	private static final String SQL_LIST_BY_MID_FWD_APPID = "SELECT * FROM `informations` WHERE id>=? AND receiverId=? AND processed=? AND appId=? ORDER BY ID DESC LIMIT ?,?";

	// list by time
	private static final String SQL_LIST_BY_TIME_LASTEST = "SELECT * FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? ORDER BY last_modified DESC";
	private static final String SQL_LIST_BY_TIME_PRE = "SELECT * FROM `informations` WHERE last_modified<? AND receiverId=? AND processed=? ORDER BY last_modified DESC LIMIT ?,?";
	private static final String SQL_LIST_BY_TIME_FWD = "SELECT * FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? ORDER BY last_modified DESC LIMIT ?,?";
	
	private static final String SQL_LIST_BY_TIME_LASTEST_APPID = "SELECT * FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? AND appId=? ORDER BY last_modified DESC";
	private static final String SQL_LIST_BY_TIME_PRE_APPID = "SELECT * FROM `informations` WHERE last_modified<? AND receiverId=? AND processed=? AND appId=? ORDER BY last_modified DESC LIMIT ?,?";
	private static final String SQL_LIST_BY_TIME_FWD_APPID = "SELECT * FROM `informations` WHERE last_modified>=? AND receiverId=? AND processed=? AND appId=? ORDER BY last_modified DESC LIMIT ?,?";
	
	private static final String SQL_QUERY_BY_APPID_TYPE_RECEIVERID_OBJECTID = "SELECT * FROM `informations` WHERE appId=? AND type=? AND receiverId=? AND object_id=? ORDER BY ID DESC";

	private static final String SQL_EXIST_BY_GUID = "SELECT id FROM `informations` WHERE guid=?";
	
	// insert information
	private static final String SQL_INSERT = "INSERT INTO informations(" +
			"appId,senderId,receiverId,type,action,date,title,data,uri,processed,process_method,importance,title_html,body,body_html,object_id,last_modified,guid) " +
			"VALUES(?,?,?,?,?,NOW(),?,?,?,?,?,?,?,?,?,?,NOW(),?)";
	
	// update information
	private static final String SQL_UPDATE_PROCESSED_STATUS = "UPDATE informations SET processed=TRUE,last_modified=NOW() WHERE ID=?";
	private static final String SQL_UPDATE_READ_STATUS = "UPDATE informations SET `read`=TRUE WHERE ID=?";;
	
	private static final String SQL_UPDATE_BY_GUID = "UPDATE informations SET appId=?,senderId=?,receiverId=?,type=?,action=?" +
			",title=?,data=?,uri=?,processed=?,process_method=?,importance=?,title_html=?,body=?,body_html=?,object_id=?,last_modified=NOW() WHERE guid=?";
	
	// update by appId, type, receiverId, objectId
	private static final String SQL_UPDATE_BY_ATRO = "UPDATE informations SET senderId=?,action=?" +
			",title=?,data=?,uri=?,processed=?,process_method=?,importance=?,title_html=?,body=?,body_html=?,last_modified=NOW(),`read`=FALSE " +
			"WHERE appId=? and type=? and receiverId=? and object_id=?";

	private static final String SQL_UPDATE_READ_BY_TIME = "UPDATE `informations` SET `read`=TRUE, `processed`=TRUE WHERE id IN (SELECT id FROM ( %s ) temp) AND process_method=1";
	
	// delete
	private static final String SQL_DELETE_BY_ID = "DELETE FROM `informations` WHERE id=?";
	private static final String SQL_DELETE_BY_APPID_TYPE_RECEIVERID_OBJECTID = "DELETE FROM `informations` WHERE appId=? AND type=? AND receiverId=? AND object_id=?";
	
	private JdbcTemplate jdbcTemplate;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// delete a record in database by record ID
	/* (non-Javadoc)
	 * @see com.borqs.information.dao.IInformationsStorageManager#delete(java.lang.String)
	 */
	
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE_BY_ID, id);
	}
	
	// add a record to database when information ID is null
	// and update a record when information ID isn't null
	/* (non-Javadoc)
	 * @see com.borqs.information.dao.IInformationsStorageManager#save(com.borqs.information.rest.bean.Information)
	 */
	
	public String save(final Information info) {
//		jdbcTemplate.update(SQL_INSERT, new PreparedStatementSetter(){
//			public void setValues(PreparedStatement ps) throws SQLException {
//				putMsgIntoStatement(ps, info);
//			}
//		});
		

		if(info.getId()<=0 && null != info.getGuid()) {
			String guid = info.getGuid()+"-"+info.getReceiverId();
			Long id = queryInformationByGuid(guid);
			if(null != id) {
//				information.setId(id);
				delete(String.valueOf(id));
			}
		}
		
		KeyHolder keyHolder = new GeneratedKeyHolder();  
		jdbcTemplate.update(new PreparedStatementCreator() {
		    public PreparedStatement createPreparedStatement(Connection conn)  
		            throws SQLException {  
		        PreparedStatement ps = null;
				if(info.getId()>0) {
					ps = conn.prepareStatement(SQL_UPDATE_BY_GUID, Statement.RETURN_GENERATED_KEYS);
		        } else {
		        	ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
		        }
		        informationToStatementForInsert(ps, info);
		        return ps;  
		    }  
		}, keyHolder);
		
		if(null==keyHolder.getKey()) {
			return String.valueOf(info.getId());
		}
		
		return keyHolder.getKey().toString();
	}
	
	/**
	 * estimate whether the information exists
	 * @param guid
	 * @return
	 */
	public boolean isExist(String guid) {
		int res = jdbcTemplate.queryForInt(COUNT_WITH_GUID, guid);
		return (res==1);
	}
	
	/**
	 * estimate whether the information exists by GUID
	 * @param guid
	 * @return
	 */
	public Long queryInformationByGuid(String guid) {
		Long res = null;
		if(!isExist(guid)) {
			return res;
		}
		try {
			res = jdbcTemplate.queryForObject(SQL_EXIST_BY_GUID, new Object[]{guid}, new RowMapper<Long>(){
				
				public Long mapRow(ResultSet rs, int row) throws SQLException {
					return rs.getLong(Information.INFO_ID);
				}
			});
		} catch (DataAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return res;
	}
	
	private void informationToStatementForInsert(PreparedStatement ps, final Information info) throws SQLException {
//		appId,senderId,receiverId,type,action,date,title,data,uri,processed,process_method,importance,guid,title_html,body,body_html
		ps.setString(1, info.getAppId());
		ps.setString(2, info.getSenderId());
		ps.setString(3, info.getReceiverId());
		ps.setString(4, info.getType());
		ps.setString(5, info.getAction());
		ps.setString(6, info.getTitle());
		ps.setString(7, info.getData());
		ps.setString(8, info.getUri());
		ps.setBoolean(9, false);
		ps.setInt(10, info.getProcessMethod());
		ps.setInt(11, info.getImportance());
		ps.setString(12, info.getTitleHtml());
		ps.setString(13, info.getBody());
		ps.setString(14, info.getBodyHtml());
		ps.setString(15, info.getObjectId());
		
		String guid = info.getGuid();
		if(null != guid && !"".equals(guid)) {
			guid = guid + "-" +info.getReceiverId();
		}
		ps.setString(16, guid);
	}
	
	/* (non-Javadoc)
	 * @see com.borqs.information.dao.IInformationsStorageManager#list()
	 */
	
	public InformationList list(String userId) {
		return list(userId, null);
	}
	
	public InformationList list(String userId, String status) {
		return list(null, userId, status);
	}
	
	public InformationList list(String appId, String userId, String status) {
		InformationList informations = new InformationList();
		
		Object[] params = null;
		String sql = null;
		
		if(null==status) {
			if(null == appId) {
				params = new Object[]{userId};
				sql = SQL_LIST_ALL_WITHOUT_STATUS;
			} else {
				params = new Object[]{userId, appId};
				sql = SQL_LIST_ALL_WITHOUT_STATUS_BY_APPID;
			}
		} else {
			if(null == appId) {
				params = new Object[]{userId, status};
				sql = SQL_LIST_ALL_WITH_STATUS;
			} else {
				params = new Object[]{userId, status, appId};
				sql = SQL_LIST_ALL_WITH_STATUS_BY_APPID;
			}
		}
		
		List<Information> result = jdbcTemplate.query(sql, params, new RowMapper<Information>(){
			
			public Information mapRow(ResultSet rs, int rn)
					throws SQLException {
				return createInformation(rs);
			}
		});
		
		informations.setInformations(result);
		
		return informations;
	}
	
	public InformationList list(String userId, String status, Long from, Integer size) {
		return list(null, userId, status, from, size);
	}

	@Override
	public InformationList list(String appId, String userId, String status,
			Long from, Integer size) {
		InformationList informations = new InformationList();
		
		// default to read unprocessed items
		if(null==status || "".equals(status.trim())) {
			status = "0";
		}
		
		// default start item is from zero position
		if(null==from) {
			from = 0L;
		}
		
		// default to read twenty items
		if(null==size) {
			size = 20;
		}
		
		List<Information> result = null;
		String sql = null;
		Object[] params = null;
		if(size<=0) {
			if(null == appId) {
				sql = SQL_LIST_ALL_BY_RECEIVER;
				params = new Object[]{userId, status};
			} else {
				sql = SQL_LIST_ALL_BY_RECEIVER_APPID;
				params = new Object[]{userId, status, appId};
			}
			result = jdbcTemplate.query(sql, params, new RowMapper<Information>(){
				
				public Information mapRow(ResultSet rs, int rn)
						throws SQLException {
					return createInformation(rs);
				}
			});
		} else {
			if(null == appId) {
				sql = SQL_LIST_BY_RECEIVER;
				params = new Object[]{userId, status, from, size};
			} else {
				sql = SQL_LIST_BY_RECEIVER_APPID;
				params = new Object[]{userId, status, appId, from, size};
			}
			result = jdbcTemplate.query(SQL_LIST_BY_RECEIVER, params, new RowMapper<Information>(){
				
				public Information mapRow(ResultSet rs, int rn)
						throws SQLException {
					return createInformation(rs);
				}
			});
		}
		
		informations.setInformations(result);
		
		return informations;
	}
	
	public InformationList listById(String userId, String status, Long mid, Integer count) {
		return listById(null, userId, status, mid, count);
	}

	@Override
	public InformationList listById(String appId, String userId, String status,
			Long mid, Integer count) {
		InformationList informations = new InformationList();
		
		// default to read unprocessed items
		if(null==status || "".equals(status.trim())) {
			status = "0";
		}
		
		// default start item is from zero position
		if(null==mid) {
			mid = 0L;
			return list(appId, userId, status);
			
		}
		
		int total = countByPosition(mid, appId, userId, status, count);
		if(total<=0) {
			informations.setTotal(total);
			return informations;
		}
		
		Object[] params = null;
		String sql = null;
		if(null==count || 0==count) {
			if(null == appId) {
				params = new Object[]{mid, userId, status};
				sql = SQL_LIST_BY_MID_LASTEST;
			} else {
				params = new Object[]{mid, userId, status, appId};
				sql = SQL_LIST_BY_MID_LASTEST_APPID;
			}
		} else if(count<0) {
			if(null == appId) {
				params = new Object[]{mid, userId, status, 0, Math.abs(count)};
				sql = SQL_LIST_BY_MID_PRE;
			} else {
				params = new Object[]{mid, userId, status, appId, 0, Math.abs(count)};
				sql = SQL_LIST_BY_MID_PRE_APPID;
			}
		} else {
			int start = total-count;
			if(start<0) {
				start = 0;
			}
			
			if(null == appId) {
				params = new Object[]{mid, userId, status, start, count};
				sql = SQL_LIST_BY_MID_FWD;
			} else {
				params = new Object[]{mid, userId, status, appId, start, count};
				sql = SQL_LIST_BY_MID_FWD_APPID;
			}
		}
		
		List<Information> result = jdbcTemplate.query(sql, params, new RowMapper<Information>(){
			
			public Information mapRow(ResultSet rs, int rn)
					throws SQLException {
				return createInformation(rs);
			}
		});
		
		informations.setInformations(result);
		informations.setTotal(total);
		
		return informations;
	}
	
	public InformationList listByTime(String userId, String status, Long time,
			Integer count) {
		return listByTime(null, userId, status, time, count);
	}

	@Override
	public InformationList listByTime(String appId, String userId,
			String status, Long time, Integer count) {
		InformationList informations = new InformationList();
		
		// default to read unprocessed items
		if(null==status || "".equals(status.trim())) {
			status = "0";
		}
		
		// default start item is from zero position
		if(null==time) {
			return list(appId, userId, status);
			
		}
		
		Date from = new Date(time);
		
		int total = countByTime(from, appId, userId, status, count);
		if(total<=0) {
			informations.setTotal(total);
			return informations;
		}
		
		Object[] params = null;
		String sql = null;
		if(null==count || 0==count) {
			if(null == appId) {
				params = new Object[]{from, userId, status};
				sql = SQL_LIST_BY_TIME_LASTEST;
			} else {
				params = new Object[]{from, userId, status, appId};
				sql = SQL_LIST_BY_TIME_LASTEST_APPID;
			}
		} else if(count<0) {
			if(null == appId) {
				params = new Object[]{from, userId, status, 0, Math.abs(count)};
				sql = SQL_LIST_BY_TIME_PRE;
			} else {
				params = new Object[]{from, userId, status, appId, 0, Math.abs(count)};
				sql = SQL_LIST_BY_TIME_PRE_APPID;
			}
		} else {
			int start = total-count;
			if(start<0) {
				start = 0;
			}
			if(null == appId) {
				params = new Object[]{from, userId, status, start, count};
				sql = SQL_LIST_BY_TIME_FWD;
			} else {
				params = new Object[]{from, userId, status, appId, start, count};
				sql = SQL_LIST_BY_TIME_FWD_APPID;
			}
		}
		
		List<Information> result = jdbcTemplate.query(sql, params, new RowMapper<Information>(){
			
			public Information mapRow(ResultSet rs, int rn)
					throws SQLException {
				return createInformation(rs);
			}
		});
		
//		if(result.size() > 0) {
//			String sql_update_read = String.format(SQL_UPDATE_READ_BY_TIME, sql);
//			jdbcTemplate.update(sql_update_read, params);
//		}
				
		informations.setInformations(result);
		informations.setTotal(total);
		
		return informations;
	}
	
	private int countByTime(Date from, String userId, String status, Integer dir) {
		int res = 0;
		
		if(null==from) {
			res = count(userId, status);
		} else {
			if(null==dir||dir>=0) { // forward
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITHOUT_STATUS_BY_TIME, from, userId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITH_STATUS_BY_TIME, from, userId, status);
				}
			} else {
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITHOUT_STATUS_BY_TIME, from, userId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITH_STATUS_BY_TIME, from, userId, status);
				}
			}
		}
		return res;
	}
	
	private int countByTime(Date from, String appId, String userId, String status, Integer dir) {
		if(null == appId) {
			return countByTime(from, userId, status, dir);
		}
		
		int res = 0;
		
		if(null==from) {
			res = count(appId, userId, status);
		} else {
			if(null==dir||dir>=0) { // forward
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITHOUT_STATUS_BY_APPID_TIME, from, userId, appId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITH_STATUS_BY_APPID_TIME, from, userId, status, appId);
				}
			} else {
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITHOUT_STATUS_BY_APPID_TIME, from, userId, appId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITH_STATUS_BY_APPID_TIME, from, userId, status, appId);
				}
			}
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see com.borqs.information.dao.IInformationsStorageManager#top(int)
	 */
	
	public InformationList top(String userId, String status, Integer topNum) {
		return top(null, userId, status, topNum);
	}

	@Override
	public InformationList top(String appId, String userId, String status,
			Integer topNum) {
		InformationList informations = new InformationList();
		
		// default to read unprocessed items
		if(null==status || "".equals(status.trim())) {
			status = "0";
		}
		
		// default to read top five items
		if(null==topNum || topNum<=0) {
			topNum = 5;
		}
		
		String sql = null;
		Object[] params = null;
		if(null == appId) {
			sql = SQL_LIST_BY_RECEIVER;
			params = new Object[]{userId, status, 0, topNum};
		} else {
			sql = SQL_LIST_BY_RECEIVER_APPID;
			params = new Object[]{userId, status, appId, 0, topNum};
		}
		
		List<Information> result = jdbcTemplate.query(sql, params, new RowMapper<Information>(){
			
			public Information mapRow(ResultSet rs, int rn)
					throws SQLException {
				return createInformation(rs);
			}
		});
		informations.setInformations(result);
		
		return informations;
	}
	
	// count with information ID position
	
	public int countByPosition(Long mid, String userId, String status, Integer dir) {
		int res = 0;
		
		if(null==mid) {
			res = count(userId, status);
		} else {
			if(null==dir||dir>=0) { // forward
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITHOUT_STATUS_BY_POS, mid, userId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITH_STATUS_BY_POS, mid, userId, status);
				}
			} else {
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITHOUT_STATUS_BY_POS, mid, userId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITH_STATUS_BY_POS, mid, userId, status);
				}
			}
		}
		return res;
	}

	@Override
	public int countByPosition(Long mid, String appId, String userId,
			String status, Integer dir) {
		if(null==appId) {
			return countByPosition(mid, userId, status, dir);
		}
		
		int res = 0;
		
		if(null==mid) {
			res = count(appId, userId, status);
		} else {
			if(null==dir||dir>=0) { // forward
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITHOUT_STATUS_BY_APPID_POS, mid, userId, appId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_FWD_WITH_STATUS_BY_APPID_POS, mid, userId, status, appId);
				}
			} else {
				if(null==status) {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITHOUT_STATUS_BY_APPID_POS, mid, userId, appId);
				} else {
					res = jdbcTemplate.queryForInt(COUNT_PRE_WITH_STATUS_BY_APPID_POS, mid, userId, status, appId);
				}
			}
		}
		return res;
	}
	
	public int count(String userId, String status) {
		int res = 0;
		if(null==status) {
			res = jdbcTemplate.queryForInt(COUNT_WITHOUT_STATUS, userId);
		} else {
			res = jdbcTemplate.queryForInt(COUNT_WITH_STATUS, userId, status);
		}
		return res;
	}

	@Override
	public int count(String appId, String userId, String status) {
		if(null==appId) {
			return count(userId, status);
		}
		
		int res = 0;
		if(null==status) {
			res = jdbcTemplate.queryForInt(COUNT_BY_APPID_WITHOUT_STATUS, appId, userId);
		} else {
			res = jdbcTemplate.queryForInt(COUNT_BY_APPID_WITH_STATUS, appId, userId, status);
		}
		
		return res;
	}

	
	public void markProcessed(String id) {
		jdbcTemplate.update(SQL_UPDATE_PROCESSED_STATUS, new Object[]{id});
	}

	
	public void markRead(String id) {
		jdbcTemplate.update(SQL_UPDATE_READ_STATUS, new Object[]{id});
	}

	public static Information createInformation(ResultSet rs) throws SQLException {
		Information information = new Information();
		information.setId(rs.getLong(Information.INFO_ID));
		
		information.setAppId(rs.getString(Information.INFO_APP_ID));
		information.setReceiverId(rs.getString(Information.INFO_RECEIVER_ID));
		information.setSenderId(rs.getString(Information.INFO_SENDER_ID));
		information.setType(rs.getString(Information.INFO_TYPE));
		
		String uri = rs.getString(Information.INFO_URI);
		if(null != uri) {
			information.setUri(uri);
		}
		String title = rs.getString(Information.INFO_TITLE);
		if(null != title) {
			information.setTitle(title);
		}
		String data = rs.getString(Information.INFO_DATA);
		if(null != data) {
			information.setData(data);
		}
		information.setProcessed(rs.getBoolean(Information.INFO_PROCESSED));
		information.setRead(rs.getBoolean(Information.INFO_READED));
		information.setProcessMethod(rs.getInt(Information.INFO_PROCESS_METHOD));
		information.setImportance(rs.getInt(Information.INFO_IMPORTANCE));
		
		String body = rs.getString(Information.INFO_BODY);
		if(null != body) {
			information.setBody(body);
		}
		String titleHtml = rs.getString(Information.INFO_TITLE_HTML);
		if(null != titleHtml) {
			information.setTitleHtml(titleHtml);
		}
		String bodyHtml = rs.getString(Information.INFO_BODY_HTML);
		if(null != bodyHtml) {
			information.setBodyHtml(bodyHtml);
		}
		
		String objectId = rs.getString(Information.INFO_OBJECT_ID);
		if(null != objectId) {
			information.setObjectId(objectId);
		}

		information.setDate(rs.getTimestamp(Information.INFO_DATE).getTime());
		information.setLastModified(rs.getTimestamp(Information.INFO_LAST_MODIFIED).getTime());
		
		// the following will be deprecated.
		String action = rs.getString(Information.INFO_ACTION);
		if(null != action) {
			information.setAction(action);
		}
		
		String guid = rs.getString(Information.INFO_GUID);
		if(null != guid) {
			information.setGuid(guid);
		}
		
		return information;
	}

	
	public InformationList query(String appId, String type, String receiverId, String objectId) {
		InformationList informations = new InformationList();
		
		List<Information> result = jdbcTemplate.query(SQL_QUERY_BY_APPID_TYPE_RECEIVERID_OBJECTID, 
				new Object[]{appId, type, receiverId, objectId}, new RowMapper<Information>(){
			
			public Information mapRow(ResultSet rs, int rn)
					throws SQLException {
				return createInformation(rs);
			}
		});
		informations.setInformations(result);
		
		return informations;
	}

	
	public String replace(final Information info) {
//		jdbcTemplate.update(SQL_DELETE_BY_APPID_TYPE_RECEIVERID_OBJECTID, 
//				information.getAppId(), information.getType(), information.getReceiverId(), information.getObjectId());
		
		final List<String> result = jdbcTemplate.query(SQL_QUERY_BY_APPID_TYPE_RECEIVERID_OBJECTID, 
				new Object[]{info.getAppId(), info.getType(), 
					info.getReceiverId(), info.getObjectId()}, new RowMapper<String>(){
			
			public String mapRow(ResultSet rs, int rn)
					throws SQLException {
				return rs.getString(Information.INFO_ID);
			}
		});
		
		if(null == result || result.size() == 0) {
			KeyHolder keyHolder = new GeneratedKeyHolder();  
			jdbcTemplate.update(new PreparedStatementCreator() {
			    public PreparedStatement createPreparedStatement(Connection conn)
			            throws SQLException {
			    	PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
			    	informationToStatementForInsert(ps, info);
			        return ps;
			    }  
			}, keyHolder);
			return keyHolder.getKey().toString();	
		} else {
//			senderId,action,title,data,uri,processed,process_method,importance,title_html,body,body_html
//			appId=? and type=? and receiverId=? and objectId=?
			jdbcTemplate.update(SQL_UPDATE_BY_ATRO, 
					info.getSenderId(), info.getUri(), info.getTitle(),
					info.getData(), info.getUri(), false,
					info.getProcessMethod(), info.getImportance(), 
					info.getTitleHtml(), info.getBody(), info.getBodyHtml(), 
					info.getAppId(), info.getType(), info.getReceiverId(), info.getObjectId());
			
			if(result.size()==0) {
				return "";
			} else if(result.size() == 1) {
				return result.get(0);
			} else {
				StringBuilder sb = new StringBuilder();
				for(String id : result) {
					if(sb.length()==0) {
						sb.append(id);
					} else {
						sb.append(",").append(id);
					}
				}
				return sb.toString();
			}
		}
	}

    @Override
    public InformationList userListByTime(String userId, String status, int type,String scene, Long from, Integer count) {
        return null;
    }

    @Override
    public InformationList userTop(String appId, String userId, Integer type, String status,String scene, Integer topNum) {
        return null;
    }

    @Override
    public InformationList userReadListByTime(String userId, String status, int type,String scene, int read, Long from, Integer count) {
        return null;
    }

    @Override
    public DBObject queryNotifByGroup(String userId, String scene) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
