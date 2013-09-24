
CREATE TABLE IF NOT EXISTS qapk  (
  package varchar(255) NOT NULL,        -- 包名称
  enabled tinyint DEFAULT 0,            -- 此应用是否审核通过1为审核通过，0为没有
  app_name varchar(255) NOT NULL,       -- 应用名称 
  version_code int NOT NULL,            -- version code
  version_name varchar(255) ,             -- version name
  min_sdk_version smallint DEFAULT 0,   -- 适用最小SDK版本
  target_sdk_version smallint DEFAULT 0, -- 目标SDK版本
  max_sdk_version smallint DEFAULT 0,   -- 适用最大SDK版本
  architecture tinyint DEFAULT 0,       -- 适用CPU架构
  created_time bigint NOT NULL,         -- 创建时间
  destroyed_time bigint DEFAULT 0,      -- 删除时间
  info_updated_time bigint DEFAULT 0,   -- 应用程序信息最后更新时间
  description varchar(4096) DEFAULT '', -- 应用描述
  recent_change varchar(4096) DEFAULT '', -- 此版本改变
  category smallint DEFAULT 0,           -- 类别
  sub_category smallint DEFAULT 0,       -- 子类别
  rating float DEFAULT 0,             -- 评分
  download_count bigint DEFAULT 0,      -- 下载数量
  install_count bigint DEFAULT 0,       -- 安装数量
  uninstall_count bigint DEFAULT 0,     -- 卸载数量
  favorite_count bigint DEFAULT 0,      -- 收藏数量
  upload_user bigint DEFAULT 0,         -- 上传此APK的用户
  screen_support varchar(1024),         -- 屏幕特性支持
  icon_url varchar(255) DEFAULT '',     -- 图标文件
  price float DEFAULT 0.0,              -- 价格
  borqs tinyint DEFAULT 0,              -- 是否为borqs开发
  developer varchar(128) DEFAULT '',           -- 开发者名称
  developer_email varchar(64) DEFAULT '',     -- 开发者email
  developer_phone varchar(64) DEFAULT '',     -- 开发者phone
  developer_website varchar(255) DEFAULT '',  -- 开发者网站
  market_url varchar(255) DEFAULT '', -- market URL
  other_urls varchar(512),             -- 其他数据库URL
  file_size int DEFAULT 0,              -- APK文件大小
  file_url varchar(255) DEFAULT '',     -- APK 相对URL
  file_md5 varchar(32) DEFAULT '',      -- 文件MD5
  tag varchar(255) DEFAULT '',                    -- 预留的自定义ID
  screenshots_urls varchar(1024) DEFAULT '', -- 截图相对 URL
  app_name_en varchar(255) DEFAULT '',       -- 英文应用名称 
  description_en varchar(4096) DEFAULT '', -- 英文应用描述
  recent_change_en varchar(4096) DEFAULT '', -- 英文此版本改变
  source tinyint DEFAULT 0, -- 截图描述来源  0为应用汇，1为google-market，2为人工填入，3为机锋 

  PRIMARY KEY (package, version_code, architecture),
  INDEX(enabled),
  INDEX(min_sdk_version),
  INDEX(app_name),
  INDEX(info_updated_time),
  INDEX(category),
  INDEX(sub_category),
  INDEX(download_count),
  INDEX(install_count)
) ENGINE InnoDB DEFAULT CHARSET utf8;