
CREATE TABLE IF NOT EXISTS circle (
  `user` bigint NOT NULL,
  `circle_id` tinyint NOT NULL,
  `name` varchar(64) DEFAULT '',
  `updated_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`user`, `circle_id`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS friend (
  `user` bigint NOT NULL,
  `type` tinyint NOT NULL,
  `friend` varchar(64) NOT NULL,
  `circle` tinyint NOT NULL,
  `updated_time` bigint NOT NULL,
  `reason` tinyint NOT NULL,


  -- indexes
  PRIMARY KEY (`user`, `type`, `friend`, `circle`),
  INDEX (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;

CREATE TABLE IF NOT EXISTS follower (
  `type` tinyint NOT NULL,
  `friend` varchar(64) NOT NULL,
  `follower` bigint NOT NULL,
  `circle` tinyint NOT NULL,
  `updated_time` bigint NOT NULL,
  `reason` tinyint NOT NULL,

  -- indexes
  PRIMARY KEY (`type`, `friend`, `follower`, `circle`),
  INDEX (`type`, `friend`)
) ENGINE InnoDB DEFAULT CHARSET utf8;


CREATE TABLE IF NOT EXISTS remark (
  `user` bigint NOT NULL,
  `type` tinyint NOT NULL,
  `friend` varchar(64) NOT NULL,
  `remark` varchar(64) DEFAULT '',
  `updated_time` bigint NOT NULL,

  -- indexes
  PRIMARY KEY (`user`, `type`, `friend`),
  INDEX (`user`)
) ENGINE InnoDB DEFAULT CHARSET utf8;
