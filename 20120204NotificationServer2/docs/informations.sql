CREATE DATABASE `informations` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `informations` (
  `ID` bigint(64) unsigned NOT NULL AUTO_INCREMENT,
  `appId` varchar(64) NOT NULL,
  `senderId` varchar(64) NOT NULL,
  `receiverId` varchar(64) NOT NULL,
  `type` varchar(64) NOT NULL,
  `action` varchar(64) DEFAULT NULL,
  `date` datetime NOT NULL,
  `title` varchar(256) DEFAULT NULL,
  `data` varchar(2048) DEFAULT NULL,
  `uri` varchar(1024) DEFAULT NULL,
  `processed` tinyint(1) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `id` (`ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=103 ;

ALTER TABLE `informations` ADD `process_method` INT NOT NULL DEFAULT '1' AFTER `processed` ,
ADD `importance` INT NOT NULL DEFAULT '30' AFTER `process_method` 

--2011.11.29 modify
ALTER TABLE `informations` ADD `body` VARCHAR( 2048 ) CHARACTER SET utf8 COLLATE armscii8_general_ci NULL ,
ADD `body_html` VARCHAR( 4096 ) CHARACTER SET utf8 COLLATE armscii8_general_ci NULL ,
ADD `title_html` VARCHAR( 1024 ) CHARACTER SET utf8 COLLATE armscii8_general_ci NULL ,
ADD `guid` VARCHAR( 256 ) CHARACTER SET utf8 COLLATE armscii8_general_ci NULL 

ALTER TABLE `informations` ADD `object_id` VARCHAR( 256 ) NULL DEFAULT NULL

ALTER TABLE `informations` ADD `last_modified` DATETIME NOT NULL 
ALTER TABLE `informations` ADD `read` TINYINT(1) NULL DEFAULT '0' AFTER `processed`

CREATE USER 'information'@'%' IDENTIFIED BY 'information2008';

GRANT ALL PRIVILEGES ON `informations` . * TO 'information'@'%' WITH GRANT OPTION ;


# clear task
SET GLOBAL event_scheduler = ON; 

USE informations;
CREATE EVENT e_notifications_clear  
ON SCHEDULE EVERY 30 day  
DO DELETE FROM informations WHERE last_modified < date_sub(NOW(), interval 30 day)

SELECT * FROM `informations` WHERE last_modified < date_sub(NOW(), interval 30 day)

