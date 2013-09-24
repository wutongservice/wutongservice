
CREATE TABLE IF NOT EXISTS ticket (
  ticket varchar(128) NOT NULL,
  user bigint NOT NULL,
  created_time bigint NOT NULL,
  app int DEFAULT 0,


  -- indexes
  PRIMARY KEY (ticket)
) ENGINE InnoDB DEFAULT CHARSET utf8;