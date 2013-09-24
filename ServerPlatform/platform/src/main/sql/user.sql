
CREATE TABLE IF NOT EXISTS user  (
    -- basic
	user_id bigint NOT NULL,
	password varchar(32) NOT NULL,
    login_email1 varchar(64) DEFAULT '',
    login_email2 varchar(64) DEFAULT '',
    login_email3 varchar(64) DEFAULT '',
    login_phone1 varchar(32) DEFAULT '',
    login_phone2 varchar(32) DEFAULT '',
    login_phone3 varchar(32) DEFAULT '',
    domain_name varchar(32) DEFAULT '',
    display_name varchar(32) DEFAULT '',
    created_time bigint NOT NULL,
    destroyed_time bigint DEFAULT 0,
    last_visited_time bigint DEFAULT 0,
    image_url varchar(512) DEFAULT '',
    small_image_url varchar(512) DEFAULT '',
    large_image_url varchar(512) DEFAULT '',
    basic_updated_time bigint DEFAULT 0,

    -- status
    status varchar(256) DEFAULT '',
    status_updated_time bigint DEFAULT 0,

    -- profile
    first_name varchar(32) DEFAULT '',
    middle_name varchar(32) DEFAULT '',
    last_name varchar(32) DEFAULT '',
    gender char(1) DEFAULT 'm',
    birthday varchar(32) DEFAULT '',
    timezone varchar(32) DEFAULT '',
    interests varchar(256) DEFAULT '',
    languages varchar(256) DEFAULT '[]',
    marriage char(1) DEFAULT 'n',
    religion varchar(32) DEFAULT '',
    about_me varchar(512) DEFAULT '',
    profile_updated_time bigint DEFAULT 0,

    -- work
    company varchar(64) DEFAULT '',
    department varchar(64) DEFAULT '',
    job_title varchar(32) DEFAULT '',
    office_address varchar(64) DEFAULT '',
    profession varchar(32) DEFAULT '',
    job_description varchar(256) DEFAULT '',
    business_updated_time bigint DEFAULT 0,

    -- contact info
    contact_info text,
    contact_info_updated_time bigint DEFAULT 0,

    -- family relation
    family text,

    -- work relation
    coworker text,

    -- addresses
    address text,
    address_updated_time bigint DEFAULT 0,

    -- work history
    work_history text,
    work_history_updated_time bigint DEFAULT 0,

    -- education history
    education_history text,
    education_history_updated_time bigint DEFAULT 0,

    -- misc
    miscellaneous text,

	-- indexes
	PRIMARY KEY (user_id),
	INDEX (login_email1),
	INDEX (login_email2),
	INDEX (login_email3),
	INDEX (login_phone1),
	INDEX (login_phone2),
	INDEX (login_phone3),
	INDEX (display_name)
) ENGINE InnoDB DEFAULT CHARSET utf8;