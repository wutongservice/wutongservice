
CREATE TABLE IF NOT EXISTS cibind (
  info varchar(128) NOT NULL,
  `user` bigint NOT NULL,
  `type` varchar(128) NOT NULL,
  created_time bigint NOT NULL,


  -- indexes
  PRIMARY KEY (info),
  INDEX `user` (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;