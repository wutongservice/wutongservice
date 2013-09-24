CREATE TABLE IF NOT EXISTS company (
  company_id BIGINT NOT NULL AUTO_INCREMENT,
  created_time BIGINT NOT NULL,
  updated_time BIGINT NOT NULL,
  destroyed_time BIGINT NOT NULL DEFAULT 0,
  email_domain1 VARCHAR(64) NOT NULL DEFAULT '',
  email_domain2 VARCHAR(64) NOT NULL DEFAULT '',
  email_domain3 VARCHAR(64) NOT NULL DEFAULT '',
  email_domain4 VARCHAR(64) NOT NULL DEFAULT '',
  `name` VARCHAR(255) NOT NULL DEFAULT '',
  `name_en` VARCHAR(255) NOT NULL DEFAULT '',
  `address` VARCHAR(255) NOT NULL DEFAULT '',
  `email` VARCHAR(255) NOT NULL DEFAULT '',
  `website` VARCHAR(255) NOT NULL DEFAULT '',
  `tel` VARCHAR(32) NOT NULL DEFAULT '',
  `fax` VARCHAR(32) NOT NULL DEFAULT '',
  `zip_code` VARCHAR(32) NOT NULL DEFAULT '',
  `small_logo_url` VARCHAR(255) NOT NULL DEFAULT '',
  `logo_url` VARCHAR(255) NOT NULL DEFAULT '',
  `large_logo_url` VARCHAR(255) NOT NULL DEFAULT '',
  `cover_url` VARCHAR(255) NOT NULL DEFAULT '',
  `small_cover_url` VARCHAR(255) NOT NULL DEFAULT '',
  `large_cover_url` VARCHAR(255) NOT NULL DEFAULT '',
  `description` MEDIUMTEXT NOT NULL,
  `description_en` MEDIUMTEXT NOT NULL,
  `department_id` BIGINT NOT NULL,
  `sk` VARCHAR(1024) NOT NULL DEFAULT '',

  PRIMARY KEY (`company_id`),
  UNIQUE INDEX (email_domain1),
  UNIQUE INDEX (email_domain2),
  UNIQUE INDEX (email_domain3),
  UNIQUE INDEX (email_domain4),
  INDEX (name),
  UNIQUE INDEX (department_id)
) ENGINE InnoDB DEFAULT CHARSET utf8;
ALTER TABLE company AUTO_INCREMENT=15000000001;

CREATE TABLE IF NOT EXISTS company_email_domain_blacklist (
  `email_domain` VARCHAR(64) NOT NULL PRIMARY KEY
);


CREATE TABLE IF NOT EXISTS employee_list  (
  company_id BIGINT NOT NULL,
  `employee_id` VARCHAR(64) NOT NULL DEFAULT '',
  `email` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL DEFAULT '',
  `tel` varchar(32) NOT NULL DEFAULT '',
  `mobile_tel` varchar(32) NOT NULL DEFAULT '',
  `department` varchar(128) NOT NULL DEFAULT '',
  `job_title` VARCHAR(128) NOT NULL DEFAULT '',
  `comment` varchar(4096) NOT NULL DEFAULT '',
  `sk` VARCHAR(1024) NOT NULL DEFAULT '',
  PRIMARY KEY (company_id, email),
  INDEX (company_id),
  INDEX (name),
  INDEX (email),
  INDEX (department),
  INDEX (job_title)
) ENGINE InnoDB DEFAULT CHARSET utf8;
