<?php
require_once APPLICATION_PATH."/modules/service/models/Notifications.php";
require_once APPLICATION_PATH."/modules/service/models/NotificationsResult.php";
require_once APPLICATION_PATH."/modules/service/models/Information.php";

class NotificationsTest extends PHPUnit_Framework_TestCase
{
	private $client;
	public static $MY_TICKET = "Y2h1bnJvbmcubGl1QGJvcnFzLmNvbV8xMzMxNzk1NDgzMzI5XzM0MzM%3D";
	
    public function testListAll()
    {
    	$status = 0;
    	$from = 0;
    	$size = 10;
    	
        $informations = $this->client->listAll(NotificationsTest::$MY_TICKET, $status, $from, $size);
        $this->assertTrue(isset($informations));
        $this->assertType("InformationList", $informations, "return result type is not correct!\n");
        $this->assertTrue(isset($informations->informations));
        $this->assertTrue(!empty($informations->informations), "return result is empty!\n");
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));
        echo $content;
    }

    public function testListbyid()
    {
    	$status = 0;
    	$mid = 0;
    	$count = 10;
    	
        $informations = $this->client->listById(NotificationsTest::$MY_TICKET, $status, $mid, $count);
        $this->assertTrue(isset($informations));
        $this->assertType("InformationList", $informations, "return result type is not correct!\n");
        $this->assertTrue(isset($informations->informations));
        $this->assertTrue(!empty($informations->informations), "return result is empty!\n");
        
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));
        echo $content;
    }

    public function testListbytime()
    {
    	$status = 0;
    	$from = 1000;
    	$count = 10;
    	
        $informations = $this->client->listbytime(NotificationsTest::$MY_TICKET, $status, $from, $count);
        $this->assertTrue(isset($informations));
        $this->assertType("InformationList", $informations, "return result type is not correct!\n");
        $this->assertTrue(isset($informations->informations));
        $this->assertTrue(!empty($informations->informations), "return result is empty!\n");
        
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));
        echo $content;
    }
    
    public function testListbytime2()
    {
    	echo "testListbytime2\n";
    	
    	$status = 0;
    	$from = time()-60*3600;
    	$count = 10;
    	
        $informations = $this->client->listbytime(NotificationsTest::$MY_TICKET, $status, $from, $count);
        $this->assertTrue(isset($informations));
        $this->assertType("InformationList", $informations, "return result type is not correct!\n");
        $this->assertTrue(isset($informations->informations));
        $this->assertTrue(!empty($informations->informations), "return result is empty!\n");
        
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));
        echo $content;
    }

    public function testTop()
    {
    	$status = 0;
    	$topn = 10;

        $informations = $this->client->top(NotificationsTest::$MY_TICKET, $status, $topn);
        $this->assertTrue(isset($informations));
        $this->assertType("InformationList", $informations, "return result type is not correct!\n");
        $this->assertTrue(isset($informations->informations));
        $this->assertTrue(!empty($informations->informations), "return result is empty!\n");
        
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));
        echo $content;
    }

    public function testCount()
    {
    	$status = 0;
    	
        $result = $this->client->count(NotificationsTest::$MY_TICKET, $status);
        $this->assertTrue(isset($result));
        $this->assertType("CountResponse", $result, "testCount return result type is not correct!\n");
        $this->assertTrue($result->count>0);
        
        $content = Zend_Json_Encoder::encode($result);
        echo $content;
    }

    public function testSend()
    {
    	$msg = new Information();
    	$msg->action = "testSend.action";
    	$msg->appId = "testSend.appId";
    	$msg->body = "testSend.body";
    	$msg->bodyHtml = "<div>testSend.bodyHtml</div>";
    	$msg->data = "testSend.data";
    	$msg->date = time();
    	$msg->guid = "testSend.guid";
    	$msg->importance = 10;
    	$msg->lastModified = $msg->date;
    	$msg->objectId = "testSend.objectId";
    	$msg->processed = 0;
    	$msg->processMethod = 30;
    	$msg->read = 0;
    	$msg->receiverId = "10208";
    	$msg->senderId = "10208";
    	$msg->title = "testSend.title";
    	$msg->titleHtml = "<div>testSend.titleHtml</div>";
    	$msg->type = "testSend.type";
    	$msg->uri = "http://";
   	
    	$result = $this->client->send(NotificationsTest::$MY_TICKET, $msg);
    	$this->assertTrue(isset($result));
    	$this->assertType("StateResult", $result, "return result type is not correct!\n");
    	$this->assertEquals("success", $result->status);
    	$this->assertTrue(isset($result->mid));
    	
    	$content = Zend_Json_Encoder::encode($result);
    	echo "\ntestSend result is : ". $content;
    }

    public function testDone()
    {
    	$msg = new Information();
    	$msg->action = "testDone.action";
    	$msg->appId = "testDone.appId";
    	$msg->body = "testDone.body";
    	$msg->bodyHtml = "<div>testDone.bodyHtml</div>";
    	$msg->data = "testDone.data";
    	$msg->date = time();
    	$msg->guid = "testDone.guid";
    	$msg->importance = 10;
    	$msg->lastModified = $msg->date;
    	$msg->objectId = "testDone.objectId";
    	$msg->processed = 0;
    	$msg->processMethod = 30;
    	$msg->read = 0;
    	$msg->receiverId = "10208";
    	$msg->senderId = "10208";
    	$msg->title = "testDone.title";
    	$msg->titleHtml = "<div>testDone.titleHtml</div>";
    	$msg->type = "testDone.type";
    	$msg->uri = "http://";
   	
    	$result = $this->client->send(NotificationsTest::$MY_TICKET, $msg);
    	$this->assertTrue(isset($result));
    	$this->assertType("StateResult", $result, "return result type is not correct!\n");
    	$this->assertEquals("success", $result->status);
    	$this->assertTrue(isset($result->mid));
    	
    	echo "markProcessed id is ".$result->mid;
   	
        $result = $this->client->markProcessed(NotificationsTest::$MY_TICKET, $result->mid);
        $this->assertTrue(isset($result));
        $this->assertType("StatusResponse", $result, "return result type is not correct!\n");
        $this->assertEquals("success", $result->status);

        $content = Zend_Json_Encoder::encode($result);
        echo $content;
    }

    public function testRead()
    {
    	$msg = new Information();
    	$msg->action = "testRead.action";
    	$msg->appId = "testRead.appId";
    	$msg->body = "testRead.body";
    	$msg->bodyHtml = "<div>testRead.bodyHtml</div>";
    	$msg->data = "testRead.data";
    	$msg->date = time();
    	$msg->guid = "testRead.guid";
    	$msg->importance = 10;
    	$msg->lastModified = $msg->date;
    	$msg->objectId = "testRead.objectId";
    	$msg->processed = 0;
    	$msg->processMethod = 30;
    	$msg->read = 0;
    	$msg->receiverId = "10208";
    	$msg->senderId = "10208";
    	$msg->title = "testRead.title";
    	$msg->titleHtml = "<div>testRead.titleHtml</div>";
    	$msg->type = "testRead.type";
    	$msg->uri = "http://";
   	
    	$result = $this->client->send(NotificationsTest::$MY_TICKET, $msg);
    	$this->assertTrue(isset($result));
    	$this->assertType("StateResult", $result, "return result type is not correct!\n");
    	$this->assertEquals("success", $result->status);
    	$this->assertTrue(isset($result->mid));
    	
    	echo "markRead id is ".$result->mid;
        
        $result = $this->client->markRead(NotificationsTest::$MY_TICKET, $result->mid);
        $this->assertTrue(isset($result));
        $this->assertType("StatusResponse", $result, "return result type is not correct!\n");
        $this->assertEquals("success", $result->status);
        
        $content = Zend_Json_Encoder::encode($result);
        echo $content;
    }
    
    public function setUp()
    {
    	/* Setup Routine */
    	$this->client = new service_Model_Notifications();
    	$this->testSend();
    }
    
    public function tearDown()
    {
    	/* Tear Down Routine */
    	$this->client->close();
    }
}