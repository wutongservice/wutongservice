<?php
require_once APPLICATION_PATH . "/modules/service/models/Notifications.php";
require_once APPLICATION_PATH . "/modules/service/models/NotificationsResult.php";

class Service_InformationsController extends Zend_Controller_Action
{
    private $logger;
    private $client;

    public function init()
    {
        /* Initialize action controller here */
        $this->_helper->viewRenderer->setNoRender();
        $this->client = new service_Model_Notifications();

        $this->logger = Zend_Registry::get('logger');
    }

    public function indexAction()
    {
        //$content = "service informations index action!";
        $this->getResponse()->setBody($content);
    }

    // list action
    private function listJson()
    {
        $ticket = null;
        $status = null;
        $from = null;
        $size = null;
        $appId = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_GET["status"])) {
            $status = $_GET["status"];
        }
        if (isset($_GET["from"])) {
            $from = (float)$_GET["from"];
        }
        if (isset($_GET["size"])) {
            $size = $_GET["size"];
        }
        if (isset($_GET["appId"])) {
            $appId = $_GET["appId"];
        }

        if (null == $appId) {
            $informations = $this->client->listAll($ticket, $status, $from, $size);
        } else {
            $informations = $this->client->listAllOfApp($appId, $ticket, $status, $from, $size);
        }
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));

        return $content;
    }

    public function listJsonAction()
    {
        $content = $this->listJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function listJsonpAction()
    {
        $content = $this->listJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // listbyid action
    private function listbyidJson()
    {
        $ticket = null;
        $status = null;
        $mid = null;
        $count = null;
        $appId = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_GET["status"])) {
            $status = $_GET["status"];
        }
        if (isset($_GET["mid"])) {
            $mid = (float)$_GET["mid"];
        }
        if (isset($_GET["count"])) {
            $count = $_GET["count"];
        }
        if (isset($_GET["appId"])) {
            $appId = $_GET["appId"];
        }

        if (null == $appId) {
            $informations = $this->client->listById($ticket, $status, $mid, $count);
        } else {
            $informations = $this->client->listOfAppById($appId, $ticket, $status, $mid, $count);
        }
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));

        return $content;
    }

    public function listbyidJsonAction()
    {
        $content = $this->listbyidJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function listbyidJsonpAction()
    {
        $content = $this->listbyidJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // listbytime action
    private function listbytimeJson()
    {
        $ticket = null;
        $status = null;
        $from = null;
        $count = null;
        $appId = null;
        $type = null;
        $read = null;
        $scene = 0;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_GET["status"])) {
            $status = $_GET["status"];
        }
        if (isset($_GET["from"])) {
            $from = (float)$_GET["from"];
        }
        if (isset($_GET["count"])) {
            $count = $_GET["count"];
        }
        if (isset($_GET["appId"])) {
            $appId = $_GET["appId"];
        }
        if (isset($_GET["type"])) {
            $type = $_GET["type"];
        }
        if (isset($_GET["read"])) {
            $read = $_GET["read"];
        }
        if (isset($_GET["scene"])) {
            $scene = $_GET["scene"];
        }
        if (null == $appId) {
            //type 表示
            if ($type != null) {
                if ($read != null) {
                    $informations = $this->client->userReadListByTime(0, $ticket, $status, $type, $read, $scene, $from, $count);
                } else {
                    $informations = $this->client->userListByTime(0, $ticket, $status, $type, $scene, $from, $count);
                }
            } else {
                if ($read != null) {
                    $informations = $this->client->userReadListByTime(0, $ticket, $status, $type, $read, $scene, $from, $count);
                } else {
                    $informations = $this->client->listbytime($ticket, $status, $from, $count);
                }
            }
        } else {
            if ($type != null) {
                if ($read != null) {
                    $informations = $this->client->userReadListByTime($appId, $ticket, $status, $type, $read, $scene, $from, $count);
                } else {
                    $informations = $this->client->userListByTime($appId, $ticket, $status, $type, $scene, $from, $count);
                }

            } else {
                if ($read != null) {
                    $informations = $this->client->userReadListByTime(0, $ticket, $status, $type, $read, $scene, $from, $count);
                } else {
                    $informations = $this->client->listOfAppByTime($appId, $ticket, $status, $from, $count);
                }
            }
        }
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));

        return $content;
    }

    public function listbytimeJsonAction()
    {
        $content = $this->listbytimeJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function listbytimeJsonpAction()
    {
        $content = $this->listbytimeJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // top action
    private function topJson()
    {
        $ticket = null;
        $status = null;
        $topn = null;
        $appId = null;
        $type = null;
        $scene = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_GET["status"])) {
            $status = $_GET["status"];
        }
        if (isset($_GET["topn"])) {
            $topn = $_GET["topn"];
        }
        if (isset($_GET["appId"])) {
            $appId = $_GET["appId"];
        }
        if (isset($_GET["type"])) {
            $type = $_GET["type"];
        }
        if (isset($_GET["scene"])) {
            $scene = $_GET["scene"];
        }

        if (null == $appId) {
            if ($type != null) {
                $informations = $this->client->userTop($appId, $ticket, $type, $status,$scene, $topn);
            } else {
                $informations = $this->client->top($ticket, $status, $topn);
            }

        } else {
            if ($type != null) {
                $informations = $this->client->userTop($appId, $ticket, $type, $status,$scene, $topn);
            } else {
                $informations = $this->client->topOfApp($appId, $ticket, $status, $topn);
            }
        }
        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));

        return $content;
    }

    public function topJsonAction()
    {
        $content = $this->topJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function topJsonpAction()
    {
        $content = $this->topJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // count action
    private function countJson()
    {
        $ticket = null;
        $status = null;
        $appId = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_GET["status"])) {
            $status = $_GET["status"];
        }
        if (isset($_GET["appId"])) {
            $appId = $_GET["appId"];
        }

        if (null == $appId) {
            $result = $this->client->count($ticket, $status);
        } else {
            $result = $this->client->countOfApp($appId, $ticket, $status);
        }
        $content = Zend_Json_Encoder::encode($result);

        return $content;
    }

    public function countJsonAction()
    {
        $content = $this->countJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function countJsonpAction()
    {
        $content = $this->countJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // send action
    private function sendJson()
    {
        $ticket = null;
        $msg = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }
        if (isset($_POST["msg"])) {
            $msg = $_POST["msg"];
        }

        $objMsg = Zend_Json::decode($msg, Zend_Json::TYPE_OBJECT);

        $result = $this->client->send($ticket, $objMsg);
        $content = Zend_Json_Encoder::encode($result);

        return $content;
    }

    public function sendJsonAction()
    {
        $content = $this->sendJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function sendJsonpAction()
    {
        $content = $this->sendJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // done action
    private function doneJson()
    {
        $ticket = null;
        $mid = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        } else {
            $ticket = $_POST["ticket"];
        }

        if (isset($_GET["mid"])) {
            $mid = $_GET["mid"];
        }

        $result = $this->client->markProcessed($ticket, $mid);
        $content = Zend_Json_Encoder::encode($result);

        return $content;
    }

    public function doneJsonAction()
    {
        $content = $this->doneJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    // in order to be compatiable with the old API, /doneget.json
    public function donegetJsonAction()
    {
        $this->doneJsonAction();
    }

    public function doneJsonpAction()
    {
        $content = $this->doneJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }

    // in order to be compatiable with the old API, /doneget.jsonp
    public function donegetJsonpAction()
    {
        $this->doneJsonpAction();
    }

    // read action
    private function readJson()
    {
        $ticket = null;
        $mid = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        } else {
            $ticket = $_POST["ticket"];
        }

        if (isset($_GET["mid"])) {
            $mid = $_GET["mid"];
        }

        $result = $this->client->markRead($ticket, $mid);
        $content = Zend_Json_Encoder::encode($result);

        return $content;
    }

    public function readJsonAction()
    {
        $content = $this->readJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    // in order to be compatiable with the old API, /readget.json
    public function readgetJsonAction()
    {
        $this->readJsonAction();
    }

    public function readJsonpAction()
    {
        $content = $this->readJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }
    // add by wangpeng at 2013-04-25
    private function unReadBySceneJson()
    {
        $ticket = null;
        $scene = null;

        if (isset($_GET["ticket"])) {
            $ticket = $_GET["ticket"];
        }

        if (isset($_GET["scene"])) {
            $scene = $_GET["scene"];
        }

        $informations = $this->client->getUnReadResultByScene($ticket, $scene);

        $content = Zend_Json_Encoder::encode(new NotificationsResult($informations));

        return $content;
    }

    public function unreadbysceneJsonAction()
    {
        $content = $this->unReadBySceneJson();
        $this->getResponse()->setHeader('Content-type', 'application/json');
        $this->getResponse()->setBody($content);
    }

    public function unreadbysceneJsonpAction()
    {
        $content = $this->unReadBySceneJson();
        $this->getResponse()->setHeader('Content-type', 'application/javascript');
        $this->getResponse()->setBody($_GET["callback"] . "(" . $content . ")");
    }
    // in order to be compatiable with the old API, /readget.jsonp
    public function readgetJsonpAction()
    {
        $this->readJsonpAction();
    }

    function __destruct()
    {
        if (null != $this->client) {
            $this->client->close();
        }
    }
}

















