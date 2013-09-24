

CREATE TABLE IF NOT EXISTS setting  (
  user bigint NOT NULL,                 
  setting_key varchar(64) NOT NULL,
  setting_value varchar(2048) DEFAULT '',

  PRIMARY KEY (user, setting_key)
) ENGINE InnoDB DEFAULT CHARSET utf8;