
CREATE TABLE IF NOT EXISTS stream  (
  post_id bigint NOT NULL,
  source bigint NOT NULL,
  created_time bigint NOT NULL,
  updated_time bigint DEFAULT 0,
  destroyed_time bigint DEFAULT 0,
  quote bigint DEFAULT 0,
  root bigint DEFAULT 0,
  mentions varchar(4096) DEFAULT '',
  app int NOT NULL,
  type int NOT NULL,
  message varchar(4096) NOT NULL,
  app_data varchar(4096) DEFAULT '{}',
  attachments varchar(8192) DEFAULT '[]',
  device varchar(128) DEFAULT '',
  can_comment tinyint NOT NULL DEFAULT 1,
  can_like tinyint NOT NULL DEFAULT 1,
  privince tinyint NOT NULL DEFAULT 0,
  target varchar(256) DEFAULT '',

  PRIMARY KEY (post_id),
  INDEX(source),
  INDEX(quote),
  INDEX(root)
) ENGINE InnoDB DEFAULT CHARSET utf8;