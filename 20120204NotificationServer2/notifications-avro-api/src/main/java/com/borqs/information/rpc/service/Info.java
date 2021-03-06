/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.borqs.information.rpc.service;  
@SuppressWarnings("all")
public class Info extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"Info\",\"namespace\":\"com.borqs.information.rpc.service\",\"fields\":[{\"name\":\"id\",\"type\":[\"null\",\"long\"]},{\"name\":\"appId\",\"type\":\"string\"},{\"name\":\"senderId\",\"type\":\"string\"},{\"name\":\"receiverId\",\"type\":\"string\"},{\"name\":\"type\",\"type\":\"string\"},{\"name\":\"uri\",\"type\":[\"null\",\"string\"]},{\"name\":\"title\",\"type\":[\"null\",\"string\"]},{\"name\":\"data\",\"type\":[\"null\",\"string\"]},{\"name\":\"processMethod\",\"type\":[\"null\",\"int\"]},{\"name\":\"processed\",\"type\":[\"null\",\"boolean\"]},{\"name\":\"read\",\"type\":[\"null\",\"boolean\"]},{\"name\":\"importance\",\"type\":[\"null\",\"int\"]},{\"name\":\"body\",\"type\":[\"null\",\"string\"]},{\"name\":\"bodyHtml\",\"type\":[\"null\",\"string\"]},{\"name\":\"titleHtml\",\"type\":[\"null\",\"string\"]},{\"name\":\"objectId\",\"type\":[\"null\",\"string\"]},{\"name\":\"cDateTime\",\"type\":[\"null\",\"long\"]},{\"name\":\"lastModified\",\"type\":[\"null\",\"long\"]},{\"name\":\"guid\",\"type\":[\"null\",\"string\"]},{\"name\":\"action\",\"type\":[\"null\",\"string\"]},{\"name\":\"push\",\"type\":[\"null\",\"boolean\"]},{\"name\":\"scene\",\"type\":[\"null\",\"string\"]},{\"name\":\"imageUrl\",\"type\":[\"null\",\"string\"]}]}");
  public java.lang.Long id;
  public java.lang.CharSequence appId;
  public java.lang.CharSequence senderId;
  public java.lang.CharSequence receiverId;
  public java.lang.CharSequence type;
  public java.lang.CharSequence uri;
  public java.lang.CharSequence title;
  public java.lang.CharSequence data;
  public java.lang.Integer processMethod;
  public java.lang.Boolean processed;
  public java.lang.Boolean read;
  public java.lang.Integer importance;
  public java.lang.CharSequence body;
  public java.lang.CharSequence bodyHtml;
  public java.lang.CharSequence titleHtml;
  public java.lang.CharSequence objectId;
  public java.lang.Long cDateTime;
  public java.lang.Long lastModified;
  public java.lang.CharSequence guid;
  public java.lang.CharSequence action;
  public java.lang.Boolean push;
  public java.lang.CharSequence scene;
  public java.lang.CharSequence imageUrl;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return appId;
    case 2: return senderId;
    case 3: return receiverId;
    case 4: return type;
    case 5: return uri;
    case 6: return title;
    case 7: return data;
    case 8: return processMethod;
    case 9: return processed;
    case 10: return read;
    case 11: return importance;
    case 12: return body;
    case 13: return bodyHtml;
    case 14: return titleHtml;
    case 15: return objectId;
    case 16: return cDateTime;
    case 17: return lastModified;
    case 18: return guid;
    case 19: return action;
    case 20: return push;
    case 21: return scene;
    case 22: return imageUrl;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.Long)value$; break;
    case 1: appId = (java.lang.CharSequence)value$; break;
    case 2: senderId = (java.lang.CharSequence)value$; break;
    case 3: receiverId = (java.lang.CharSequence)value$; break;
    case 4: type = (java.lang.CharSequence)value$; break;
    case 5: uri = (java.lang.CharSequence)value$; break;
    case 6: title = (java.lang.CharSequence)value$; break;
    case 7: data = (java.lang.CharSequence)value$; break;
    case 8: processMethod = (java.lang.Integer)value$; break;
    case 9: processed = (java.lang.Boolean)value$; break;
    case 10: read = (java.lang.Boolean)value$; break;
    case 11: importance = (java.lang.Integer)value$; break;
    case 12: body = (java.lang.CharSequence)value$; break;
    case 13: bodyHtml = (java.lang.CharSequence)value$; break;
    case 14: titleHtml = (java.lang.CharSequence)value$; break;
    case 15: objectId = (java.lang.CharSequence)value$; break;
    case 16: cDateTime = (java.lang.Long)value$; break;
    case 17: lastModified = (java.lang.Long)value$; break;
    case 18: guid = (java.lang.CharSequence)value$; break;
    case 19: action = (java.lang.CharSequence)value$; break;
    case 20: push = (java.lang.Boolean)value$; break;
    case 21: scene = (java.lang.CharSequence)value$; break;
    case 22: imageUrl = (java.lang.CharSequence)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
