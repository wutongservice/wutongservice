<?php

class Bootstrap extends Zend_Application_Bootstrap_Bootstrap
{
 	protected function _initLogger() {
		$logfile = '/var/log/information/information.log';
		
		$writer = new Zend_Log_Writer_Stream($logfile, 'a');
	
		$format = '%timestamp% %priorityName%: %message%' . PHP_EOL;
		$formatter = new Zend_Log_Formatter_Simple($format);
		$writer->setFormatter($formatter);
	
		$logger = new Zend_Log($writer);
// 		$logger->registerErrorHandler();
		
		$logger->setTimestampFormat("Y-M-dd H:i:s");
	
		Zend_Registry::set('logger', $logger);
	}

//  protected function _initAppAutoload()
//  {
//  	$autoloader = new Zend_Application_Module_Autoloader(array(
//		'namespace' => 'Application',
//		'basePath'  => dirname(__FILE__),
//  	));
//  	return $autoloader;
//  }
}

