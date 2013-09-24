
CREATE TABLE IF NOT EXISTS circle  (
    user bigint NOT NULL,
    circle smallint NOT NULL,
    name varchar(64) DEFAULT '',
    created_time bigint NOT NULL,
    updated_time bigint DEFAULT 0,
    member_count smallint DEFAULT 0,
    PRIMARY KEY (user, circle)
) ENGINE InnoDB DEFAULT CHARSET utf8;