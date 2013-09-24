CREATE TABLE IF NOT EXISTS contact0 (
  `owner` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` smallint NOT NULL,
  `content` varchar(255) NOT NULL,
  `reason` tinyint NOT NULL,
  `created_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`owner`, `type`, `content`, `reason`),
  INDEX owner(`owner`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS contact1 (
  `owner` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` smallint NOT NULL,
  `content` varchar(255) NOT NULL,
  `reason` tinyint NOT NULL,
  `created_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`owner`, `type`, `content`, `reason`),
  INDEX content(`content`)
) ENGINE InnoDB DEFAULT CHARSET utf8;