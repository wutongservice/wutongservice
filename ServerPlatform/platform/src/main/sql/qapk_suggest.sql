
CREATE TABLE IF NOT EXISTS qapk_suggest  (
  sub_id bigint NOT NULL,               -- 编号 专题从2000开始，专区从1000开始
  sub_name varchar(255) NOT NULL,  --  专题、专区名称
  policy varchar(4096) NOT NULL,             -- 策略内容
  created_time bigint NOT NULL,         -- 创建时间
  destroyed_time bigint NOT NULL,              -- 删除时间

  PRIMARY KEY (sub_id),
  INDEX(sub_name),
  INDEX(policy)
) ENGINE InnoDB DEFAULT CHARSET utf8;