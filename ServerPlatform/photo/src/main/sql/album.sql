
CREATE TABLE IF NOT EXISTS album  (
  aid varchar(128) NOT NULL,    	-- 相册ID
  cover_pid varchar(128) default '',   	-- 封面图片ID
  user_id varchar(128) NOT NULL,   	-- 用户ID
  name varchar(32) NOT NULL, 		-- 相册名
  created bigint NOT NULL,         	-- 创建时间
  modified bigint default 0,         	-- 修改时间
  description varchar(1024) default '',-- 简述信息
  location varchar(255) default '',	-- 位置信息
  asize SMALLINT default 0,  		-- 图片总数
  visible varchar(255) NOT NULL,	-- private属性
  link varchar(255) default '',		-- reserved
  data1 varchar(255) default '',	-- reserved
  data2 varchar(255) default '',	-- reserved
  data3 varchar(255) default '',	-- reserved
  
  PRIMARY KEY (aid),
  INDEX(name),
  INDEX(user_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;
