
CREATE TABLE IF NOT EXISTS `user` (
  `user_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `password` VARCHAR(32) NOT NULL,
  `created_time` BIGINT(20) NOT NULL,
  `destroyed_time` BIGINT(20) DEFAULT '0',
  `login_email1` VARCHAR(64) DEFAULT NULL,
  `login_email2` VARCHAR(64) DEFAULT NULL,
  `login_email3` VARCHAR(64) DEFAULT NULL,
  `login_phone1` VARCHAR(32) DEFAULT NULL,
  `login_phone2` VARCHAR(32) DEFAULT NULL,
  `login_phone3` VARCHAR(32) DEFAULT NULL,
  PRIMARY KEY (`user_id`)


  -- indexes
  PRIMARY KEY (user_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;
ALTER TABLE `user` AUTO_INCREMENT = 10001;


CREATE TABLE IF NOT EXISTS user_property (
  `user` bigint NOT NULL,
  `key` smallint NOT NULL,
  `sub` tinyint NOT NULL,
  `index` smallint DEFAULT 0,
  updated_time bigint NOT NULL,
  `type` tinyint NOT NULL,
  `value` varchar(2048) NOT NULL,

  -- indexes
  PRIMARY KEY (`user`, `key`, `sub`, `index`),
  INDEX (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;