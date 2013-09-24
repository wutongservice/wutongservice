package com.borqs.server.photo.servlet;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.image.ImageException;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.photo.PhotoException;
import com.borqs.server.photo.SimplePhoto;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TimerTask;

public class PhotoServlet extends WebMethodServlet {

    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private StaticFileStorage photoStorage;
    private SimplePhoto photo;
    
	private static String PHOTO_TYPE_SMALL = "small";
	private static String PHOTO_TYPE_ORIGINAL = "original";
	private static String PHOTO_TYPE_LARGE = "large";
	private String SERVER_HOST = "api.borqs.com";
	StreamTask sTask;
    
	public PhotoServlet() {
		
	}
    @Override
	public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();

        SERVER_HOST = conf.getString("server.host", "api.borqs.com");
        transceiverFactory.setConfig(conf);
        transceiverFactory.init();
        
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
        
        photo = new SimplePhoto();
        photo.setConfig(conf);
        photo.init();
    }
    @Override
    public void destroy() {
    	super.destroy();
    }
    
    @Override
    protected String getXmlDocumentPath() {
        return "document/photo";
    }

    @Override
    protected String getXmlDocument() {
    	return null;
//        return getConfiguration().getBoolean("qiupu.servlet.document", false)
//                ? TextLoader.loadClassPath(QiupuServlet.class, "qiupu_servlet_document.xml")
//                : null;
    }
	
    @WebMethod("photo/create_album")
    public boolean createAlbum(QueryParams qp) throws AvroRemoteException{
        Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't save");
		String name = qp.checkGetString("name");
		String privacy = qp.getString("privacy", "default");
		String description = qp.getString("description", "");
		String location = qp.getString("location", "");
		String aid = Long.toString(RandomUtils.generateId());
		long uploaded_time = DateUtils.nowMillis();
		Record rc = new Record();

		rc.put("aid", aid);
		rc.put("user_id", userId);
		rc.put("name", name);
		rc.put("created", uploaded_time);
		rc.put("description", description);
		rc.put("location", location);
		rc.put("visible", privacy);
		return photo.createAlbum(rc);
    }
    
    @WebMethod("photo/get_allalbum")
    public RecordSet getAlbums(QueryParams qp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't query album");
        
        return photo.getAllAlbum(userId);
    }
    
    @WebMethod("photo/update_album")
    public boolean updateAlbum(QueryParams qp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't update album");
        
        String albumId = qp.checkGetString("aid");
        String name = qp.getString("name", null);
        String description = qp.getString("description", null);
        String location = qp.getString("location", null);
        String privacy = qp.getString("privacy", null);
        
        Record rc = new Record();
        if(StringUtils.isNotBlank(name)){
        	rc.put("name", name);
        }
        if(StringUtils.isNotBlank(description)){
        	rc.put("description", description);
        }
        if(StringUtils.isNotBlank(location)){
        	rc.put("location", location);
        }
        if(StringUtils.isNotBlank(privacy)){
        	rc.put("visible",privacy);
        }
        
        return photo.updateAlbum(albumId, rc);
    }
    
    @WebMethod("photo/delete_album")
    public boolean deleteAlbum(QueryParams qp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't delete album");
        
        String albumId = qp.checkGetString("aid");
    	return photo.deleteAlbumById(albumId, photoStorage);
    	
    }
    @WebMethod("photo/get_photo")
    public NoResponse getPhotoById(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        
        String pID = qp.checkGetString("pid");
        String ft = qp.getString("filetype", PHOTO_TYPE_ORIGINAL); 
        
        SFSUtils.writeResponse(resp, photoStorage, getFileName(pID, ft),"image/JPEG");
        
        return NoResponse.get();
    }
    
    @WebMethod("photo/photo")
    public NoResponse downloadPhoto(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException{
        String pID = qp.checkGetString("pid");
        String ft = qp.getString("filetype", PHOTO_TYPE_ORIGINAL); 
        SFSUtils.writeResponse(resp, photoStorage, getFileName(pID, ft),"image/JPEG");
    	return NoResponse.get();
    }
    
    private String genDownloadURL(String pid, String filetype){
    	return "http://" + SERVER_HOST +"/photo/photo?pid=" + pid + "&&filetype=" + filetype;
    }
    
    private String getFileName(String pId, String filetype) {
    	
    	if (null == photo)
    		throw new PhotoException("server error, can't get");
        
        Record rc = photo.getPhotoById(pId);
        
        if (rc.isEmpty())
        	throw new PhotoException("photo is not exist!!");
        if (PHOTO_TYPE_LARGE.equals(filetype)){
        	return rc.getString("src_big");
        } else if (PHOTO_TYPE_SMALL.equals(filetype)){
        	return rc.getString("src_small");
        } else
        	return rc.getString("src");
    	
    }
    
    private void saveUploadPhoto(FileItem fileItem, String file){
    	
        
        SFSUtils.saveScaledUploadImage(fileItem, photoStorage, file + "_O.jpg", null, null, "jpg");
        
        int width, height, sWidth, sHeight;
        
        try {
			BufferedImage image = ImageIO.read(fileItem.getInputStream());
			width = image.getWidth();
			height = image.getHeight();
			
			sWidth = 64 * width / height;
			sHeight = 64 * height / width;
			if (width == height){
				width = height = 480;
			} else if (width > height){
				if (width > 640) {
					height = (int) 640 * height / width;
					width = 640;
				}
			} else if (height > width) {
				if (height > 640) {
					width = (int) 640 * width / height;
					height = 640;
				}
			}
			
			
		} catch (IOException e) {
			throw new ImageException(e);
		}
        
        SFSUtils.saveScaledUploadImage(fileItem, photoStorage, file + "_S.jpg",
        		Integer.toString(sWidth), Integer.toString(sHeight), "jpg");
        
        SFSUtils.saveScaledUploadImage(fileItem, photoStorage, file + "_L.jpg", 
        			Integer.toString(width), Integer.toString(height), "jpg");
    }
    @WebMethod("photo/upload_photo")
    public boolean uploadPhoto(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        
    	FileItem fi = qp.checkGetFile("image_data");
    	if (null == photo)
    		throw new PhotoException("server error, can't save");
    	
    	String albumId = qp.getString("aid","");
    	
    	if (StringUtils.isEmpty(albumId))
    	 	albumId = photo.getDefaultAlbum(userId);
    	
    	if (!photo.isAlbumExist(albumId)) {
    		throw new PhotoException("album not exist, please create album first");
    	}
    	
    	String name = fi.getName();
    	
    	long uploaded_time = DateUtils.nowMillis();
        String imageName = userId + "/" + userId + "_" + name + "_" + uploaded_time;
    	

        saveUploadPhoto(fi, imageName);

		String photoID = photo.genPhotoId(userId);
		Record rc = new Record();
		rc.put("pid", photoID);
		rc.put("aid", albumId);
		rc.put("user_id", userId);
		rc.put("src", imageName + "_O.jpg");
		rc.put("src_big", imageName + "_L.jpg");
		rc.put("src_small", imageName + "_S.jpg");
		rc.put("caption", name);
		rc.put("created", DateUtils.nowMillis());
    	
		boolean result = photo.saveUploadPhoto(rc);
		
		if (result) {
			//TODO: create scream
//			if (null == sTask || !sTask.isPending()) {
//				Timer timer = new Timer();
//				sTask = new StreamTask();
//				timer.schedule(sTask, 2 * 60 * 1000);
//			} else {
//				sTask.photoMap.put(key, value);
//			}
			Record sRecord = new Record();
			Record album = photo.getAlbumById(albumId);
			sRecord.put("AlbumId", album.getString("aid"));
			sRecord.put("AlbumName", album.getString("name"));
			sRecord.put("AlbumSize", album.getString("asize"));
			sRecord.put("AlbumCoverUrl", genDownloadURL(album.getString("cover_pid"), PHOTO_TYPE_LARGE));
			sRecord.put("AlbumDescription", album.getString("description"));
			sRecord.put("visible", album.getString("visible"));
			sRecord.put("Location", album.getString("location"));
			//create one scream for one photo
			sRecord.put("photoSize", 1);
			sRecord.put("photoInfo", StringUtils2.join(",", photoID, genDownloadURL(photoID, PHOTO_TYPE_LARGE)));
			
			String postId;
			postId = p.post(userId, Constants.PHOTO_POST, name, sRecord.toString(),
					ObjectUtils.toString(Constants.APP_TYPE_PHOTO, ""), "", "",
					null, "", "default".equals(album.getString("visible")), "", "", album.getString("location"), "", "",true,true,true,"");
		}
    	return result;
    }
    
    class StreamTask extends TimerTask {
    	boolean pending = true;
    	public StringMap photoMap = new StringMap();
    	@Override
    	public void run() {
    		
    		
    		pending = false;
    	}
    	public void setPending(boolean pend) {
    		pending = pend;
    	}
    	public boolean isPending() {
    		return pending;
    	}
    }
    @WebMethod("photo/update_photo")
    public boolean updatePhoto(QueryParams qp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't update photo");
        
        String pID = qp.checkGetString("pid");
        String caption = qp.getString("caption", null);
        Record rc = new Record();
        rc.put("caption", caption);
        
    	return photo.updatePhoto(pID, rc);
    	
    }
    @WebMethod("photo/delete_photo")
    public boolean deletePhoto(QueryParams qp) throws AvroRemoteException{
    	Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
    		throw new PhotoException("server error, can't delete photo");
        
        String pIDs = qp.checkGetString("pids");
        
        return photo.deletePhotoById(pIDs, photoStorage);
    }
    
    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }
}
