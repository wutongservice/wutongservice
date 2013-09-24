namespace java com.borqs.notifications.thrift
namespace py notifications
namespace php notifications

struct Info {
	// information ID, readonly
	1:optional i64 id;
	
	// application identity
	2:required string appId;
	// sender identity
	3:required string senderId;
	// receiver identity
	4:required string receiverId;
	// information type
	5:required string type;
	
	// URI about information
	6:optional string uri;
	// information title
	7:optional string title;
	// detail information
	8:optional string data;
	
	// process method(default is 1)
	// 1 -- start activity for result and result is OK
	// 2 -- click list item
	// 3 -- loaded from server
	9:optional i32 processMethod;
	
	// processed mark, readonly
	10:optional bool processed;
	// read or unread mark, readonly
	11:optional bool read;
	
	// importance(default is 30)
	// 50 -- most important
	// 40 -- more important
	// 30 -- normal important
	// 20 -- less important
	// 10 -- not important
	12:optional i32 importance;
	
	// be used to update information if 'guid' field isn't null
	13:optional string body;
	14:optional string bodyHtml;
	15:optional string titleHtml;
	// object ID
	16:optional string objectId;
	
	// create date time, readonly
	17:optional i64 date;		
	// last modify time, readonly
	18:optional i64 lastModified;
	
	// the following have been deprecated
	// for replace information
	19:optional string guid;
	// client action
	20:optional string action;
	
	// control whether push a message to Push server
	21:optional bool push;
}

/* returned result */
struct StateResult {
	1:optional string mid;
	2:optional string status;
}
	
service INotificationsThriftService {

	// send an information
	StateResult sendInf(1:required Info info);
	// batch send informations
	StateResult batchSendInf(1:required list<Info> infos);
	// mark information processed state
	StateResult markProcessed(1:required string mid);
	// mark information read state
	StateResult markRead(1:required string mid);
	// query informations by appid, type, object and receiver ID.
	list<Info> queryInfo(1:required string appId,
						 2:required string type,
						 3:required string receiverId,
						 4:required string objectId);
	// replace information by appId, type, object and receiver ID.
	StateResult replaceInf(1:required Info info);
	// batch replace information by appId, type, object and receiver ID.
	StateResult batchReplaceInf(1:required list<Info> infos);
	
	list<Info> listAll(1:required string receiverId, 
					 2:optional string status="0",
					 3:optional i64 from=0,
					 4:optional i32 size=20);
	list<Info> listAllOfApp(
					1:required string appId, 
					2:required string receiverId, 
					3:optional string status="0",
					4:optional i64 from=0,
					5:optional i32 size=20);
					 	
	list<Info> listById(1:required string receiverId,
						2:optional string status="0",
						3:optional i64 mid=0,
						4:optional i32 count=20);
	list<Info> listOfAppById(
						1:required string appId, 
						2:required string receiverId,
						3:optional string status="0",
						4:optional i64 mid=0,
						5:optional i32 count=20);
						
	list<Info> listByTime(1:required string receiverId,
						  2:optional string status="0",
						  3:optional i64 from=0,
						  4:optional i32 count=20);
	list<Info> listOfAppByTime(
						1:required string appId, 
						2:required string receiverId,
						3:optional string status="0",
						4:optional i64 from=0,
						5:optional i32 count=20);
						  
	list<Info> top(1:required string receiverId,
				   2:optional string status="0",
				   3:optional i32 topn=5);
	list<Info> topOfApp(
				1:required string appId, 
				2:required string receiverId,
				3:optional string status="0",
				4:optional i32 topn=5);

	i32 count(1:required string receiverId,
			  2:optional string status="0");
	i32 countOfApp(
			1:required string appId, 
			2:required string receiverId,
			3:optional string status="0");
	
	// send and information by JSON
	string send(1:required string message);
	// batch send informations by JSON
	string batchSend(1:required string messages);
	// mark information processed state by JSON
	string process(1:required string mid);	
	// query informations by appid, type, object and receiver ID.
	string query(1:required string appId,
				 2:required string type,
				 3:required string receiverId,
				 4:required string objectId);
	// replace information by appId, type, object and receiver ID.
	string replace(1:required string message);
	// batch replace information by appId, type, object and receiver ID.
	string batchReplace(1:required string messages);
}