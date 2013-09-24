<?php

class IndexController extends Zend_Controller_Action
{

    public function init()
    {
        /* Initialize action controller here */
    }

    public function indexAction()
    {
      // action body
    	//$content = "hello world! home!";
    	//$this->getResponse()->setBody($content);
    	$this->getFrontController()->setParam('noViewRenderer', true);
    }
}



