

CREATE TABLE IF NOT EXISTS history  (
  user bigint NOT NULL,                 
  action varchar(64) NOT NULL,
  acted_time bigint DEFAULT 0,

  PRIMARY KEY (user, action)
) ENGINE InnoDB DEFAULT CHARSET utf8;