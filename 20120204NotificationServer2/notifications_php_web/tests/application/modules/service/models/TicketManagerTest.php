<?php
require_once APPLICATION_PATH."/modules/service/models/TicketManager.php";

class TicketManagerTest extends PHPUnit_Framework_TestCase
{
	public static $MY_TICKET = "Y2h1bnJvbmcubGl1QGJvcnFzLmNvbV8xMzMxNzk1NDgzMzI5XzM0MzM%3D";
	
	public function testTicketToID()
	{
		$myid = TicketManager::ticket2ID(TicketManagerTest::$MY_TICKET);
		$this->assertNotEquals($myid, 0);
		echo "\nBorqs ID is = ".$myid;
	}
	
	public function setUp()
	{
		/* Setup Routine */
	}
	
	public function tearDown()
	{
		/* Tear Down Routine */
	}
}