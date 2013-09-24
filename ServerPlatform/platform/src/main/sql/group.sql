CREATE TABLE IF NOT EXISTS `group_` (
  `id` BIGINT(20) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `member_limit` INT(11) NOT NULL,
  `is_stream_public` TINYINT(4) DEFAULT 1,
  `can_search` TINYINT(4) DEFAULT 1,
  `can_view_members` TINYINT(4) DEFAULT 1,
  `can_join` TINYINT(4) DEFAULT 1,
  `can_member_invite` TINYINT(4) DEFAULT 1,
  `can_member_approve` TINYINT(4) DEFAULT 1,
  `can_member_post` TINYINT(4) DEFAULT 1,
  `can_member_quit` TINYINT(4) DEFAULT 1,
  `need_invited_confirm` TINYINT(4) DEFAULT 1,
  `creator` BIGINT(20) NOT NULL,
  `label` VARCHAR(100) DEFAULT '其它',
  `created_time` BIGINT(20) DEFAULT 0,
  `updated_time` BIGINT(20) DEFAULT 0,
  `destroyed_time` BIGINT(20) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `group_property` (
  `group_id` bigint NOT NULL,
  `key_` varchar(100) NOT NULL,
  `value_` varchar(2048) DEFAULT '',
  -- indexes
  PRIMARY KEY (`group_id`, `key_`),
  INDEX (`group_id`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS `group_members` (
  `group_id` BIGINT(20) NOT NULL,
  `member` BIGINT(20) DEFAULT 0,
  `role` INT(11) NOT NULL,
  `recv_notif` TINYINT(4) DEFAULT 0,
  `notif_email` VARCHAR(64) DEFAULT '',
  `notif_phone` VARCHAR(64) DEFAULT '',
  `joined_time` BIGINT(20) DEFAULT 0,
  `updated_time` BIGINT(20) DEFAULT 0,
  `created_time` BIGINT(20) DEFAULT 0,
  `destroyed_time` BIGINT(20) DEFAULT 0,
  PRIMARY KEY (`group_id`, `member`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `group_pendings` (
  `group_id` BIGINT(20) NOT NULL,
  `user_id` BIGINT(20) DEFAULT 0,
  `display_name` VARCHAR(100) DEFAULT '',
  `identify` VARCHAR(64) DEFAULT '',
  `source` BIGINT(20) DEFAULT 0,
  `status` INT(11) NOT NULL,
  `created_time` BIGINT(20) DEFAULT 0,
  `updated_time` BIGINT(20) DEFAULT 0,
  `destroyed_time` BIGINT(20) DEFAULT 0,
  PRIMARY KEY (`group_id`, `user_id`, `identify`, `source`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;