/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.borqs.server.service.platform;

@SuppressWarnings("all")
public interface ReportAbuse {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"ReportAbuse\",\"namespace\":\"com.borqs.server.service.platform\",\"types\":[{\"type\":\"error\",\"name\":\"ResponseError\",\"namespace\":\"com.borqs.server.base\",\"fields\":[{\"name\":\"code\",\"type\":\"int\"},{\"name\":\"message\",\"type\":\"string\"}]}],\"messages\":{\"saveReportAbuse\":{\"request\":[{\"name\":\"reportAbuse\",\"type\":\"bytes\"}],\"response\":\"boolean\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"getReportAbuseCount\":{\"request\":[{\"name\":\"post_id\",\"type\":\"string\"}],\"response\":\"int\",\"errors\":[\"com.borqs.server.base.ResponseError\"]},\"iHaveReport\":{\"request\":[{\"name\":\"viewerId\",\"type\":\"string\"},{\"name\":\"post_id\",\"type\":\"string\"}],\"response\":\"int\",\"errors\":[\"com.borqs.server.base.ResponseError\"]}}}");
  boolean saveReportAbuse(java.nio.ByteBuffer reportAbuse) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  int getReportAbuseCount(java.lang.CharSequence post_id) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;
  int iHaveReport(java.lang.CharSequence viewerId, java.lang.CharSequence post_id) throws org.apache.avro.AvroRemoteException, com.borqs.server.base.ResponseError;

  @SuppressWarnings("all")
  public interface Callback extends ReportAbuse {
    public static final org.apache.avro.Protocol PROTOCOL = com.borqs.server.service.platform.ReportAbuse.PROTOCOL;
    void saveReportAbuse(java.nio.ByteBuffer reportAbuse, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void getReportAbuseCount(java.lang.CharSequence post_id, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void iHaveReport(java.lang.CharSequence viewerId, java.lang.CharSequence post_id, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
  }
}