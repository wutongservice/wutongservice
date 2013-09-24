
CREATE TABLE IF NOT EXISTS privacy (
  `user` bigint NOT NULL,
  `res` varchar(64) NOT NULL,
  `scope` tinyint NOT NULL,
  `id` varchar(64) NOT NULL,
  `allow` tinyint NOT NULL,

  -- indexes
  PRIMARY KEY (`user`, `res`, `scope`, `id`),
  INDEX `user` (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;