CREATE TABLE `boxes` (
  `boxId` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `owner` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`boxId`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

 CREATE TABLE `messages` (
  `msgId` int(11) NOT NULL AUTO_INCREMENT,
  `boxId` int(11) NOT NULL,
  `msgIndex` int(11) NOT NULL,
  `message` mediumtext,
  `epoch` mediumtext NOT NULL,
  `sender` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`msgId`),
  UNIQUE KEY `boxIdMsgIdx` (`boxId`,`msgIndex`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`boxId`) REFERENCES `boxes` (`boxId`)
) ENGINE=InnoDB AUTO_INCREMENT=1408 DEFAULT CHARSET=utf8;

CREATE TABLE `tokens` (
  `tokenId` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`tokenId`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

