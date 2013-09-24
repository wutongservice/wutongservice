

CREATE TABLE IF NOT EXISTS friend  (
    user bigint NOT NULL,
    friend bigint NOT NULL,
    circle smallint NOT NULL,
    created_time bigint DEFAULT 0,
    reason tinyint DEFAULT 0
    PRIMARY KEY (user, friend, circle)
) ENGINE InnoDB DEFAULT CHARSET utf8;