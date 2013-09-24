CREATE TABLE IF NOT EXISTS global_counter (
  key_ varchar(128) NOT NULL,
  count_ bigint NOT NULL,
   PRIMARY KEY (key_)
) ENGINE InnoDB DEFAULT CHARSET utf8;