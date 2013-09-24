
CREATE TABLE IF NOT EXISTS setting (
  `user` bigint NOT NULL,
  `key` varchar(128) NOT NULL,
  `value` varchar(255) NOT NULL,
  updated_time bigint NOT NULL,


  -- indexes
  PRIMARY KEY (`user`, `key`),
  INDEX `user` (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;