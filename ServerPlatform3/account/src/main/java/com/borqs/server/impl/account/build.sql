
CREATE TABLE IF NOT EXISTS `user` (
  user_id bigint NOT NULL AUTO_INCREMENT,
  password varchar(32) NOT NULL,
  created_time bigint NOT NULL,
  destroyed_time bigint DEFAULT 0,
  status varchar(256) NOT NULL DEFAULT '',
  status_updated_time bigint NOT NULl DEFAULT 0,


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