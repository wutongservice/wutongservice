
CREATE TABLE IF NOT EXISTS op_history (
  `oper_id` bigint NOT NULL,
  `user` bigint NOT NULL,
  `as_` VARCHAR(255) NOT NULL DEFAULT '',
  `action` tinyint NOT NULL,
  `flag` int NOT NULL DEFAULT 0,
  `targets` VARCHAR(16384),
  `info` VARCHAR(2048) NOT NULL DEFAULT '',

  -- indexes
  PRIMARY KEY (`oper_id`),
  INDEX (`user`),
  INDEX (`action`),
) ENGINE MyISAM DEFAULT CHARSET utf8;