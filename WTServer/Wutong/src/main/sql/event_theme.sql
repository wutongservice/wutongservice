
CREATE TABLE IF NOT EXISTS event_theme  (
    `id` bigint NOT NULL,
    creator bigint NOT NULL,
    updated_time bigint NOT NULL,
    `name` varchar(255) NOT NULL DEFAULT '',
    `image_url` varchar(255) NOT NULL DEFAULT '',
    PRIMARY KEY (id)
) ENGINE InnoDB DEFAULT CHARSET utf8;