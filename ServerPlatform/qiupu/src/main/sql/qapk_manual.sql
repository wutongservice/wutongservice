
CREATE TABLE IF NOT EXISTS qapk_manual  (
  apk_id varchar(255) NOT NULL,      -- APKID
  type int NOT NULL,         -- 类型 

  PRIMARY KEY (apk_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;