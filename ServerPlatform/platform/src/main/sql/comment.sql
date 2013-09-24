
CREATE TABLE IF NOT EXISTS comment (
  comment_id bigint NOT NULL,
  target varchar(128) NOT NULL,
  created_time bigint NOT NULL,
  destroyed_time bigint DEFAULT 0,
  commenter bigint NOT NULL,
  commenter_name varchar(64) NOT NULL,
  message varchar(4096) NOT NULL,
  device varchar(128),
  can_like tinyint DEFAULT 1,

  PRIMARY KEY (comment_id),
  INDEX (target),
  INDEX (created_time)
) ENGINE InnoDB DEFAULT CHARSET utf8;