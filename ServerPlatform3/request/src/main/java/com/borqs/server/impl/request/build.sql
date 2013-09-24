CREATE TABLE IF NOT EXISTS request (
  `id` bigint NOT NULL,
  `from` bigint NOT NULL,
  `to` bigint NOT NULL,
  `app` int DEFAULT 0,
  `type` smallint NOT NULL,
  `message` varchar(1024) DEFAULT '',
  `data` varchar(4096) DEFAULT '',
  `status` tinyint DEFAULT 1,
  `created_time` bigint NOT NULL,
  `done_time` bigint DEFAULT 0,

  -- indexes
  PRIMARY KEY (`id`),
  INDEX (`to`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS request_index (
  `id` bigint NOT NULL,
  `from` bigint NOT NULL,
  `to` bigint NOT NULL,
  `type` smallint NOT NULL,
  `status` tinyint DEFAULT 1,

  -- indexes
  PRIMARY KEY (`id`),
  INDEX (`from`)
) ENGINE InnoDB DEFAULT CHARSET utf8;