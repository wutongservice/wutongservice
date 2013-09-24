<?php
require_once 'NotificationConfig.php';

class TicketManager
{
	static public function ticket2ID($ticket)
	{
		$logger = Zend_Registry::get('logger');
		
		$configUrl = null;
		$configUrl = NotificationConfig::getWhoUrl();
		$logger->info("\nauth url is : ".$configUrl."?ticket=".$ticket);
		
		$client = new Zend_Http_Client();
		$client->setUri($configUrl."?ticket=".$ticket);
// 		$client->setParameterGet(array(
// 					"ticket"	=>	$ticket
// 				));
		$res = $client->request(Zend_Http_Client::GET);
		
		$body = $res->getBody();
		$logger->info($body);
		
		$resObj = Zend_Json::decode($body, Zend_Json::TYPE_OBJECT);
		$logger->info("BPC result is :".$resObj->result);
		
		return $resObj->result;
	}
	
	static public function toBorqsId($ticket)
	{
		$logger = Zend_Registry::get('logger');
		
		$redis = new Redis();
		$host = NotificationConfig::getRedisHost();
		$port = NotificationConfig::getRedisPort();
		
			$borqsId = null;
		try {
			$res = $redis->connect($host, $port);
			if($res) {
	// 			$redis->delete($ticket);
				if($redis->exists($ticket)) {
					$borqsId = $redis->get($ticket);
					if('0'==$borqsId) {
						$redis->del($ticket);
						$borqsId = TicketManager::ticket2ID($ticket);
						if('0'!=$borqsId) {
							$redis->set($ticket, $borqsId);
						}
					}
				} else {
					$borqsId = TicketManager::ticket2ID($ticket);
					if('0'!=$borqsId) {
						$redis->set($ticket, $borqsId);
					}
				}
			} else {
				$borqsId = TicketManager::ticket2ID($ticket);
			}
			$logger->info("ticket is ".$ticket.", Borqs ID is ".$borqsId);
			return $borqsId;
		} catch (Exception $e) {
			$logger->err("failed to get Borqs ID by ticket! ".$e->getMessage());
		}
		$redis->close();
	}
}
