<?php
class NotificationConfig
{
	private static $config = null;
	
	public static function getInstance()
	{
		if(!isset($config)) {
			NotificationConfig::$config = new Zend_Config_Ini(APPLICATION_PATH."/configs/application.ini",
				APPLICATION_ENV);
		}
		return NotificationConfig::$config;	
	}
	
	/**
	 * read thrift service host
	 */
	public static function getThriftHost()
	{
		if(!isset(NotificationConfig::$config)) {
			NotificationConfig::getInstance();
		}
		return NotificationConfig::$config->thrift->host;
	}
	
	/**
	 * read thrift service port
	 */
	public static function getThriftPort()
	{
		if(!isset(NotificationConfig::$config)) {
			NotificationConfig::getInstance();
		}
		return NotificationConfig::$config->thrift->port;
	}
	
	/**
	 * read redis cache host
	 */
	public static function getRedisHost()
	{
		if(!isset(NotificationConfig::$config)) {
			NotificationConfig::getInstance();
		}
		return NotificationConfig::$config->redis->host;
	}
	
	/**
	 * read redis cache port
	 */
	public static function getRedisPort()
	{
		if(!isset(NotificationConfig::$config)) {
			NotificationConfig::getInstance();
		}
		return NotificationConfig::$config->redis->port;
	}
	
	/**
	 * read borqs account service auth URL
	 */
	public static function getWhoUrl()
	{
		if(!isset(NotificationConfig::$config)) {
			NotificationConfig::getInstance();
		}
		return NotificationConfig::$config->bpc->auth->url;
	}
}