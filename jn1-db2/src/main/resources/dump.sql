-- MySQL dump 10.13  Distrib 5.7.16, for osx10.11 (x86_64)
--
-- Host: localhost    Database: jn1_telecom
-- ------------------------------------------------------
-- Server version	5.7.16

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `basic`
--

DROP TABLE IF EXISTS `basic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `basic` (
  `begin_time` datetime DEFAULT NULL,
  `call_cost_per_min` double DEFAULT NULL,
  `text_cost_per_message` double DEFAULT NULL,
  `local_traffic_cost_per_mb` double DEFAULT NULL,
  `domestic_traffic_cost_per_mb` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `basic`
--

LOCK TABLES `basic` WRITE;
/*!40000 ALTER TABLE `basic` DISABLE KEYS */;
INSERT INTO `basic` VALUES ('1998-01-01 00:00:00',0.5,0.1,2,5);
/*!40000 ALTER TABLE `basic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `combo`
--

DROP TABLE IF EXISTS `combo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `combo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cost_per_month` double DEFAULT NULL,
  `free_call_min` double DEFAULT NULL,
  `free_messages` int(11) DEFAULT NULL,
  `free_local_traffic` double DEFAULT NULL,
  `free_domestic_traffic` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `combo`
--

LOCK TABLES `combo` WRITE;
/*!40000 ALTER TABLE `combo` DISABLE KEYS */;
INSERT INTO `combo` VALUES (1,20,100,0,0,0),(2,10,0,200,0,0),(3,10,0,0,1000,0),(4,50,0,0,500,2000);
/*!40000 ALTER TABLE `combo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `operation`
--

DROP TABLE IF EXISTS `operation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `operation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(20) DEFAULT NULL,
  `operation` enum('order','cancel') DEFAULT NULL,
  `combo_id` int(11) DEFAULT NULL,
  `operation_time` datetime DEFAULT NULL,
  `effectuate_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation`
--

LOCK TABLES `operation` WRITE;
/*!40000 ALTER TABLE `operation` DISABLE KEYS */;
INSERT INTO `operation` VALUES (1,'189','order',1,'1998-01-01 00:00:00','1998-01-01 00:00:00'),(2,'189','order',4,'2018-01-03 00:00:00','2018-01-03 00:00:00');
/*!40000 ALTER TABLE `operation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_calls`
--

DROP TABLE IF EXISTS `user_calls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_calls` (
  `username` varchar(20) DEFAULT NULL,
  `begin_time` datetime DEFAULT NULL,
  `duration` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_calls`
--

LOCK TABLES `user_calls` WRITE;
/*!40000 ALTER TABLE `user_calls` DISABLE KEYS */;
INSERT INTO `user_calls` VALUES ('189','2018-01-04 00:00:02',100);
/*!40000 ALTER TABLE `user_calls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_texts`
--

DROP TABLE IF EXISTS `user_texts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_texts` (
  `username` varchar(20) DEFAULT NULL,
  `send_time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_texts`
--

LOCK TABLES `user_texts` WRITE;
/*!40000 ALTER TABLE `user_texts` DISABLE KEYS */;
INSERT INTO `user_texts` VALUES ('189','2018-01-04 00:00:02'),('189','2018-01-04 00:00:05'),('189','2018-01-04 00:00:04');
/*!40000 ALTER TABLE `user_texts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_traffic`
--

DROP TABLE IF EXISTS `user_traffic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_traffic` (
  `username` varchar(20) DEFAULT NULL,
  `request_time` datetime DEFAULT NULL,
  `mb` double DEFAULT NULL,
  `type` enum('domestic','local') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_traffic`
--

LOCK TABLES `user_traffic` WRITE;
/*!40000 ALTER TABLE `user_traffic` DISABLE KEYS */;
INSERT INTO `user_traffic` VALUES ('189','2018-01-04 00:00:00',300,'local'),('189','2018-01-04 00:00:02',2000,'domestic'),('189','2018-01-04 00:00:01',500,'local');
/*!40000 ALTER TABLE `user_traffic` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-10-30  9:46:23
