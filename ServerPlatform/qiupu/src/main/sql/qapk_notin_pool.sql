
CREATE TABLE IF NOT EXISTS qapk_notin_pool  (
  apk_id varchar(255) NOT NULL,      -- APKID
  created_time bigint NOT NULL,         -- 创建时间 

  PRIMARY KEY (apk_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;