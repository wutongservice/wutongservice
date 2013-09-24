
CREATE TABLE IF NOT EXISTS qapk_history  (
  qapk_history_id bigint NOT NULL,      -- 随机数ID
  user bigint NOT NULL,                 -- 应用
  package varchar(255) NOT NULL,        -- 包名称
  version_code int NOT NULL,            -- version code
  architecture tinyint NOT NULL,       -- arch
  version_name varchar(255),            -- version name
  created_time bigint NOT NULL,         -- 创建时间
  action smallint NOT NULL,             -- 行为 install, uninstall, ...

  PRIMARY KEY (qapk_history_id),
  INDEX(user),
  INDEX(package),
  INDEX(version_code)
) ENGINE InnoDB DEFAULT CHARSET utf8;