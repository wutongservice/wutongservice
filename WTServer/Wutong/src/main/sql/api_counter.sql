CREATE TABLE IF NOT EXISTS api_counter  (
    api varchar(255) NOT NULL PRIMARY KEY,
    call_count BIGINT NOT NULL,
    call_elapsed INT NOT NULL,
    last_call_time BIGINT NOT NULL
) ENGINE InnoDB DEFAULT CHARSET utf8;