

CREATE TABLE IF NOT EXISTS ticket (
  ticket varchar(128) NOT NULL,
  user bigint NOT NULL,
  app int NOT NULL,
  created_time bigint NOT NULL,
  `type_` TINYINT(4) DEFAULT 0,

  PRIMARY KEY (ticket),
  INDEX(user)
) ENGINE InnoDB DEFAULT CHARSET utf8;