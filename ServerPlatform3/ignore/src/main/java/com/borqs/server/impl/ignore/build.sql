CREATE TABLE IF NOT EXISTS `ignore` (
  `ignore_id` BIGINT(20) NOT NULL,
  `target_id` VARCHAR(64) NOT NULL DEFAULT '',
  `target_type` BIGINT(20) DEFAULT '0',
  `created_time` BIGINT(20) NOT NULL,
  `user_id` BIGINT(20) NOT NULL,
  `destroyed_time` BIGINT(20) DEFAULT NULL,
  `feature` BIGINT(20) NOT NULL,
  PRIMARY KEY (`ignore_id`),
  KEY `createdTime` (`created_time`)
) ENGINE=INNODB DEFAULT CHARSET=utf8