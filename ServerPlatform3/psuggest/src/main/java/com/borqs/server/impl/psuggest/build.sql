CREATE TABLE IF NOT EXISTS psuggest (
  `user` bigint NOT NULL,
  `type` tinyint NOT NULL,
  `id` varchar(64) NOT NULL,
  `reason` smallint NOT NULL,
  `source` varchar(2048) NOT NULL,
  `status` tinyint NOT NULL,
  `created_time` bigint NOT NULL,
  `deal_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`user`, `type`, `id`, `reason`),
  INDEX `user` (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;