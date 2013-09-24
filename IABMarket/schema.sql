
-- Android APP
create table apps (
  id varchar(200) primary key,                  -- 应用ID，为android app的 package
  created_at bigint not null,
  updated_at bigint not null,
  name text not null,                            -- 应用名称
  supported_languages varchar(512) not null default '' -- 支持发布者的语言
) engine innodb default charset utf8;

-- App 下的应用内产品分类
create table categories (
  app_id varchar(200) not null,                 -- 应用ID
  category varchar(32) not null,                -- 应用资源分类ID，例如theme, wallpaper
  created_at bigint not null,
  updated_at bigint not null,
  name text not null,                           -- 应用资源分类名称，例如“主题”，“壁纸”

  primary key (app_id, category),
  foreign key (app_id) references apps (id),
  index (app_id),
  index (category)
) engine innodb default charset utf8;

-- 应用内产品的价格标签
create table pricetags (                        -- 价格标签ID
  id varchar(255) primary key,
  created_at bigint not null,
  updated_at bigint not null,
  app_id varchar(200) not null,         -- 价格标签所属的APP ID
  category varchar(255) not null,       -- 价格标签所属的应用内资源分类ID
  google_iab_sku varchar(255) not null default '', -- 价格标签所关联的Google Play 的SKU
  price int not null,                          -- 友好可读的价格信息

  foreign key (app_id) references apps (id),
  foreign key (category) references categories (category),

  index(app_id),
  index(category)
) engine innodb default charset utf8;

-- 应用内产品
create table products (
  id varchar(64) primary key,                   -- 应用内产品ID
  created_at bigint not null,
  updated_at bigint not null,
  pricetag_id varchar(255) not null,            -- 产品的价格标签

  supported_languages varchar(512) not null default '', -- 产品支持的语言
  name text not null,                           -- 友好可读的产品名称
  type1 varchar(255) not null default '',
  type2 varchar(255) not null default '',
  status tinyint not null,                      -- 产品状态，例如是否激活等
  description mediumtext not null,              -- 产品的描述信息
  author_name varchar(255) not null,                  -- 作者名称
  author_email varchar(255) not null default '',      -- 作者email
  author_borqs_id varchar(255) not null default '',   -- 作者Borqs ID
  author_google_id varchar(255) not null default '',  -- 作者Google ID
  download_count bigint not null default 0,           -- 产品购买数量
  logo_image varchar(1024) not null default '{}',     -- 产品的logo image
  cover_image varchar(1024) not null default '{}',    -- 产品的cover image
  screenshot1_image varchar(1204) not null default '{}', -- 截图1
  screenshot2_image varchar(1204) not null default '{}', -- 截图2
  screenshot3_image varchar(1204) not null default '{}',
  screenshot4_image varchar(1204) not null default '{}',
  screenshot5_image varchar(1204) not null default '{}',

  foreign key (pricetag_id) references pricetags (id),
  index(pricetag_id),
  index(author_borqs_id)
) engine innodb default charset utf8;

-- 应用内产品项，一个产品携带多个版本的产品项
create table product_versions (
  product_id varchar(64) not null,              -- 产品项所属的产品ID
  version varchar(128) not null,                -- 产品项的版本名称（必须为"1.2.14"这种格式）
  created_at bigint not null,
  updated_at bigint not null,

  support_mod varchar(64) not null default '*',         -- 值为'HD'或'PHONE'或'*'
  status tinyint not null,                      -- 产品项的状态，例如是否激活等
  update_change mediumtext not null,            -- 此版本产品项的更新信息
  min_app_vc int not null default 0,            -- 此版本产品项所对应的app最低version code
  max_app_vc int not null default 2147483647,   -- 此版本产品项所对应的app最高version code
  action varchar(100) not null,                 -- 购买此产品后的行为，例如"download"
  action_url varchar(768) not null,             -- 购买此产品后访问的url，例如theme的下载链接
  file_size bigint not null,                    -- 如果action为"download"，那么描述下载文件大小，否则为0

  primary key (product_id, version),
  foreign key (product_id) references products (id),
  index(product_id)
) engine innodb default charset utf8;

-- 订单
create table orders (
  id varchar(255) primary key,                            -- 订单ID
  created_at bigint not null,                             -- 订单时间

  product_id varchar(64) not null,                        -- 购买的产品ID
  product_version varchar(128) not null,                  -- 产品的版本
  app_id varchar(200) not null,                   -- 购买产品所属的APP ID
  category varchar(255) not null,                 -- 价格标签所属的应用内资源分类ID

  purchaser_device_id varchar(255) not null default '',   -- 购买者设备ID
  purchaser_borqs_id varchar(255) not null default '',    -- 购买者Borqs ID
  purchaser_google_id1 varchar(255) not null default '',  -- 购买者Google ID 1
  purchaser_google_id2 varchar(255) not null default '',  -- 购买者Google ID 2
  purchaser_google_id3 varchar(255) not null default '',

  google_iab_order_id varchar(255) not null default '',

  purchaser_locale varchar(32) not null,                  -- 购买者的地区，格式为"zh_CN"
  purchaser_ip varchar(32) not null,                      -- 购买者IP
  purchaser_user_agent varchar(255) not null,              -- 购买者客户端的User Agent

  index(created_at),
  index(product_id),
  index(product_id, app_id),
  index(product_id, purchaser_device_id),
  index(product_id, purchaser_borqs_id),
  index(product_id, purchaser_google_id1),
  index(product_id, purchaser_google_id2),
  index(product_id, purchaser_google_id3)
) engine innodb default charset utf8;

-- 操作日志
create table logs (
  id bigint not null primary key auto_increment,
  created_at bigint not null,                             -- 时间
  op_borqs_id varchar(255) not null,                      -- 产品发布者的Borqs ID
  op_ip varchar(32) not null default '',                  -- 产品发布者的IP
  op_user_agent varchar(255) not null default '',         -- 产品发布者浏览器User Agent
  action varchar(64) not null,                            -- 产品发布者的行为，例如"publish_product"
  object_id varchar(255) not null,                        -- 根据action不同所携带的各种ID信息
  index (created_at),
  index (op_borqs_id),
  index (object_id)
) engine innodb default charset utf8;




