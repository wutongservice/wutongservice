

CREATE TABLE IF NOT EXISTS ticket (
  ticket varchar(128) NOT NULL,
  user bigint NOT NULL,
  app int NOT NULL,
  created_time bigint NOT NULL,

  PRIMARY KEY (ticket),
  INDEX(user)
) ENGINE InnoDB DEFAULT CHARSET utf8;