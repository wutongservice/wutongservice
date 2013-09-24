CREATE TABLE IF NOT EXISTS conversation0 (
  `type` tinyint NOT NULL,
  `id` varchar(64) NOT NULL,
  `reason` smallint NOT NULL,
  `user` bigint NOT NULL,
  `created_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`type`, `id`, `reason`, `user`),
  INDEX target(`type`, `id`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS conversation1 (
  `type` tinyint NOT NULL,
  `id` varchar(64) NOT NULL,
  `reason` smallint NOT NULL,
  `user` bigint NOT NULL,
  `created_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`type`, `id`, `reason`, `user`),
  INDEX (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;