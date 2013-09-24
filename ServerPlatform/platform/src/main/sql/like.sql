CREATE TABLE IF NOT EXISTS like_  (
  target varchar(128) NOT NULL,
  liker bigint NOT NULL,
  created_time bigint NOT NULL,

  PRIMARY KEY (target, liker)
) ENGINE InnoDB DEFAULT CHARSET utf8;