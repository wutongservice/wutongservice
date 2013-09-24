
CREATE TABLE IF NOT EXISTS user_qapp  (
  user bigint NOT NULL, -- 用户ID
  package varchar(255) NOT NULL,     -- 应用包名
  reason int NULL,      -- 用户与此应用关联的原因
  privacy tinyint NULL, -- 用户是否将此应用隐私掉
  version_code int DEFAULT 0,
  architecture tinyint DEFAULT 1,

  PRIMARY KEY (user, package)
) ENGINE InnoDB DEFAULT CHARSET utf8; 