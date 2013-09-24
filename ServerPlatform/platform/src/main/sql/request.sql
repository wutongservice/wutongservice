
CREATE TABLE IF NOT EXISTS request  (
    request_id bigint NOT NULL,
    user bigint NOT NULL,
    source bigint NOT NULL,
    app int NOT NULL,
    type varchar(128) NOT NULL,
    created_time bigint NOT NULL,
    done_time bigint DEFAULT 0,
    status tinyint DEFAULT 0,
    message varchar(1024) DEFAULT '',
    options varchar(1024) DEFAULT '',


    PRIMARY KEY (request_id),
    INDEX(user),
    INDEX(source),
    INDEX(app),
    INDEX(type),
    INDEX(created_time),
    INDEX(status)
) ENGINE InnoDB DEFAULT CHARSET utf8;