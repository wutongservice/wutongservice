CREATE TABLE IF NOT EXISTS photo  (
  pid varchar(128) NOT NULL,    	-- 图片ID
  aid varchar(128) NOT NULL,    	-- 相册ID
  user_id varchar(128) NOT NULL,   	-- 用户ID
  src varchar(255) NOT NULL, 		-- 原始图片url
  src_big varchar(255) NOT NULL,	-- 大图url
  src_small varchar(255) NOT NULL,	-- 小图url
  caption varchar(255) NOT NULL,	-- 文件名
  created bigint NOT NULL ,   		-- 创建时间
  modified bigint default 0,      	-- 修改时间
  location varchar(255) default '',	-- 位置信息 
  post_id bigint default 0,		-- stream ID
  link varchar(255) default '',		-- reserved
  data1 varchar(255) default '',	-- reserved
  data2 varchar(255) default '',	-- reserved
  data3 varchar(255) default '',	-- reserved
  
  PRIMARY KEY (pid),
  INDEX(aid),
  INDEX(user_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;
