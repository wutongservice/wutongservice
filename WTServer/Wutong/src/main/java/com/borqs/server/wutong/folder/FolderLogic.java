package com.borqs.server.wutong.folder;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.net.MalformedURLException;
import java.util.List;

public interface FolderLogic {
    boolean createFolder(Context ctx,Record record);
    RecordSet getUserFolder(Context ctx,String viewerId, String userId);
    RecordSet getFolderFiles(Context ctx,String viewerId, String folder_id, int page, int count);
    Record getFolderOriginal(Context ctx,String folder_id);
    boolean isFolderExist(Context ctx,String folder_id);
    boolean isMyFolderExist(Context ctx,String userId,int folder_type,String folder_name);
    boolean isMyFolderExistType(Context ctx,String userId,int folder_type);
    boolean updateFolder(Context ctx,String folder_id, Record rc);
    boolean deleteFolderById(Context ctx,String userId, String folder_id);
    int getFolderSize(Context ctx,String viewerId, String folder_id);
    Record getFolderById(Context ctx,String viewerId,String userId, String folder_id);
    boolean updateStaticFileStreamId(Context ctx,String stream_id, List<String> file_ids);
    String getFolder(Context ctx,String userId,int folder_type,String folderName);
    boolean deleteFileById(Context ctx,String viewerId, String file_ids, boolean delete_all);
    boolean updateFile(Context ctx,String file_id, Record rc);
    boolean updateFileStreamId(Context ctx,int folder_type,String asc);
    boolean saveStaticFile(Context ctx,Record staticFile);
    boolean saveStaticFiles(Context ctx,RecordSet staticFiles);
    RecordSet getStaticFileByIds(Context ctx,String file_ids);
    RecordSet getOriginalStaticFileByIds(Context ctx,String file_ids);
    boolean saveVideo(Context ctx,Record video);
    Record getVideoById(Context ctx,String file_id,String author);
    boolean saveAudio(Context ctx,Record audio);
    Record getAudioById(Context ctx,String file_id);



}