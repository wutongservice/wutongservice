package com.borqs.server.photo;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;

public class SimplePhoto extends ConfigurableBase{
	
	public final Schema photoSchema = Schema.loadClassPath(SimplePhoto.class, "photo.schema");
	public final Schema albumSchema = Schema.loadClassPath(SimplePhoto.class, "album.schema");
	
    private ConnectionFactory connectionFactory;
    private String db;
    
	public void init() {
		Configuration conf = getConfig();

		this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
				.getString("qiupu.simple.connectionFactory", "dbcp"));

		this.db = conf.getString("photo.simple.db", null);

	}

    public void destroy() {
    	this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }
    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }
    
	public boolean createAlbum(Record record){
		String name = record.getString("name", null);
		
		if (StringUtils.isEmpty(name)){
			throw new PhotoException("name is null , can't create");
		}
        
        final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", "album",
                "alias", albumSchema.getAllAliases(),
                "info", record);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
	}
	
	public RecordSet getAllAlbum(String uid){
		
		final String SQL = "SELECT aid,cover_pid,user_id,name,created,modified,description," +
				"location,asize,link from album WHERE user_id = " + "'" + uid + "'";
		SQLExecutor se = getSqlExecutor();
		RecordSet rs = se.executeRecordSet(SQL.toString(), null);
		
		for (Record rc : rs) {
			String pids = getAllPIDInAlbum(rc.getString("aid"));
			
			rc.put("pids", pids);
		} 
		
		return rs;
		
	}
	
	private String getAllPIDInAlbum(String aid) {
		
		final String sql = "SELECT pid from photo WHERE aid = " + "'" + aid + "'";
		SQLExecutor se = getSqlExecutor();
		RecordSet rs = se.executeRecordSet(sql.toString(), null);
		
		return rs.toString().toString();
		
	}
	
	public boolean isAlbumExist(String aid){
		SQLExecutor se = getSqlExecutor();
		final String SQL = "SELECT * from album WHERE aid = " + "'" + aid + "'";
		Record rec = se.executeRecord(SQL.toString(), null);
		if (null != rec && rec.isEmpty()){
			return false;
		}
		return true;
	}
	
	public boolean updateAlbum(String aid, Record rc){
		long update_time = DateUtils.nowMillis();
		String cover_pid = getLatestPhotoId(aid);
		rc.put("cover_pid", cover_pid);
		
		String sql = new SQLBuilder.Update(albumSchema).update("album").values(rc).value("modified", update_time)
						.where("aid = " + "'" + aid +"'").toString();

		SQLExecutor se = getSqlExecutor();
		long n = se.executeUpdate(sql);
		return n > 0;
	}
	
	public boolean deleteAlbumById(String aid , StaticFileStorage photoStorage){
		SQLExecutor se = getSqlExecutor();
		//delete the storage files
		String sql = "SELECT * from photo WHERE aid = " + "'" + aid +"'";
		RecordSet rs = se.executeRecordSet(sql, null);
		if (rs.size() > 0) {
			for (Record r : rs) {
				photoStorage.delete(r.getString("src"));
				photoStorage.delete(r.getString("src_big"));
				photoStorage.delete(r.getString("src_small"));
			}
		}
		// delete photo DB
		sql = "DELETE from photo WHERE aid = " + "'" + aid + "'";
		long n = se.executeUpdate(sql);

		//delete album DB
		sql = "DELETE from album WHERE aid = " + "'" + aid + "'";
		n = se.executeUpdate(sql);
		
		return n > 0;
		
	}
	public boolean saveUploadPhoto(Record record){
		String aid = record.getString("aid", null);
		if (null == aid)
			throw new PhotoException("no album , can't save");
		
		final String  SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", "photo",
                "alias", photoSchema.getAllAliases(),
                "info", record);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        
        //update album size
        if (n > 0 ) {
        	updateAlbumSizeInDB(aid);
        }
        return n > 0;
	}
	
	private void updateAlbumSizeInDB(String aid){
		Record rc = new Record();
		rc.put("asize", getAlbumSize(aid));
		updateAlbum(aid, rc);
	}
	public long getAlbumSize(String aid){
		final String sql = "SELECT COUNT(*) from photo WHERE aid = " + "'" + aid + "'";
		SQLExecutor se = getSqlExecutor();
		Number count = (Number) se.executeScalar(sql);
		return count.intValue();
	}
	private String getLatestPhotoId(String aid){
		String sql = "SELECT * from photo WHERE aid = "+ "'" + aid + "' ORDER BY created DESC" ;
		SQLExecutor se = getSqlExecutor();
		RecordSet rs = se.executeRecordSet(sql.toString(), null);
		
		return rs.getFirstRecord().getString("pid");
	}
	
	public Record getAlbumById(String aid){
		String sql = "SELECT * from album WHERE aid = " + "'" + aid + "'";
		SQLExecutor se = getSqlExecutor();
		Record rec = se.executeRecord(sql.toString(), null);
		
		return rec;
	}
	
	public Record getPhotoById(String pID){
		String sql = "SELECT * from photo WHERE pid = " + "'" + pID + "'";
		SQLExecutor se = getSqlExecutor();
		Record rec = se.executeRecord(sql.toString(), null);
		
		String aid = rec.getString("aid", null);
		if (null != aid){
			sql = "SELECT * from album WHERE aid = " + "'" + aid + "'";
			Record rec1 = se.executeRecord(sql.toString(), null);
			if (null != rec1 && rec1.isEmpty())
				throw new PhotoException("no such album, query error");
		}
			
		return rec;
	}
	
	public boolean updatePhoto(String pid, Record rc){
		long update_time = DateUtils.nowMillis();
		
		String sql = new SQLBuilder.Update(photoSchema).update("photo").values(rc).value("modified", update_time)
				.where("pid = " + "'" + pid +"'").toString();
		SQLExecutor se = getSqlExecutor();
		long n = se.executeUpdate(sql);
		return n > 0;
	}
	
	public boolean deletePhotoById(String pids , StaticFileStorage photoStorage){
		List<String> l = StringUtils2.splitList(pids, ",", true);
		
		ArrayList<String> pidlist = new ArrayList<String>();
        for (String s : l)
        	pidlist.add(s);
		SQLExecutor se = getSqlExecutor();
		//delete the storage files
		String SQL = "SELECT * from photo WHERE pid IN " + " (${vjoin(pids)})";
		String sql = SQLTemplate.merge(SQL, "pids", pidlist);
		
		RecordSet rs = se.executeRecordSet(sql.toString(), null);
		if (rs.size() > 0) {
			for (Record rec : rs) {
				if (rec.isEmpty())
					continue;
				photoStorage.delete(rec.getString("src"));
				photoStorage.delete(rec.getString("src_big"));
				photoStorage.delete(rec.getString("src_small"));
			}
		}
		
		SQL = "SELECT DISTINCT aid from photo WHERE pid IN " + " (${vjoin(pids)})";
		sql = SQLTemplate.merge(SQL, "pids", pidlist);
		RecordSet rs1 = se.executeRecordSet(sql.toString(), null);
		
		// delete photo DB
		SQL = "DELETE from photo WHERE pid IN " + " (${vjoin(pids)})";
		sql = SQLTemplate.merge(SQL, "pids", pidlist);
		long n = se.executeUpdate(sql);
		
		// update album size
        if (n > 0 ) {
        	for (Record rc1 : rs1)
        		updateAlbumSizeInDB(rc1.getString("aid"));
        }
		
		return n > 0;
	}
	public String getDefaultAlbum(String uid){
		if (!isDefaultAlbumExist(uid))
			createDefaultAlbum(uid);
		
		String sql = "SELECT * from album WHERE user_id = " + "'" + uid + "' AND name = " + "'" 
						+ "User default Album" + "' ORDER BY created";
		SQLExecutor se = getSqlExecutor();
		RecordSet rec = se.executeRecordSet(sql.toString(), null);
		
		if (rec.size() > 0) {
			return rec.getFirstRecord().getString("aid");
		} else
			throw new PhotoException("can't get default album");
	}
	
	private boolean createDefaultAlbum(String uid){
		Record rc = new Record();
		String aid = Long.toString(RandomUtils.generateId());
		long uploaded_time = DateUtils.nowMillis();

		rc.put("aid", aid);
		rc.put("user_id", uid);
		rc.put("name", "User default Album");
		rc.put("created", uploaded_time);
		rc.put("visible", "default");
		
		return createAlbum(rc);
	}
	
	private boolean isDefaultAlbumExist(String uid) {
		String sql = "SELECT * from album WHERE user_id = " + "'" + uid + "' AND name = " + "'" 
					+ "User default Album" + "'" ;
		SQLExecutor se = getSqlExecutor();
		Record rec = se.executeRecord(sql.toString(), null);
		
		return !rec.isEmpty();
	}
	public String genPhotoId(String uid) {
        return uid+ "_" +Long.toString(RandomUtils.generateId());
    }
}
