/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.borqs.server.service.platform;

@SuppressWarnings("all")
public interface Conversation {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"Conversation\",\"namespace\":\"com.borqs.server.service.platform\",\"types\":[{\"type\":\"error\",\"name\":\"ResponseError\",\"namespace\":\"com.borqs.server.base\",\"fields\":[{\"name\":\"code\",\"type\":\"int\"},{\"name\":\"message\",\"type\":\"string\"}]}],\"messages\":{\"createConversation\":{\"request\":[{\"name\":\"conversation\",\"type\":\"bytes\"}],\"response\":\"boolean\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"deleteConversation\":{\"request\":[{\"name\":\"target_type\",\"type\":\"int\"},{\"name\":\"target_id\",\"type\":\"string\"},{\"name\":\"reason\",\"type\":\"int\"},{\"name\":\"from\",\"type\":\"long\"}],\"response\":\"boolean\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"getConversation\":{\"request\":[{\"name\":\"target_type\",\"type\":\"int\"},{\"name\":\"target_id\",\"type\":\"string\"},{\"name\":\"reasons\",\"type\":\"string\"},{\"name\":\"from\",\"type\":\"long\"},{\"name\":\"page\",\"type\":\"int\"},{\"name\":\"count\",\"type\":\"int\"}],\"response\":\"bytes\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"ifExistConversation\":{\"request\":[{\"name\":\"target_type\",\"type\":\"int\"},{\"name\":\"target_id\",\"type\":\"string\"},{\"name\":\"reason\",\"type\":\"int\"},{\"name\":\"from\",\"type\":\"long\"}],\"response\":\"boolean\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"updateConversationTarget\":{\"request\":[{\"name\":\"old_target_id\",\"type\":\"string\"},{\"name\":\"new_target_id\",\"type\":\"string\"}],\"response\":\"boolean\",\"errors\":[\"com.borqs.server.base.ResponseError\"]}}}");
  boolean createConversation(java.nio.ByteBuffer conversation) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  boolean deleteConversation(int target_type, java.lang.CharSequence target_id, int reason, long from) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  java.nio.ByteBuffer getConversation(int target_type, java.lang.CharSequence target_id, java.lang.CharSequence reasons, long from, int page, int count) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  boolean ifExistConversation(int target_type, java.lang.CharSequence target_id, int reason, long from) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  boolean updateConversationTarget(java.lang.CharSequence old_target_id, java.lang.CharSequence new_target_id) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;

  @SuppressWarnings("all")
  public interface Callback extends Conversation {
    public static final org.apache.avro.Protocol PROTOCOL = com.borqs.server.service.platform.Conversation.PROTOCOL;
    void createConversation(java.nio.ByteBuffer conversation, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void deleteConversation(int target_type, java.lang.CharSequence target_id, int reason, long from, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void getConversation(int target_type, java.lang.CharSequence target_id, java.lang.CharSequence reasons, long from, int page, int count, org.apache.avro.ipc.Callback<java.nio.ByteBuffer> callback) throws java.io.IOException;
    void ifExistConversation(int target_type, java.lang.CharSequence target_id, int reason, long from, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void updateConversationTarget(java.lang.CharSequence old_target_id, java.lang.CharSequence new_target_id, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
  }
}