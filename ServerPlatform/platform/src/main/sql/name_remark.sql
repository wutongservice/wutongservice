
CREATE TABLE IF NOT EXISTS name_remark (
    user bigint NOT NULL,
    friend bigint NOT NULL,
    remark varchar(32) NOT NULL,
    PRIMARY KEY (user, friend)
) ENGINE InnoDB DEFAULT CHARSET utf8;