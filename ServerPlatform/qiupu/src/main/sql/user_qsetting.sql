

CREATE TABLE IF NOT EXISTS user_qsetting  (
  user bigint NOT NULL,                 -- 用户
  setting_name smallint NOT NULL,        -- 设置名称
  setting_value varchar(2048) DEFAULT '',-- 设置值

  PRIMARY KEY (user, setting_name)
) ENGINE InnoDB DEFAULT CHARSET utf8;