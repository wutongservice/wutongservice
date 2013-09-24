CREATE TABLE `poll` (
  `id` bigint(20) NOT NULL,
  `source` bigint(20) NOT NULL,
  `target` varchar(5000) DEFAULT '',
  `title` varchar(4096) NOT NULL,
  `description` varchar(4096) DEFAULT '',
  `multi` tinyint(4) DEFAULT 1,
  `limit_` tinyint(4) DEFAULT 0,
  `privacy` tinyint(4) DEFAULT 0,
  `anonymous` tinyint(4) DEFAULT 0,
  `mode` tinyint(4) DEFAULT 0,
  `type` int(11)  NOT NULL,
  `attachments` varchar(8192) DEFAULT '[]',
  `created_time` bigint(20) DEFAULT '0',
  `start_time` bigint(20) DEFAULT '0',
  `end_time` bigint(20) DEFAULT '0',
  `updated_time` bigint(20) DEFAULT '0',
  `destroyed_time` bigint(20) DEFAULT '0',
  `can_add_items` TINYINT(4) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `poll_items` (
  `poll_id` bigint(20) NOT NULL,
  `item_id` bigint(20) NOT NULL,
  `type` int(11)  NOT NULL,
  `message` varchar(4096) DEFAULT '',
  `attachments` varchar(8192) DEFAULT '[]',
  `created_time` bigint(20) DEFAULT '0',
  `updated_time` bigint(20) DEFAULT '0',
  `destroyed_time` bigint(20) DEFAULT '0',
  `index_` int(11) DEFAULT 1,
  `source` bigint(20) DEFAULT 0,
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `poll_participants` (
   `poll_id` bigint(20) NOT NULL,
   `item_id` bigint(20) NOT NULL,
   `user`  bigint(20) NOT NULL,
   `weight` bigint(20) DEFAULT 1,
   `created_time` bigint(20) DEFAULT '0',
   PRIMARY KEY (`item_id`, `user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;