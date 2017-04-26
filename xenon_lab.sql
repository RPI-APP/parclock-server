-- Adminer 4.2.4 MySQL dump

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

DROP TABLE IF EXISTS `ci_sessions`;
CREATE TABLE `ci_sessions` (
  `session_id` varchar(40) COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `ip_address` varchar(45) COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `user_agent` varchar(120) COLLATE utf8_unicode_ci NOT NULL,
  `last_activity` int(10) unsigned NOT NULL DEFAULT '0',
  `user_data` text COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`session_id`),
  KEY `last_activity_idx` (`last_activity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT INTO `ci_sessions` (`session_id`, `ip_address`, `user_agent`, `last_activity`, `user_data`) VALUES
('3cba4f85bdde6896821f3ca8c0e92c55',	'127.0.0.1',	'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0',	1479486622,	''),
('9f2e8240e9b77d33273918164be06035',	'128.113.209.197',	'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36',	1493178067,	''),
('c765bce1f3975965207f5124450c3918',	'128.113.209.147',	'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36',	1479505652,	'');

DROP TABLE IF EXISTS `current_clients`;
CREATE TABLE `current_clients` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `data_field`;
CREATE TABLE `data_field` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `guid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8_unicode_ci NOT NULL,
  `description` text COLLATE utf8_unicode_ci NOT NULL,
  `instrument` int(11) NOT NULL,
  `type` tinyint(4) NOT NULL COMMENT '0=int,1=float,2=string',
  `enabled` tinyint(4) NOT NULL DEFAULT '1',
  `timeout` int(11) NOT NULL DEFAULT '10000',
  PRIMARY KEY (`id`),
  UNIQUE KEY `guid` (`guid`),
  UNIQUE KEY `name_instrument` (`name`,`instrument`),
  KEY `data_field_group` (`instrument`),
  CONSTRAINT `data_field_ibfk_2` FOREIGN KEY (`instrument`) REFERENCES `instrument` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `data_type_float`;
CREATE TABLE `data_type_float` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_field` int(11) NOT NULL,
  `data` decimal(34,17) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_field_timestamp` (`data_field`,`timestamp`),
  CONSTRAINT `data_type_float_ibfk_2` FOREIGN KEY (`data_field`) REFERENCES `data_field` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `data_type_int`;
CREATE TABLE `data_type_int` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_field` int(11) NOT NULL,
  `data` bigint(20) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_field_timestamp` (`data_field`,`timestamp`),
  CONSTRAINT `data_type_int_ibfk_1` FOREIGN KEY (`data_field`) REFERENCES `data_field` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `data_type_string`;
CREATE TABLE `data_type_string` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_field` int(11) NOT NULL,
  `data` text COLLATE utf8_unicode_ci NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_field_timestamp` (`data_field`,`timestamp`),
  CONSTRAINT `data_type_string_ibfk_1` FOREIGN KEY (`data_field`) REFERENCES `data_field` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `freeboard`;
CREATE TABLE `freeboard` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user` int(11) NOT NULL,
  `json` mediumtext COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user` (`user`),
  CONSTRAINT `freeboard_ibfk_1` FOREIGN KEY (`user`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `information`;
CREATE TABLE `information` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(32) COLLATE utf8_unicode_ci NOT NULL,
  `value` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT INTO `information` (`id`, `title`, `value`) VALUES
(1,	'clock',	1493178741163);

DROP TABLE IF EXISTS `instrument`;
CREATE TABLE `instrument` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `enabled` int(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `log`;
CREATE TABLE `log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `text` mediumtext COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `time_marker`;
CREATE TABLE `time_marker` (
  `id` int(11) NOT NULL,
  `name` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `description` text COLLATE utf8_unicode_ci NOT NULL,
  `timestamp` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `can_users_edit` tinyint(4) NOT NULL DEFAULT '0',
  `can_instruments_rem` tinyint(4) NOT NULL DEFAULT '0',
  `can_instruments_add` tinyint(4) NOT NULL DEFAULT '0',
  `can_data_view` tinyint(4) NOT NULL DEFAULT '1',
  `can_markers_manage` tinyint(4) NOT NULL DEFAULT '0',
  `json` mediumtext COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT INTO `users` (`id`, `username`, `password`, `name`, `can_users_edit`, `can_instruments_rem`, `can_instruments_add`, `can_data_view`, `can_markers_manage`, `json`) VALUES
(1,	'centod',	'29218137c77d38259d18a55d319098b07478cd48',	'Daniel Centore',	1,	1,	1,	1,	1,	'{\n	\"version\": 1,\n	\"allow_edit\": true,\n	\"plugins\": [],\n	\"panes\": [\n		{\n			\"title\": \"Potato Peeler\",\n			\"width\": 1,\n			\"row\": {\n				\"2\": 1,\n				\"3\": 1\n			},\n			\"col\": {\n				\"2\": 2,\n				\"3\": 2\n			},\n			\"col_width\": 1,\n			\"widgets\": [\n				{\n					\"type\": \"indicator\",\n					\"settings\": {\n						\"value\": \"datasources[\\\"Test Double 1\\\"][\\\"connected\\\"]\",\n						\"on_text\": \"CONNECTED\",\n						\"off_text\": \"DISCONNECTED\"\n					}\n				},\n				{\n					\"type\": \"text_widget\",\n					\"settings\": {\n						\"title\": \"Rounded Value\",\n						\"size\": \"regular\",\n						\"value\": \"datasources[\\\"Test Double 1\\\"][\\\"round_value\\\"]\",\n						\"sparkline\": false,\n						\"animate\": false\n					}\n				},\n				{\n					\"type\": \"sparkline\",\n					\"settings\": {\n						\"value\": [\n							\"datasources[\\\"Test Double 1\\\"][\\\"round_value\\\"]\",\n							\"datasources[\\\"Test Double 1\\\"][\\\"int_value\\\"]\"\n						],\n						\"include_legend\": true,\n						\"legend\": \"Actual Value,Rounded to Integer\"\n					}\n				},\n				{\n					\"type\": \"gauge\",\n					\"settings\": {\n						\"value\": \"datasources[\\\"Test Double 1\\\"][\\\"round_value\\\"]\",\n						\"units\": \"liters\",\n						\"min_value\": \"-2\",\n						\"max_value\": \"2\"\n					}\n				}\n			]\n		},\n		{\n			\"title\": \"Sum\",\n			\"width\": 1,\n			\"row\": {\n				\"2\": 1,\n				\"3\": 1\n			},\n			\"col\": {\n				\"2\": 1,\n				\"3\": 3\n			},\n			\"col_width\": 1,\n			\"widgets\": [\n				{\n					\"type\": \"sparkline\",\n					\"settings\": {\n						\"value\": [\n							\"datasources[\\\"Hey\\\"][\\\"round_value\\\"] + datasources[\\\"Soup\\\"][\\\"round_value\\\"] / 5\"\n						]\n					}\n				},\n				{\n					\"type\": \"html\",\n					\"settings\": {\n						\"html\": \"This is: (Potato Peeler + Soup Maker / 5)\",\n						\"height\": 1\n					}\n				}\n			]\n		},\n		{\n			\"width\": 1,\n			\"row\": {\n				\"2\": 9,\n				\"3\": 1\n			},\n			\"col\": {\n				\"2\": 1,\n				\"3\": 1\n			},\n			\"col_width\": 1,\n			\"widgets\": []\n		},\n		{\n			\"title\": \"Soup Maker\",\n			\"width\": 1,\n			\"row\": {\n				\"2\": 13,\n				\"3\": 5\n			},\n			\"col\": {\n				\"2\": 1,\n				\"3\": 1\n			},\n			\"col_width\": 1,\n			\"widgets\": [\n				{\n					\"type\": \"indicator\",\n					\"settings\": {\n						\"value\": \"datasources[\\\"Soup\\\"][\\\"connected\\\"]\",\n						\"on_text\": \"CONNECTED\",\n						\"off_text\": \"DISCONNECTED\"\n					}\n				},\n				{\n					\"type\": \"text_widget\",\n					\"settings\": {\n						\"title\": \"Rounded Value\",\n						\"size\": \"regular\",\n						\"value\": \"datasources[\\\"Soup\\\"][\\\"round_value\\\"]\",\n						\"animate\": false\n					}\n				},\n				{\n					\"type\": \"sparkline\",\n					\"settings\": {\n						\"value\": [\n							\"datasources[\\\"Soup\\\"][\\\"round_value\\\"]\"\n						]\n					}\n				},\n				{\n					\"type\": \"sparkline\",\n					\"settings\": {\n						\"title\": \"Slow\",\n						\"value\": [\n							\"datasources[\\\"SoupSlow\\\"][\\\"round_value\\\"]\"\n						]\n					}\n				}\n			]\n		}\n	],\n	\"datasources\": [\n		{\n			\"name\": \"Test Double 1\",\n			\"type\": \"centek_database_plugin\",\n			\"settings\": {\n				\"guid\": \"(dbl) [ Test Instrument / Test Double 1 ] 3594726e-0e3b-11e6-8187-6c0b843e9461\",\n				\"refresh_time\": \"1000\"\n			}\n		}\n	],\n	\"columns\": 3\n}'),
(11,	'admin',	'd948d86510198ef7171c469b63acba2b18564ca6',	'Administrator',	1,	0,	1,	1,	1,	NULL);

-- 2017-04-26 03:52:21
