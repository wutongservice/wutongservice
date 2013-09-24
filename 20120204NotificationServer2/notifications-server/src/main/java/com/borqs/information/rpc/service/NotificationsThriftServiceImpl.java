package com.borqs.information.rpc.service;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.notifications.thrift.INotificationsThriftService;
import com.borqs.notifications.thrift.Info;
import com.borqs.notifications.thrift.NotificationUnreadResult;
import com.borqs.notifications.thrift.StateResult;
import com.mongodb.DBObject;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationsThriftServiceImpl extends NotificationsServiceCommonImpl implements INotificationsThriftService.Iface {
    private static Logger logger = LoggerFactory.getLogger(NotificationsThriftServiceImpl.class);

    public StateResult sendInf(Info info) throws org.apache.thrift.TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            Information information = NotificationsThriftHelper.convertToInformation(info);
            if (null == information.getSenderId() || information.getSenderId().equals("")) {
                throw new Exception("Sender ID can not be null or blank!");
            }
            String mid = send(information);
            state.setMid(mid);
            state.setStatus("success");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    public StateResult batchSendInf(List<Info> infos) throws org.apache.thrift.TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            StringBuilder sb = new StringBuilder();
            for (Info si : infos) {
                if (null == si.getSenderId() || si.getSenderId().equals("")) {
                    continue;
                }
                Information information = NotificationsThriftHelper.convertToInformation(si);
                String ids = send(information);
                if (0 == sb.length()) {
                    sb.append(ids);
                } else if (null != ids && ids.length() > 0) {
                    sb.append(",").append(ids);
                }
            }
            state.setMid(sb.toString());
            state.setStatus("success");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    public StateResult markProcessed(String mid) throws org.apache.thrift.TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            dao.markProcessed(mid.toString());
            state.setStatus("success");
            state.setMid(mid);
            logger.info("markProcessed by IPC->" + state.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    public StateResult markRead(String mid) throws org.apache.thrift.TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            dao.markRead(mid.toString());
            state.setStatus("success");
            state.setMid(mid);
            logger.info("markProcessed by IPC->" + state.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    public List<Info> queryInfo(String appId, String type, String receiverId, String objectId) throws org.apache.thrift.TException {
        InformationList result = exeQueryForList(appId, type, receiverId, objectId);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    public StateResult replaceInf(Info info) throws TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            if (null == info.getSenderId() || info.getSenderId().equals("")) {
                throw new Exception("Sender ID can not be blank or null!");
            }

            Information information = NotificationsThriftHelper.convertToInformation(info);
            String receiverId = information.getReceiverId();
            String[] receivers = receiverId.split(",");
            for (String rid : receivers) {
                information.setReceiverId(rid);

                if (null != mqPublisher && information.isPush()) {
                    try {
                        mqPublisher.send(information);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("failed to push message to user(" + information.getReceiverId() + ")");
                    }
                }
                String mid = dao.replace(information);
                if (null == state.getMid() || "-1".equals(state.getMid())) {
                    state.setMid(mid);
                } else {
                    state.setMid(state.getMid() + "," + mid);
                }
                logger.info("replaceInfo by IPC->" + information.toString());
            }
            state.setStatus("success");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    public StateResult batchReplaceInf(List<Info> infos) throws org.apache.thrift.TException {
        StateResult state = new StateResult();
        state.setStatus("failed");
        try {
            StringBuilder sb = new StringBuilder();
            for (Info info : infos) {
                if (null == info.getSenderId() || info.getSenderId().equals("")) {
                    continue;
                }

                Information information = NotificationsThriftHelper.convertToInformation(info);
                String mid = dao.replace(information);
                if (sb.length() == 0) {
                    sb.append(mid);
                } else {
                    sb.append(",").append(mid);
                }
            }
            state.setMid(sb.toString());
            state.setStatus("success");
            logger.info("replaceInfo by IPC->" + state.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new TException(e);
        }
        return state;
    }

    @Override
    public List<Info> listAll(String receiverId, String status, long from,
                              int size) throws TException {
        InformationList result = dao.list(receiverId, status, from, size);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    @Override
    public List<Info> listAllOfApp(String appId, String receiverId,
                                   String status, long from, int size) throws TException {
        InformationList result = dao.list(appId, receiverId, status, from, size);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    public List<Info> listById(String receiverId, String status, long mid, int count) throws org.apache.thrift.TException {
        InformationList result = dao.listById(receiverId, status, mid, count);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    @Override
    public List<Info> listOfAppById(String appId, String receiverId,
                                    String status, long mid, int count) throws TException {
        InformationList result = dao.listById(appId, receiverId, status, mid, count);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    public List<Info> listByTime(String receiverId, String status, long from, int count) throws org.apache.thrift.TException {
        logger.info("receiverId=" + receiverId + ",status=" + status + ",from=" + from + ",count=" + count);
        InformationList result = dao.listByTime(receiverId, status, from, count);
        logger.info("thrift list by time: " + result + ",count=" + result.getCount());
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        logger.info("thrift list by time after convertToInfos: " + infos + ",size=" + infos.size());
        return infos;
    }

    @Override
    public List<Info> listOfAppByTime(String appId, String receiverId,
                                      String status, long from, int count) throws TException {
        logger.info("appId=" + appId + ",receiverId=" + receiverId + ",status=" + status + ",from=" + from + ",count=" + count);
        InformationList result = dao.listByTime(appId, receiverId, status, from, count);
        logger.info("thrift list by time of App: " + result + ",count=" + result.getCount());
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        logger.info("thrift list by time of App after convertToInfos: " + infos + ",size=" + infos.size());
        return infos;
    }

    public List<Info> top(String receiverId, String status, int topn) throws org.apache.thrift.TException {
        InformationList result = dao.top(receiverId, status, topn);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    @Override
    public List<Info> topOfApp(String appId, String receiverId, String status,
                               int topn) throws TException {
        InformationList result = dao.top(appId, receiverId, status, topn);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    // by JSON
    public String send(String message) throws org.apache.thrift.TException {
        try {
            return exeSend(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public String batchSend(String messages) throws org.apache.thrift.TException {
        try {
            return exeBatchSend(messages);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public String process(String mid) throws org.apache.thrift.TException {
        try {
            return exeProcess(mid);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public String query(String appId, String type, String receiverId, String objectId) throws org.apache.thrift.TException {
        try {
            return exeQuery(appId, type, receiverId, objectId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public String replace(String message) throws org.apache.thrift.TException {
        try {
            return exeReplace(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public String batchReplace(String messages) throws org.apache.thrift.TException {
        try {
            return exeBatchReplace(messages);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    public int count(String receiverId, String status) throws org.apache.thrift.TException {
        try {
            return exeCount(receiverId, status);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    @Override
    public int countOfApp(String appId, String receiverId, String status)
            throws TException {
        try {
            return exeCount(appId, receiverId, status);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    /**
     * add by wangpeng
     *
     * @param receiverId
     * @param status
     * @param type
     * @param scene
     * @param from
     * @param count
     * @return
     * @throws TException
     */
    @Override
    public List<Info> userListByTime(String receiverId, String status, int type, String scene, long from, int count) throws TException {
        logger.info("receiverId=" + receiverId + ",status=" + status + ",type=" + type + ",from=" + from + ",count=" + count + ",scene=" + scene);
        InformationList result = dao.userListByTime(receiverId, status, type, scene, from, count);
        logger.info("thrift list by time: " + result + ",count=" + result.getCount());
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        logger.info("thrift list by time after convertToInfos: " + infos + ",size=" + infos.size());
        return infos;
    }

    @Override
    public List<Info> userTop(String appId, String receiverId, String status, int type,String scene, int topn) throws TException {
        InformationList result = dao.userTop(appId, receiverId, type, status,scene, topn);
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        return infos;
    }

    /**
     * add by wangpeng
     *
     * @param receiverId
     * @param status
     * @param type
     * @param read
     * @param scene
     * @param from
     * @param count
     * @return
     * @throws TException
     */
    @Override
    public List<Info> userReadListByTime(String receiverId, String status, int type, int read, String scene, long from, int count) throws TException {
        logger.info("receiverId=" + receiverId + ",status=" + status + ",type=" + type + ",read=" + read + ",from=" + from + ",count=" + count + ",scene=" + scene);
        InformationList result = dao.userReadListByTime(receiverId, status, type, scene, read, from, count);
        logger.info("thrift user read list by time: " + result + ",count=" + result.getCount());
        List<Info> infos = NotificationsThriftHelper.convertToInfos(result.getInformations());
        logger.info("thrift user read list by time after convertToInfos: " + infos + ",size=" + infos.size());
        return infos;
    }

    /**
     * add by wangpeng
     *
     * @param userId
     * @param scene
     * @return
     * @throws TException
     */
    @Override
    public List<NotificationUnreadResult> getUnReadResultByScene(String userId, String scene) throws TException {
        logger.info("userId=" + userId + " ,scene=" + scene);
        DBObject dbObject = dao.queryNotifByGroup(userId, scene);
        return NotificationsThriftHelper.notifGroupConverter(dbObject);
    }


}
