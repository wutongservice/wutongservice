<?php
class Information
{
	public $id;

	public $appId;
	public $senderId;
	public $receiverId;
	public $type;

	public $uri;
	public $title;
	public $data;

	public $processMethod = 1;
	public $processed = false;
	public $read = false;

	public $importance = 30;

	public $body;
	public $bodyHtml;
	public $titleHtml;
	// Object ID
	public $objectId;

	public $date;
	// last modified time
	public $lastModified;

	// the following have been deprecated.
	public $action;
	// attain only information if GUID isn't null
	public $guid;
}