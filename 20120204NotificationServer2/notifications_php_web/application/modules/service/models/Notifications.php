<?php
require_once 'TicketManager.php';
require_once 'InformationList.php';
require_once 'CountResponse.php';
require_once 'StatusResponse.php';
require_once 'SendStatusResponse.php';
require_once 'NotificationConfig.php';

if (!isset($GEN_DIR)) {
    $GEN_DIR = 'gen-php';
}
if (!isset($MODE)) {
    $MODE = 'normal';
}

$GLOBALS['MODE'] = $MODE;

/** Set the Thrift root */
$GLOBALS['THRIFT_ROOT'] = 'thrift';

/** Include the Thrift base */
require_once $GLOBALS['THRIFT_ROOT'] . '/Thrift.php';

/** Include the binary protocol */
require_once $GLOBALS['THRIFT_ROOT'] . '/protocol/TBinaryProtocol.php';

/** Include the socket layer */
require_once $GLOBALS['THRIFT_ROOT'] . '/transport/TSocketPool.php';

/** Include the socket layer */
require_once $GLOBALS['THRIFT_ROOT'] . '/transport/TFramedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'] . '/transport/TBufferedTransport.php';

/** Include the generated code */
require_once $GEN_DIR . '/INotificationsThriftService/INotificationsThriftService.php';
//require_once $GEN_DIR.'/INotificationsThriftService/INotficationsThriftService_types.php';

class service_Model_Notifications
{
    private $logger;

    private $service;
    private $host = 'localhost';
    private $port = 8084;

    private $transport;
    private $client;

    public function service_Model_Notifications()
    {
        $this->logger = Zend_Registry::get('logger');

        $this->host = NotificationConfig::getThriftHost();
        $this->port = NotificationConfig::getThriftPort();

        $hosts = array($this->host);

        $timeout = 60;
        $socket = new TSocket($this->host, $this->port);
        $socket = new TSocketPool($hosts, $this->port);
        $socket->setDebug(TRUE);
        $socket->setRecvTimeout($timeout * 1000);

        if ($GLOBALS['MODE'] == 'inline') {
            $this->transport = $socket;
            $this->client = new INotificationsThriftServiceClient($this->transport);
        } else if ($GLOBALS['MODE'] == 'framed') {
            $framedSocket = new TFramedTransport($socket);
            $this->transport = $framedSocket;
            $protocol = new TBinaryProtocol($this->transport);
            $this->client = new INotificationsThriftServiceClient($protocol);
        } else {
            $bufferedSocket = new TBufferedTransport($socket, 1024, 1024);
            $this->transport = $bufferedSocket;
            $protocol = new TBinaryProtocol($this->transport);
            $this->client = new INotificationsThriftServiceClient($protocol);
        }

        $this->logger->info("start to open Thrift transport.");
        $this->transport->open();
        $this->logger->info("opening thrift transport is end!");
    }

    public function listAll($ticket, $status, $from = 0, $size = 20)
    {
        $informations = $this->listAllOfApp(0, $ticket, $status, $from, $size);

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    public function listAllOfApp($appId, $ticket, $status, $from = 0, $size = 20)
    {
        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("listAllOfApp -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {

                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $from) {
                    $from = 0;
                }

                // default to read twenty items
                if (null == $size) {
                    $size = 20;
                }

                $this->logger->info("listAllOfApp -> request parameter is status=" . $status . ",from=" . $from . ",size=" . $size . ",ticket=" . $ticket);
                $result = $this->client->listAllOfApp($appId, $receiverId, $status, $from, $size);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("listAllOfApp -> failed to list all informations! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    // list by id
    public function listbyid($ticket, $status, $mid, $count)
    {
        $informations = $this->listOfAppById(0, $ticket, $status, $mid, $count);

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    public function listOfAppById($appId, $ticket, $status, $mid = 0, $count = 25)
    {
        $total = $this->countOfApp($appId, $ticket, $status);

        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("listOfAppById -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {

                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $mid) {
                    $mid = 0;
                }

                // default start item is from zero position
                if (null == $count) {
                    $count = 0;
                }

                $this->logger->info("listOfAppById -> request parameter is status=" . $status . ",mid=" . $mid . ",count=" . $count);

                $result = $this->client->listOfAppById($appId, $receiverId, $status, $mid, $count);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("listOfAppById -> failed to list informations by ID! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        $informations->total = $total->count;

        return $informations;
    }

    // list by time
    public function listbytime($ticket, $status, $from, $count)
    {
        $informations = $this->listOfAppByTime(0, $ticket, $status, $from, $count);
        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    public function listOfAppByTime($appId, $ticket, $status, $from, $count)
    {
        $total = $this->countOfApp($appId, $ticket, $status);

        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("listOfAppByTime -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $from) {
                    $from = 0;
                }

                // default start item is from zero position
                if (null == $count) {
                    $count = 0;
                }

                $this->logger->info("listOfAppByTime -> request parameter is status=" . $status . ",from=" . $from . ",count=" . $count);

                $result = $this->client->listOfAppByTime($appId, $receiverId, $status, $from, $count);
                //$result = $this->client->userListByTime($appId, $receiverId, $status, $from, $count);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("listOfAppByTime -> failed to list informations by time! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        $informations->total = $total->count;

        return $informations;
    }

    public function userReadListByTime($appId, $ticket, $status, $type, $read, $scene, $from, $count)
    {
        $total = $this->countOfApp($appId, $ticket, $status);

        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("listOfAppByTime -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $from) {
                    $from = 0;
                }

                // default start item is from zero position
                if (null == $count) {
                    $count = 0;
                }

                $this->logger->info("userReadListByTime -> request parameter is status=" . $status . "type=" . $type . "read=" . $read . ",from=" . $from . ",count=" . $count);

                //$result = $this->client->listOfAppByTime($appId, $receiverId, $status, $from, $count);
                $result = $this->client->userReadListByTime($receiverId, $status, $type, $read, $scene, $from, $count);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("listOfAppByTime -> failed to list informations by time! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        $informations->total = $total->count;

        return $informations;
    }

    public function userListByTime($appId, $ticket, $status, $type, $scene, $from, $count)
    {
        $total = $this->countOfApp($appId, $ticket, $status);

        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("listOfAppByTime -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $from) {
                    $from = 0;
                }

                // default start item is from zero position
                if (null == $count) {
                    $count = 0;
                }

                $this->logger->info("listOfAppByTime -> request parameter is status=" . $status . ",from=" . $from . ",type=" . $type . ",count=" . $count . ",scene" . $scene);


                //$result = $this->client->listOfAppByTime($appId, $receiverId, $status, $from, $count);
                $result = $this->client->userListByTime($receiverId, $status, $type, $scene, $from, $count);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("listOfAppByTime -> failed to list informations by time! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        $informations->total = $total->count;

        return $informations;
    }

    // top n
    public function top($ticket, $status, $topn)
    {
        $informations = $this->topOfApp(0, $ticket, $status, $topn);

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    public function userTop($appId, $ticket, $type, $status,$scene, $topn)
    {
        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("userTop -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $topn) {
                    $topn = 5;
                }

                $this->logger->info("userTop -> request parameter is status=" . $status . ",topn=" . $topn);

                $result = $this->client->userTop($appId, $receiverId, $status, $type,$scene, $topn);

                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("userTop -> failed to top informations! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    public function topOfApp($appId, $ticket, $status, $topn)
    {
        $informations = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("topOfApp -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                // default start item is from zero position
                if (null == $topn) {
                    $topn = 5;
                }

                $this->logger->info("topOfApp -> request parameter is status=" . $status . ",topn=" . $topn);

                $result = $this->client->topOfApp($appId, $receiverId, $status, $topn);
                $informations = new InformationList($result);
            }
        } catch (Exception $e) {
            $this->logger->err("topOfApp -> failed to top informations! " . $e->getMessage());
            throw $e;
        }

        if (null == $informations) {
            $informations = new InformationList(null);
        }

        return $informations;
    }

    // count
    public function count($ticket, $status)
    {
        return $this->countOfApp(0, $ticket, $status);
    }

    public function countOfApp($appId, $ticket, $status)
    {
        $result = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("countOfApp -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                // default to read unprocessed items
                if (null == $status || "" == $status) {
                    $status = "0";
                }

                $this->logger->info("countOfApp -> The count operation request parameter is status=" . $status . ", receiverId=" . $receiverId . ", appId=" . $appId);

                $count = $this->client->countOfApp($appId, $receiverId, $status);
                $result = new CountResponse();
                $result->count = $count;
            }
        } catch (Exception $e) {
            $this->logger->err("countOfApp -> failed to count informations! " . $e->getMessage());
            throw $e;
        }

        return $result;
    }

    public function send($ticket, $msgObj)
    {
        // save to database
        $result = new SendStatusResponse();
        try {
            if (null == $msgObj->senderId || "" == $msgObj->senderId) {
                throw new Exception("SenderID can not be null or blank!");
            }
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("send -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                $time = microtime(true);

                $info = new Info();
                if (isset($msgObj->action)) {
                    $info->action = $msgObj->action;
                }
                if (isset($msgObj->appId)) {
                    $info->appId = $msgObj->appId;
                }
                if (isset($msgObj->body)) {
                    $info->body = $msgObj->body;
                }
                if (isset($msgObj->bodyHtml)) {
                    $info->bodyHtml = $msgObj->bodyHtml;
                }
                $info->cDateTime = $time;
                if (isset($msgObj->data)) {
                    $info->data = $msgObj->data;
                }
                if (isset($msgObj->guid)) {
                    $info->guid = $msgObj->guid;
                }
// 				$info->id = $msgObj->id;
                if (isset($msgObj->importance)) {
                    $info->importance = $msgObj->importance;
                }
                $info->lastModified = $time;
                if (isset($msgObj->objectId)) {
                    $info->objectId = $msgObj->objectId;
                }
                if (isset($msgObj->processed)) {
                    $info->processed = $msgObj->processed;
                }
                if (isset($msgObj->processMethod)) {
                    $info->processMethod = $msgObj->processMethod;
                }
                if (isset($msgObj->read)) {
                    $info->read = $msgObj->read;
                }
                if (isset($msgObj->receiverId)) {
                    $info->receiverId = $msgObj->receiverId;
                }
                if (isset($msgObj->senderId)) {
                    $info->senderId = $msgObj->senderId;
                }
                if (isset($msgObj->title)) {
                    $info->title = $msgObj->title;
                }
                if (isset($msgObj->titleHtml)) {
                    $info->titleHtml = $msgObj->titleHtml;
                }
                if (isset($msgObj->type)) {
                    $info->type = $msgObj->type;
                }
                if (isset($msgObj->uri)) {
                    $info->uri = $msgObj->uri;
                }
                if (isset($msgObj->push)) {
                    $info->push = $msgObj->push;
                }

                $this->logger->info("send -> send information is " . $msgObj);

                $result = $this->client->sendInf($info);
                $result->status = "success";
                $result->mid = $result->mid;
            }
        } catch (Exception $e) {
            $this->logger->err("send -> failed to send information! " . $e->getMessage());
            throw $e;
        }

        return $result;
    }

    public function markProcessed($ticket, $mid)
    {
        // save to database
        $result = new StatusResponse();
        try {
            $id = TicketManager::toBorqsId($ticket);
            $this->logger->info("markProcessed -> receiver's Borqs ID is " . $receiverId);
            $this->logger->info("markProcessed -> request parameter is mid=" . $mid);

            if (null != $id) {
                $msgIds = explode(",", $mid);
                foreach ($msgIds as $msgId) {
                    $this->logger->info("markProcessed -> start to mark processed information " . $msgId);
                    $this->client->markProcessed($msgId);
                }
                $result->status = "success";
            }
        } catch (Exception $e) {
            $this->logger->err("markProcessed -> failed to mark information to processed! " . $e->getMessage());
            throw $e;
        }

        return $result;
    }

    public function markRead($ticket, $mid)
    {
        // save to database
        $result = new StatusResponse();
        try {
            $id = TicketManager::toBorqsId($ticket);
            $this->logger->info("markRead -> receiver's Borqs ID is " . $receiverId);
            $this->logger->info("markRead -> request parameter is mid=" . $mid);

            if (null != $id) {
                $msgIds = explode(",", $mid);
                foreach ($msgIds as $msgId) {
                    $this->logger->info("markRead -> start to mark read information " . $msgId);
                    $this->client->markRead($msgId);
                }
                $result->status = "success";
            }
        } catch (Exception $e) {
            $this->logger->err("markRead -> failed to mark information to read! " . $e->getMessage());
            throw $e;
        }

        return $result;
    }

    # add by wangpeng at 2013-04-24 get count of unreaded
    public function getUnReadResultByScene($ticket, $scene)
    {
        $result = null;
        try {
            $receiverId = TicketManager::toBorqsId($ticket);
            $this->logger->info("getUnReadResultByScene -> receiver's Borqs ID is " . $receiverId);

            if (null != $receiverId) {
                $this->logger->info("getUnReadResultByScene -> request parameter is receiverId =" . $receiverId . "scene=" . $scene);
                $result = $this->client->getUnReadResultByScene($receiverId, $scene);
            }
        } catch (Exception $e) {
            $this->logger->err("getUnReadResultByScene -> failed to list informations by time! " . $e->getMessage());
            throw $e;
        }


        return $result;
    }

    public function close()
    {
        $this->transport->close();
    }
}

