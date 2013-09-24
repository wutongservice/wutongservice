

CREATE TABLE IF NOT EXISTS privacy  (
    user bigint NOT NULL,  
    resource varchar(255) NOT NULL,
    auths varchar(4096) NOT NULL,
    PRIMARY KEY (user, resource)
) ENGINE InnoDB DEFAULT CHARSET utf8;