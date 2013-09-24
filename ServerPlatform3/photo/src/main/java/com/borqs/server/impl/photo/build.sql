CREATE TABLE IF EXISTS `album` (         --相册
  `album_id` bigint(20) NOT NULL ,  --相册ID
  `album_type` tinyint(11) NOT NULL DEFAULT '0', --相册类型 profile, mobile, wall, activity...
  `user_id` bigint(20) NOT NULL,   --用户ID
  `title` varchar(32) DEFAULT '',  --相册标题
  `summary` varchar(512) NOT NULL ,   --相册描述
  `cover_photo_id` bigint(20) DEFAULT '0' ,   --封面照片ID
  `privacy` tinyint(11) DEFAULT '0' ,   --隐私
  `can_upload` tinyint(11) DEFAULT '0' ,   --是否能上传
  `num_photos` tinyint(11) DEFAULT '0',   --相册照片数量
  `location` varchar(512) DEFAULT '',  --相册位置信息
  `html_page_url` varchar(256) DEFAULT '',    --相册html_page_url（生成规则待定）
  `thumbnail_url` varchar(32)  DEFAULT '',   --相册缩略图url 
  `bytes_used` int(4) DEFAULT '0',  --相册空间使用大小
  `created_time` bigint(20) NOT NULL,  --相册信息修改时间
  `updated_time` bigint(20) NOT NULL,  --相册发布时间
  `publish_time` bigint(20) NOT NULL,  --相册信息修改时间
  `photos_etag` varchar(512) DEFAULT '',   --
  `photos_dirty` tinyint(11) DEFAULT '0',   --
  PRIMARY KEY (`album_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF EXISTS `photos` (         --照片表
  `photo_id` bigint(20) NOT NULL ,  --照片ID
  `user_id` bigint(20) NOT NULL,   --用户ID
  `title` varchar(32) DEFAULT '',  --标题
  `thumbnail_url` varchar(256)  DEFAULT '',   --缩略图url 
  `summary` varchar(512) NOT NULL ,   --描述
  `keywords` varchar(256) DEFAULT '',   --关键字，用于检索
  `content_type` varchar(512) DEFAULT '',  --content类型
  `content_url` varchar(512) DEFAULT '',  --
  `html_page_url` varchar(512) DEFAULT '',    --html_page_url（生成规则待定）
  `size` bigint(20) DEFAULT '0',  --大小
  `height` int(4) NOT NULL,  --高度
  `width` int(4) DEFAULT '0',   --宽度
  `album_id` bigint(20) NOT NULL,  --相册ID
  `longitude` varchar(32) DEFAULT '',  --经度
  `latitude` varchar(32) DEFAULT '',  --维度
  `location` varchar(128) DEFAULT '',  --位置
  `tags` varchar(4096) DEFAULT '',  --圈人
  `fingerprint` varchar(512) DEFAULT '',  --
  `face_rectangles` varchar(512) DEFAULT '',  --
  `face_names` varchar(512) DEFAULT '',  --
  `face_ids` varchar(256) DEFAULT '',  --
  `exif_model` varchar(64) DEFAULT '',  --
  `exif_make`  varchar(64) DEFAULT '',  --
  `exif_focal_length` varchar(64) DEFAULT '',  --
  `created_time` bigint(20) NOT NULL DEFAULT '0',  --修改时间
  `updated_time` bigint(20) NOT NULL DEFAULT '0',  --修改时间
  `taken_time` bigint(20) NOT NULL DEFAULT '0',  --
  `published_time` bigint(20)  DEFAULT '0',  --发布时间
  `fingerprint_hash` bigint(20) NOT NULL DEFAULT '0',  --
  `display_index` bigint(20) DEFAULT '0',  --
  `exif_exposure` varchar(32) DEFAULT '',  --
  `exif_flash` varchar(32) DEFAULT '',  --  
  `rotation` tinyint(11) DEFAULT '0',  --
  `camera_sync` tinyint(11) DEFAULT '0',  --是否从相机同步来的照片
  `exif_iso` tinyint(11) DEFAULT '0',   --感光度
  `exif_fstop` tinyint(11) DEFAULT '0',   --光圈
  PRIMARY KEY (`photo_id`),
  KEY `album_id` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;