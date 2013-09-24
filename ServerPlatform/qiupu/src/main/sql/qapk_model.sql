

CREATE TABLE IF NOT EXISTS qapk_model (
  package varchar(255) NOT NULL,    -- package
  version_name int NOT NULL,        -- version name
  compatibility tinyint NOT NULL,   -- 1为适用，0为不适用
  model varchar(255) NOT NULL,      -- model ID

  PRIMARY KEY (package, model)
) ENGINE InnoDB DEFAULT CHARSET utf8;