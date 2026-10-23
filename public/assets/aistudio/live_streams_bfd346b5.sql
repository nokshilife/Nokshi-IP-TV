-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Oct 20, 2026 at 08:56 PM
-- Server version: 10.11.19-MariaDB-cll-lve
-- PHP Version: 8.4.26

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `bas24db`
--

-- --------------------------------------------------------

--
-- Table structure for table `live_streams`
--

CREATE TABLE `live_streams` (
  `id` int(11) NOT NULL,
  `ChannelURL` varchar(255) NOT NULL,
  `youtube_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stream_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Poster` varchar(255) DEFAULT NULL,
  `Category` tinyint(1) NOT NULL DEFAULT 0,
  `is_active` tinyint(4) DEFAULT 1,
  `display_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `live_streams`
--

INSERT INTO `live_streams` (`id`, `ChannelURL`, `youtube_url`, `stream_name`, `Poster`, `Category`, `is_active`, `display_order`, `created_at`) VALUES
(1, 'https://www.youtube.com/@news24television90', 'https://www.youtube.com/watch?v=NoXnyiPzyg8', 'ðŸ”´ News24 | ðŸ“± Dev: Md A Jalil | â—€ï¸ Left - â–¶ï¸ Right: Change Channel | â¬‡ï¸ Down: Channel List', 'https://images.seeklogo.com/logo-png/36/1/news24-channel-logo-png_seeklogo-362149.png', 0, 0, 9, '2025-04-07 17:31:09'),
(2, 'https://www.youtube.com/@channel24digital', 'https://www.youtube.com/watch?v=p6EO0TuILpw', 'ðŸ”´ à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à§¨à§ª  |  â—€ï¸ à¦¬à¦¾à¦®à§‡ - â–¶ï¸ à¦¡à¦¾à¦¨à§‡: à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦ªà¦°à¦¿à¦¬à¦°à§à¦¤à¦¨  |  â¬‡ï¸ à¦¨à¦¿à¦šà§‡: à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦²à¦¿à¦¸à§à¦Ÿ', 'https://images.seeklogo.com/logo-png/42/1/channel-24-logo-png_seeklogo-424910.png', 0, 1, 0, '2025-04-07 17:32:08'),
(3, 'https://www.youtube.com/@channelionline', 'https://www.youtube.com/watch?v=COGMxvfc4pg', 'à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦†à¦‡ ðŸ‘ï¸ à¦…à¦¨à¦²à¦¾à¦‡à¦¨', 'https://images.seeklogo.com/logo-png/42/1/channel-i-logo-png_seeklogo-427745.png', 0, 0, 11, '2025-04-07 17:32:44'),
(4, 'https://www.youtube.com/@ekhontv', 'https://www.youtube.com/watch?v=gh_DJW0hB8w', 'à¦à¦–à¦¨ à¦Ÿà¦¿à¦­à¦¿', 'https://ekhon.tv/logo.png', 0, 1, 10, '2025-04-07 17:35:08'),
(5, 'https://www.youtube.com/@somoynews360', 'https://www.youtube.com/watch?v=JSaja_7xyvM', 'à¦¸à¦®à¦¯à¦¼ à¦Ÿà¦¿à¦­à¦¿', 'https://jalil.is-great.net/images/SomoyTV.jpg', 0, 1, 5, '2025-04-07 17:35:48'),
(6, 'https://www.youtube.com/@aljazeeraenglish', 'https://www.youtube.com/watch?v=gCNeDWCI0vo', 'ðŸ”´ à¦†à¦² à¦œà¦¾à¦œà¦¿à¦°à¦¾', 'https://logos-world.net/wp-content/uploads/2023/04/Al-Jazeera-Logo-500x281.png', 0, 1, 42, '2025-04-07 17:36:34'),
(7, 'https://www.youtube.com/@JamunaTVbd', 'https://www.youtube.com/watch?v=yRX-c1F1Clg', 'ðŸ”´ à¦¯à¦®à§à¦¨à¦¾ à¦Ÿà¦¿à¦­à¦¿  |  â¬‡ï¸ à¦¨à¦¿à¦šà§‡: à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦²à¦¿à¦¸à§à¦Ÿ  |  ðŸ” à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦²à¦¿à¦¸à§à¦Ÿ à¦¥à§‡à¦•à§‡ à¦²à¦—à¦‡à¦¨ à¦•à¦°à§à¦¨', 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fe/Jamuna_TV_logo.svg/960px-Jamuna_TV_logo.svg.png?utm_source=bn.wikipedia.org&utm_campaign=index&utm_content=thumbnail', 0, 1, 4, '2025-04-07 17:38:50'),
(8, 'https://www.youtube.com/@atnnewsltd', 'https://www.youtube.com/watch?v=03Rp8h4qykk', 'ðŸ”´ à¦à¦Ÿà¦¿à¦à¦¨ à¦¨à¦¿à¦‰à¦œ', 'https://jalil.is-great.net/images/ATN_News.jpg', 0, 1, 6, '2025-04-07 17:40:44'),
(9, 'https://www.youtube.com/@IndependentTelevision', 'https://www.youtube.com/watch?v=iWq2kH0iq2Q', 'à¦‡à¦¨à§à¦¡à¦¿à¦ªà§‡à¦¨à§à¦¡à§‡à¦¨à§à¦Ÿ à¦Ÿà§‡à¦²à¦¿à¦­à¦¿à¦¶à¦¨', 'https://cdn.itvbd.net/contents/themes/public/style/images/logo.png', 0, 1, 2, '2025-04-07 17:44:33'),
(10, 'https://www.youtube.com/@RtvNews', 'https://www.youtube.com/watch?v=FiQW0M9unNY', 'à¦†à¦° à¦Ÿà¦¿à¦­à¦¿', 'https://images.seeklogo.com/logo-png/23/1/r-tv-logo-png_seeklogo-237300.png', 0, 0, 16, '2025-04-07 17:45:39'),
(11, 'https://www.youtube.com/@EkattorTelevision', 'https://www.youtube.com/watch?v=Z7R3s-KVg60', 'à§­à§§ à¦Ÿà¦¿à¦­à¦¿', 'https://cdn.ekattorbd.com/contents/themes/public/style/images/logo.png', 0, 1, 3, '2025-04-07 17:46:56'),
(12, 'https://www.youtube.com/@dbcnewstv', 'https://www.youtube.com/watch?v=_j_rSwx5Kso', 'à¦¡à¦¿à¦¬à¦¿à¦¸à¦¿ à¦¨à¦¿à¦‰à¦œ', 'https://images.seeklogo.com/logo-png/62/1/dbc-news-logo-png_seeklogo-626101.png', 0, 1, 7, '2025-04-07 17:48:45'),
(13, 'https://www.youtube.com/@NTVDigitalLive', 'https://www.youtube.com/watch?v=bzkwxuW8Z4k', 'ðŸ”´ à¦à¦¨ à¦Ÿà¦¿à¦­à¦¿', 'https://jalil.is-great.net/images/NTV.jpg', 0, 1, 13, '2025-04-07 17:52:05'),
(14, 'https://www.youtube.com/@AlQuran4KOfficial', 'https://www.youtube.com/watch?v=UegEBr979G8', 'ðŸ”´ à¦ªà¦¬à¦¿à¦¤à§à¦° à¦®à¦•à§à¦•à¦¾ à¦¸à¦°à¦¾à¦¸à¦°à¦¿ ðŸ•‹', 'https://w7.pngwing.com/pngs/320/417/png-transparent-illustration-of-kaaba-mecca-kaaba-medina-great-mosque-of-mecca-hajj-umrah-islam-angle-logo-eid-aladha-thumbnail.png', 0, 1, 31, '2025-04-08 16:53:14'),
(15, 'https://www.youtube.com/@SkyNews', 'https://www.youtube.com/watch?v=xDWQ3LkccY8', 'à¦¸à§à¦•à¦¾à¦‡ à¦¨à¦¿à¦‰à¦œ', 'https://jalil.is-great.net/images/SkyNews.jpg', 0, 1, 46, '2025-04-08 16:54:25'),
(16, 'https://www.youtube.com/@WION', 'https://www.youtube.com/watch?v=vfszY1JYbMc', 'WiOn News', 'https://jalil.is-great.net/images/WiOnTV.jpg', 0, 1, 37, '2025-04-08 16:55:06'),
(17, 'https://www.youtube.com/@republicbangla', 'https://www.youtube.com/watch?v=YoAzEs1GwJg', 'à¦°à¦¿à¦ªà¦¾à¦¬à¦²à¦¿à¦• à¦¬à¦¾à¦‚à¦²à¦¾ à¦—à§à¦œà¦¬ à¦Ÿà¦¿à¦­à¦¿', 'https://jalil.is-great.net/images/RBanglaTV.jpg', 0, 1, 35, '2025-04-10 15:11:41'),
(18, 'https://www.youtube.com/@channelnewsasia', 'https://www.youtube.com/watch?v=-PWMxjBgr0k', 'CNA', 'https://jalil.is-great.net/images/CNA.jpg', 0, 1, 50, '2025-04-10 15:26:14'),
(19, 'https://www.youtube.com/@trtworld', 'https://www.youtube.com/watch?v=qBe59l6p_EU', 'TRT World', 'https://jalil.is-great.net/images/TRTWorld.jpg', 0, 1, 51, '2025-04-10 15:34:13'),
(20, 'https://www.youtube.com/@BoomerangUK', 'https://www.youtube.com/watch?v=rEKifG2XUZg', 'Tom & Jerry', NULL, 0, 1, 94, '2025-04-10 15:39:33'),
(21, 'https://www.youtube.com/@oggy', 'https://www.youtube.com/watch?v=1R8767hzeLk', 'Oggy and The Cockroaches', NULL, 0, 0, 97, '2025-04-10 15:41:41'),
(22, 'https://m.youtube.com/@livedeshtv', 'https://www.youtube.com/watch?v=VxAnLFTH-7E', 'à¦¦à§‡à¦¶ à¦Ÿà¦¿à¦­à¦¿', 'https://jalil.is-great.net/images/DeshTV.jpg', 0, 1, 20, '2025-04-10 15:43:57'),
(23, 'https://www.youtube.com/@MrBean', 'https://www.youtube.com/watch?v=S_71wzZMf0M', 'Mr Bean', NULL, 0, 1, 108, '2025-04-10 15:45:12'),
(24, 'https://www.youtube.com/@lovenature', 'https://www.youtube.com/watch?v=QhahoVG0BfI', 'Love Nature', 'https://upload.wikimedia.org/wikipedia/commons/0/0d/Love_Nature_logo_%282025%29.svg', 0, 1, 64, '2025-04-10 15:53:08'),
(25, 'https://www.youtube.com/@KOLKATATV', 'https://www.youtube.com/watch?v=6O8JnafDA80', 'à¦•à¦²à¦•à¦¾à¦¤à¦¾ à¦Ÿà¦¿à¦­à¦¿', 'https://jalil.is-great.net/images/KolkataTV.jpg', 0, 1, 34, '2025-04-10 15:55:49'),
(26, 'https://www.youtube.com/@PeppasPlaygroupLittleLearners', 'https://www.youtube.com/watch?v=m_HcXArl7AQ', 'ðŸ”´ Peppa Pig: Best Full Episodes 2025', NULL, 0, 1, 98, '2025-04-10 16:07:33'),
(27, 'https://www.youtube.com/@ShaunandFriendsOfficial', 'https://www.youtube.com/watch?v=UavAcv2CBfc', 'Shaun the Sheep - New Episodes', NULL, 0, 0, 105, '2025-04-10 16:10:33'),
(28, 'https://www.youtube.com/@MashaBearEN', 'https://www.youtube.com/watch?v=lIpoONCcZqE', 'Masha and the Bear', NULL, 0, 1, 102, '2025-04-12 16:22:43'),
(29, 'https://www.youtube.com/@DhakaPostOfficial', 'https://www.youtube.com/watch?v=HAGVpgRBCn8', 'ðŸ”´ à¦¢à¦¾à¦•à¦¾ à¦ªà§‹à¦¸à§à¦Ÿ', 'https://assets.dhakapost.com/media/setup/author/dhaka-post-20260806183058.jpeg?width=100&height=100&width=100&height=100', 0, 0, 24, '2025-04-14 06:13:09'),
(31, 'https://www.youtube.com/@discoveryrelaxation3457', 'https://www.youtube.com/watch?v=H3JYmEYC1pk', 'Discovery Relaxation', 'https://png.pngtree.com/png-vector/20220417/ourmid/pngtree-women-nature-logo-png-image_4546651.png', 0, 1, 66, '2025-04-14 06:27:54'),
(32, 'https://www.youtube.com/@MrBeanTVOnYouTube', 'https://www.youtube.com/watch?v=Abe5jlZ-QPY', 'Mr Bean Live Action', NULL, 0, 0, 107, '2025-04-15 07:06:43'),
(34, 'https://www.youtube.com/@BoomerangUK', 'https://www.youtube.com/watch?v=90kaC8Kbw0k', 'Tom and Jerry - Super', NULL, 0, 1, 96, '2025-04-15 07:12:38'),
(35, 'https://www.youtube.com/@BoomerangUK', 'https://www.youtube.com/watch?v=GEw3m4xvct4', 'Tom & Jerry FUNNIEST Adventures', NULL, 0, 1, 95, '2025-04-15 07:14:03'),
(46, 'https://www.youtube.com/@ShaunandFriendsOfficial', 'https://www.youtube.com/watch?v=ON5SiD1fmq0', 'Shaun and the Sheep', NULL, 0, 0, 106, '2025-04-16 03:44:01'),
(66, 'https://www.youtube.com/@bollywoodqueenss', 'https://www.youtube.com/watch?v=2MUulFP0rJ4', 'à§¯à§¦ à¦¦à¦¶à¦•à§‡à¦° à¦¹à¦¿à¦¨à§à¦¦à§€ à¦—à¦¾à¦¨', NULL, 0, 1, 80, '2025-04-17 14:21:16'),
(67, 'https://www.youtube.com/channel/UCD90fziYsFWGcTXIEFcqw5A', 'https://www.youtube.com/watch?v=-DDpGO398Ho', 'Summer Mix 2025', NULL, 0, 1, 81, '2025-04-17 14:48:39'),
(68, 'https://www.youtube.com/@ntparty98', 'https://www.youtube.com/watch?v=I9Kn7uYT9Vs', 'N&T Party', NULL, 0, 1, 82, '2025-04-17 14:48:39'),
(69, 'https://www.youtube.com/@TopGearClassic', 'https://www.youtube.com/watch?v=Xw2Xu5wKQVw', 'BBC Top Gear', 'https://banner2.cleanpng.com/20180523/ok/kisspng-car-the-stig-bbc-worldwide-television-show-schoendorf-construction-co-llc-5b05aa3f6a5bb8.3070606315270979194357.jpg', 0, 1, 53, '2025-04-19 06:55:39'),
(70, 'https://www.youtube.com/@cnnnews18', 'https://www.youtube.com/watch?v=rfDx1HMvXbQ', 'CNN 18', 'https://jalil.is-great.net/images/CNN18.jpg', 0, 1, 48, '2025-04-20 17:56:20'),
(71, 'https://www.youtube.com/@JagoLiveNews', 'https://www.youtube.com/watch?v=iYbIWLOO2ac', 'à¦œà¦¾à¦—à§‹ à¦¨à¦¿à¦‰à¦œ', 'https://cdn.jagonews24.com/media/common/new-logo.png', 0, 1, 27, '2025-04-21 12:38:04'),
(72, 'https://www.youtube.com/@CTVNAKDPLUS', 'https://www.youtube.com/watch?v=h96J3JfwOII', 'CTVN AKD PLUS', 'https://static.wikia.nocookie.net/logopedia/images/a/ad/Ctvn_akd_plus.png', 0, 1, 33, '2025-04-21 16:52:32'),
(73, 'https://www.youtube.com/@SATVTalkShow', 'https://www.youtube.com/watch?v=njGyURGmRcc', 'à¦à¦¸ à¦ à¦Ÿà¦¿à¦­à¦¿', 'https://www.satv.tv/wp-content/uploads/2016/07/logo1.png', 0, 0, 25, '2025-04-21 17:13:04'),
(75, 'https://www.youtube.com/@unitednations', 'https://www.youtube.com/watch?v=vYRfQo6JMxc', 'à¦œà¦¾à¦¤à¦¿ à¦¸à¦‚à¦˜', 'https://jalil.is-great.net/images/UNTV.jpg', 0, 1, 49, '2025-04-23 04:08:56'),
(77, 'https://www.youtube.com/@TSportsbd', 'https://www.youtube.com/watch?v=7LA046brW08', 'à¦Ÿà¦¿-à¦¸à§à¦ªà§‹à¦°à§à¦Ÿà¦¸', NULL, 0, 0, 58, '2025-04-26 04:56:36'),
(78, 'https://www.youtube.com/@MarvelHQ', 'https://www.youtube.com/watch?v=yABPtUc7KOg', 'Marvel', NULL, 0, 1, 99, '2025-04-26 19:19:15'),
(83, 'https://www.youtube.com/@ALLTHEKPOP', 'https://www.youtube.com/watch?v=GaccTXdeRRI', 'K-Pop', NULL, 0, 0, 83, '2025-04-30 17:08:24'),
(84, 'https://www.youtube.com/@dag-music', 'https://www.youtube.com/watch?v=9ylmSDP9VSA', 'Dag Music', NULL, 0, 0, 86, '2025-04-30 17:11:47'),
(85, 'https://www.youtube.com/@quieromusicatv', 'https://www.youtube.com/watch?v=vGNglKWqwcQ', 'Quiero MÃºsica TV', NULL, 0, 1, 87, '2025-04-30 17:19:30'),
(86, 'https://www.youtube.com/@thekpop2', 'https://www.youtube.com/watch?v=Yc0pIJcbjdk', 'THE K-POP2', NULL, 0, 0, 84, '2025-04-30 17:23:16'),
(87, 'https://www.youtube.com/@zifostudio', 'https://www.youtube.com/watch?v=cERy_JvCJeg', 'ZIFO STUDIO', NULL, 0, 0, 85, '2025-04-30 17:27:32'),
(89, 'https://www.youtube.com/@CNAInsider', 'https://www.youtube.com/watch?v=9Y4kOQk_K3Y', 'CNA Insider', 'https://images.squarespace-cdn.com/content/v1/654ddd678339736c2176e230/38333b9d-587a-432c-bb1a-1dbaacd352e2/CNA+insider+PNG.png', 0, 1, 52, '2025-04-30 17:31:31'),
(90, 'https://www.youtube.com/@NatGeoAnimals', 'https://www.youtube.com/watch?v=MiQe9ob9aDc', 'Nat Geo Animals', 'https://jalil.is-great.net/images/NatGeoWild.jpg', 0, 1, 63, '2025-04-30 17:32:56'),
(91, 'https://www.youtube.com/@Cartoons-GoldenCollection', 'https://www.youtube.com/watch?v=bMFRPomEUfQ', 'Soviet Toons', NULL, 0, 1, 101, '2025-04-30 17:39:54'),
(93, 'https://www.youtube.com/@NatGeo', 'https://www.youtube.com/watch?v=tbLcWRxPel0', 'National Geographic', 'https://jalil.is-great.net/images/NatGeo.jpg', 0, 1, 62, '2025-04-30 17:54:56'),
(94, 'https://www.youtube.com/@ShoutStudios', 'https://www.youtube.com/watch?v=PXj0J8Q8N34', 'Shout Studio', 'https://images.seeklogo.com/logo-png/49/1/shout-studios-logo-png_seeklogo-491477.png', 0, 1, 65, '2025-04-30 18:00:47'),
(95, 'https://www.youtube.com/@TravelChannel', 'https://www.youtube.com/watch?v=Fvdg9-maSEc', 'Travel Channel', 'https://banner2.cleanpng.com/lnd/20250117/wl/d4afd55f2c504864a91718576d0e4a.webp', 0, 1, 60, '2025-04-30 18:03:48'),
(96, 'https://www.youtube.com/@disneyjr', 'https://www.youtube.com/watch?v=-WIhWB9pEqo', 'Disney Jr.', NULL, 0, 1, 100, '2025-04-30 18:12:21'),
(97, 'https://www.youtube.com/@SunriseJungleBeat', 'https://www.youtube.com/watch?v=vOz5i3x7UO0', 'Jungle Beat', NULL, 0, 1, 104, '2025-04-30 18:16:59'),
(98, 'https://www.youtube.com/@SpongeBobOfficial', 'https://www.youtube.com/watch?v=SkjgMm6xRbg', 'SpongeBob', NULL, 0, 1, 103, '2025-04-30 18:23:56'),
(99, 'https://www.youtube.com/@BanijayEngine', 'https://www.youtube.com/watch?v=WcgqJxghvEM', 'Banijay Engine', 'https://images.seeklogo.com/logo-png/39/1/banijay-group-logo-png_seeklogo-398517.png', 0, 0, 54, '2025-04-30 18:31:00'),
(100, 'https://www.youtube.com/@Riyadiyatv', 'https://www.youtube.com/watch?v=TK1XWtPXj6w', 'à¦¸à§Œà¦¦à¦¿ à¦¸à§à¦ªà§‹à¦°à§à¦Ÿà¦¸', NULL, 0, 1, 59, '2025-05-01 03:54:03'),
(101, 'https://www.youtube.com/@BanijayHomeandGarden', 'https://www.youtube.com/watch?v=KU_2Dx3IfPM', 'Banijay Home and Garden', NULL, 0, 0, 67, '2025-05-01 03:54:03'),
(102, 'https://www.youtube.com/@banijayscience', 'https://www.youtube.com/watch?v=RG0fiauDAOI', 'Banijay Science', 'https://images.seeklogo.com/logo-png/49/1/banijay-germany-logo-png_seeklogo-494785.png', 0, 0, 55, '2025-05-01 03:55:41'),
(104, 'https://www.youtube.com/@JoyPrimeTV', 'https://www.youtube.com/watch?v=4FHXS9q6ZBA', 'Joy Prime TV', NULL, 0, 1, 72, '2025-05-01 04:00:41'),
(105, 'https://www.youtube.com/@WatchMojo', 'https://www.youtube.com/watch?v=AHa_Uz50sJk', 'WatchMojo', NULL, 0, 1, 68, '2025-05-01 04:05:03'),
(106, 'https://www.youtube.com/@HGTV', 'https://www.youtube.com/watch?v=ENkf85U_XkM', 'HGTV', NULL, 0, 1, 69, '2025-05-01 04:11:11'),
(107, 'https://www.youtube.com/@TheAsylumMovieChannel', 'https://www.youtube.com/watch?v=zGADFbACcKQ', 'Asylum Movie Channel', NULL, 0, 0, 70, '2025-05-01 04:14:35'),
(109, 'https://www.youtube.com/@PTVWorldOfficial', 'https://www.youtube.com/watch?v=x3Pn2CUfYaI', 'à¦ªà¦¾à¦•à¦¿à¦¸à§à¦¤à¦¾à¦¨ à¦Ÿà§‡à¦²à¦¿à¦­à¦¿à¦¶à¦¨', 'https://images.seeklogo.com/logo-png/22/1/ptv-logo-png_seeklogo-220051.png', 0, 0, 38, '2025-05-07 17:36:04'),
(110, 'https://www.youtube.com/@calcuttanews', 'https://www.youtube.com/watch?v=OJUk-YAUYfA', 'à¦•à§à¦¯à¦¾à¦²à¦•à¦¾à¦Ÿà¦¾ à¦—à§à¦œà¦¬ à¦¨à¦¿à¦‰à¦œ', 'https://static.wikia.nocookie.net/etv-gspn-bangla/images/d/d1/CTVN_AKD_Plus_logo_%281999-present%29.png', 0, 1, 36, '2025-05-07 17:46:59'),
(112, 'https://www.youtube.com/@Divine-Al-Quran', 'https://www.youtube.com/watch?v=hN3MGEFEj9k', 'à¦®à¦¦à¦¿à¦¨à¦¾ à¦¥à§‡à¦•à§‡ à¦¸à¦°à¦¾à¦¸à¦°à¦¿', 'https://w7.pngwing.com/pngs/170/592/png-transparent-islamic-madina-thumbnail.png', 0, 1, 32, '2025-05-08 04:46:01'),
(113, 'https://www.youtube.com/@DDIndia', 'https://www.youtube.com/watch?v=9X37FfIrKio', 'à¦¦à§‚à¦° à¦¦à¦°à§à¦¶à¦¨ à¦‡à¦¨à§à¦¡à¦¿à¦¯à¦¼à¦¾', 'https://upload.wikimedia.org/wikipedia/commons/2/21/DD_India_logo.jpg', 0, 1, 41, '2025-05-11 04:27:09'),
(114, 'https://owrcovcrpy.gpcdn.net/bpk-tv/1709/output/1709.m3u8', 'https://owrcovcrpy.gpcdn.net/bpk-tv/1709/output/1709.m3u8', 'BTV', 'https://upload.wikimedia.org/wikipedia/en/thumb/0/02/Bangladesh_Television_Logo.svg/250px-Bangladesh_Television_Logo.svg.png?utm_source=en.wikipedia.org&utm_campaign=parser&utm_content=thumbnail', 1, 1, 1, '2025-05-20 05:57:20'),
(115, 'https://digitalviewonline.com/zee5_by_skshakib/playlist.php?id=ZeeBanglaELE&key=2105260&e=.m3u8', 'https://digitalviewonline.com/zee5_by_skshakib/playlist.php?id=ZeeBanglaELE&key=2105260&e=.m3u8', 'Zee Bangla', NULL, 1, 0, 29, '2025-05-20 05:57:20'),
(118, 'https://amg01448-samsungin-cnnnow-samsungin-4npqg.amagi.tv/playlist/amg01448-samsungin-cnnnow-samsungin/playlist.m3u8', 'https://amg01448-samsungin-cnnnow-samsungin-4npqg.amagi.tv/playlist/amg01448-samsungin-cnnnow-samsungin/playlist.m3u8', 'CNN', NULL, 1, 1, 39, '2025-05-20 15:57:32'),
(119, 'https://cdn4.skygo.mn/live/disk1/BBC_News/HLSv3-FTA/BBC_News-avc1_3000000=8-mp4a_208000_eng=2.m3u8', 'https://cdn4.skygo.mn/live/disk1/BBC_News/HLSv3-FTA/BBC_News-avc1_3000000=8-mp4a_208000_eng=2.m3u8', 'BBC News', NULL, 1, 1, 40, '2025-05-20 15:58:12'),
(123, 'https://travelxp-travelxp-3-nl.samsung.wurl.tv/3000.m3u8', 'https://travelxp-travelxp-3-nl.samsung.wurl.tv/3000.m3u8', 'Travel xp Bangla', NULL, 1, 0, 61, '2025-05-20 15:59:56'),
(126, 'https://app24.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/sbin.stream/playlist.m3u8', 'https://app24.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/sbin.stream/playlist.m3u8', 'ATN Music', NULL, 1, 1, 76, '2025-05-20 16:01:24'),
(131, 'https://live.tsports.com/?bitrate=1024000&channel=tsports_live_2_720&res=720', 'https://live.tsports.com/?bitrate=1024000&channel=tsports_live_2_720&res=720', 'T-Sports', NULL, 1, 0, 57, '2025-05-20 16:03:38'),
(133, 'https://d1e7rcqq4o2ma.cloudfront.net/bpk-tv/1722/output/1722.m3u8', 'https://d1e7rcqq4o2ma.cloudfront.net/bpk-tv/1722/output/1722.m3u8', 'ATN Bangla', NULL, 1, 0, 17, '2025-05-21 07:57:09'),
(134, 'https://europentv.ntvbd.com/livetv', 'https://statiics.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/ntvuk00332211.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9Ni8zMC8yMDI1IDEwOjA5OjI4IEFNJmhhc2hfdmFsdWU9L3lucW', 'NTV 2', NULL, 1, 0, 14, '2025-05-21 07:59:33'),
(135, 'https://boishakhi.sonarbanglatv.com/boishakhi/boishakhitv/index.m3u8', 'https://boishakhi.sonarbanglatv.com/boishakhi/boishakhitv/index.m3u8', 'à¦¬à§ˆà¦¶à¦¾à¦–à§€ à¦Ÿà¦¿à¦­à¦¿', NULL, 1, 0, 15, '2025-05-21 08:02:47'),
(136, 'https://www.ekushey-tv.com/etvlive/', 'https://ekusheyserver.com/etvlivesn.m3u8', 'à¦à¦•à§à¦¶à§‡ à¦Ÿà¦¿à¦­à¦¿', NULL, 1, 1, 18, '2025-05-21 08:04:20'),
(137, 'https://byphdgllyk.gpcdn.net/hls/deeptotv/0_1/index.m3u8', 'https://byphdgllyk.gpcdn.net/hls/deeptotv/index.m3u8', 'à¦¦à§€à¦ªà§à¦¤ à¦Ÿà¦¿à¦­à¦¿', NULL, 1, 1, 19, '2025-05-21 08:05:56'),
(138, 'https://jolshabd.com/live/mbanglatv/playlist.m3u8', 'https://jolshabd.com/live/mbanglatv/playlist.m3u8', 'à¦®à§à¦¯à¦¾à¦œà¦¿à¦• à¦¬à¦¾à¦‚à¦²à¦¾ à¦Ÿà¦¿à¦­à¦¿', NULL, 1, 0, 26, '2025-05-21 08:12:12'),
(139, 'https://dzkyvlfyge.erbvr.com/PeaceTvBangla/tracks-v3a1/mono.m3u8', 'https://dzkyvlfyge.erbvr.com/PeaceTvBangla/tracks-v3a1/mono.m3u8', 'à¦ªà¦¿à¦¸ à¦Ÿà¦¿à¦­à¦¿ à¦¬à¦¾à¦‚à¦²à¦¾', NULL, 1, 1, 28, '2025-05-21 08:13:42'),
(140, 'https://pubads.g.doubleclick.net/ssai/event/JCAm25qkRXiKcK1AJMlvKQ/master.m3u8', 'https://pubads.g.doubleclick.net/ssai/event/JCAm25qkRXiKcK1AJMlvKQ/master.m3u8', 'à¦œà§à¦® à¦®à¦¿à¦‰à¦œà¦¿à¦•', NULL, 1, 0, 79, '2025-05-21 08:23:50'),
(141, 'https://dbcnews.tv/live', 'https://statiics.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/dbcnews.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9Ni8zMC8yMDI1IDEwOjA2OjQ4IEFNJmhhc2hfdmFsdWU9SEY5bzQxem5m', 'DBC 2 TV', NULL, 1, 1, 8, '2025-05-23 01:34:41'),
(142, 'https://www.youtube.com/@warnerbrosentertainment', 'https://www.youtube.com/watch?v=G43NInZfoPE', 'Warner Bros', NULL, 0, 1, 71, '2025-05-24 21:33:21'),
(143, 'https://padmaonline.duckdns.org:8088/restream3/index.m3u8', 'https://padmaonline.duckdns.org:8088/restream3/index.m3u8', 'Live Sports', NULL, 1, 0, 56, '2025-05-30 15:53:59'),
(144, 'https://tvsen5.aynascope.net/tK2BNvT68fHZ/index.m3u8?e=1749091590&u=ee45857d-e890-416d-913a-de05937bafa3&token=932dff5f03b0dd43694c845125e52f84', 'https://tvsen5.aynascope.net/tK2BNvT68fHZ/index.m3u8?e=1749091590&u=ee45857d-e890-416d-913a-de05937bafa3&token=932dff5f03b0dd43694c845125e52f84', 'à¦®à¦¾à¦›à¦°à¦¾à¦™à§à¦—à¦¾ à¦Ÿà§‡à¦²à¦¿à¦­à¦¿à¦¶à¦¨', NULL, 1, 0, 22, '2025-05-30 16:23:15'),
(145, 'https://cloudfrontnet.vercel.app/tplay/playout/209611/master.m3u8', 'https://cloudfrontnet.vercel.app/tplay/playout/209611/master.m3u8', 'à¦¸à¦¨à¦¿ à¦†à¦Ÿ', NULL, 1, 1, 93, '2025-06-04 18:19:42'),
(146, 'https://mcncdndigital.com/balleballetv/index.m3u8', 'https://mcncdndigital.com/balleballetv/index.m3u8', 'Balle Music', NULL, 1, 1, 78, '2025-06-04 18:26:55'),
(147, 'https://digitalviewonline.com/zee5_by_skshakib/playlist.php?id=ZeeBanglaHDELE&key=2105554&e=.m3u8', 'https://digitalviewonline.com/zee5_by_skshakib/playlist.php?id=ZeeBanglaHDELE&key=2105554&e=.m3u8', 'Zee Bang HD', NULL, 1, 0, 30, '2025-06-04 18:51:18'),
(149, 'https://ocko-live.ssl.cdn.cra.cz/channels/ocko/playlist.m3u8', 'https://ocko-live.ssl.cdn.cra.cz/channels/ocko/playlist.m3u8', 'Ocko Muzik', NULL, 1, 1, 92, '2025-06-06 18:51:17'),
(150, 'https://ocko-live.ssl.cdn.cra.cz/channels/ocko_expres/playlist/cze%20/live_lq.m3u8', 'https://ocko-live.ssl.cdn.cra.cz/channels/ocko_expres/playlist/cze%20/live_lq.m3u8', 'Express Hits', NULL, 1, 1, 89, '2025-06-07 03:30:45'),
(151, 'https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0bc8e8376bd8417a1b6761138aa41c26c7309312/bollywood-hd/43e99595-3ab3-4f82-a828-0d32dc308c98/0.m3u8', 'https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0bc8e8376bd8417a1b6761138aa41c26c7309312/bollywood-hd/43e99595-3ab3-4f82-a828-0d32dc308c98/0.m3u8', 'Bollywood HD', NULL, 1, 0, 77, '2025-06-07 03:38:52'),
(152, 'https://mytvbangla.com/0.m3u8', 'https://mytvbangla.com/0.m3u8', 'My TV', NULL, 1, 0, 21, '2025-06-07 03:44:04'),
(153, 'https://stream-us-east-1.getpublica.com/playlist.m3u8?network_id=50', 'https://stream-us-east-1.getpublica.com/playlist.m3u8?network_id=50', 'Kids Flix', NULL, 1, 1, 109, '2025-06-07 03:48:23'),
(154, 'https://live-stream.utkalbongo.com/utkalbongo/stream10/hls/amarbangla.m3u8', 'https://live-stream.utkalbongo.com/utkalbongo/stream10/hls/amarbangla.m3u8', 'à¦†à¦®à¦¾à¦° à¦¬à¦¾à¦‚à¦²à¦¾', NULL, 1, 0, 75, '2025-06-07 04:05:33'),
(155, 'https://live-bangla.akamaized.net/liveabr/playlist.m3u8', 'https://live-bangla.akamaized.net/liveabr/playlist.m3u8', 'Enter10 Bangla', NULL, 1, 1, 88, '2025-06-07 04:11:53'),
(156, 'https://30a-tv.com/music.m3u8', 'https://30a-tv.com/music.m3u8', '30A Music', NULL, 1, 1, 90, '2025-06-07 04:39:43'),
(157, 'https://stream.ecable.tv/afrobeats/index.m3u8', 'https://stream.ecable.tv/afrobeats/index.m3u8', 'Afro Beats', NULL, 1, 1, 91, '2025-06-07 04:41:28'),
(158, 'https://rt-glb.rttv.com/live/rtnews/playlist.m3u8', 'https://rt-glb.rttv.com/live/rtnews/playlist.m3u8', 'RT News', NULL, 1, 1, 47, '2025-06-07 04:54:56'),
(159, 'https://deshitv.deshitv24.net/live/myStream/playlist.m3u8', 'https://deshitv.deshitv24.net/live/myStream/playlist.m3u8', 'à¦¦à§‡à¦¶à¦¿ à¦Ÿà¦¿à¦­à¦¿', NULL, 1, 1, 74, '2025-06-07 05:01:19'),
(160, 'https://mtv.sunplex.live/MAASRANGA-TV/index.m3u8', 'https://mtv.sunplex.live/MAASRANGA-TV/index.m3u8', 'à¦®à¦¾à¦›à¦°à¦¾à¦™à§à¦—à¦¾ à¦Ÿà§‡à¦²à¦¿à¦­à¦¿à¦¶à¦¨ à§¨', NULL, 1, 0, 23, '2025-06-07 05:07:11'),
(161, 'https://iptvbd.live/metv1080/1080.m3u8', 'https://iptvbd.live/metv1080/1080.m3u8', 'Me TV', NULL, 1, 1, 73, '2025-06-07 05:09:20'),
(162, 'https://di-kzbhv8pw.vo.lswcdn.net/sportitalia/sisolocalcio.smil/playlist.m3u8', 'https://di-kzbhv8pw.vo.lswcdn.net/sportitalia/sisolocalcio.smil/playlist.m3u8', 'Sports Italia (Football)', NULL, 1, 0, 110, '2025-06-07 10:20:26'),
(163, 'https://d1e7rcqq4o2ma.cloudfront.net/bpk-tv/1715/output/master.m3u8', 'https://d1e7rcqq4o2ma.cloudfront.net/bpk-tv/1715/output/master.m3u8', 'à¦¬à¦¾à¦‚à¦²à¦¾à¦­à¦¿à¦¶à¦¨', NULL, 1, 0, 111, '2025-06-07 10:29:59'),
(164, 'https://34cfa10eb04f4fc4a7ef2c0e50045c37.mediatailor.eu-central-1.amazonaws.com/v1/master/a70cc84c3dba4ef72d8049e87ba9bd3c406d8e24/Deluxe_Dance_Ads/64733_264_live.m3u8', 'https://34cfa10eb04f4fc4a7ef2c0e50045c37.mediatailor.eu-central-1.amazonaws.com/v1/master/a70cc84c3dba4ef72d8049e87ba9bd3c406d8e24/Deluxe_Dance_Ads/64733_264_live.m3u8', 'Just Dance', NULL, 1, 0, 112, '2025-06-07 10:50:38'),
(165, 'https://tumar-abbu.pages.dev/WavesOTT/DD-BANGLA/tracks-v1a1/mono.m3u8', 'https://tumar-abbu.pages.dev/WavesOTT/DD-BANGLA/tracks-v1a1/mono.m3u8', 'à¦¦à§à¦°à¦¦à¦°à§à¦¶à¦¨ à¦¬à¦¾à¦‚à¦²à¦¾', NULL, 1, 1, 113, '2025-06-07 10:59:55'),
(166, 'https://www.youtube.com/@i24NEWS_EN', 'https://www.youtube.com/watch?v=QYL4WcJJNo0', 'I24 News (Israel)', 'https://upload.wikimedia.org/wikipedia/commons/7/79/LOGO_i24NEWS.png', 0, 0, 45, '2025-06-17 17:49:48'),
(167, 'https://bcovlive-a.akamaihd.net/ecf224f43f3b43e69471a7b626481af0/eu-central-1/5377161796001/profile_0/chunklist.m3u8', 'https://bcovlive-a.akamaihd.net/ecf224f43f3b43e69471a7b626481af0/eu-central-1/5377161796001/profile_0/chunklist.m3u8', 'I24 News (Israel) Live', NULL, 1, 1, 114, '2025-06-20 13:23:47'),
(168, 'https://static.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/boishakhitv-org.stream/playlist.m3u8?wmsAuthSign=', 'https://static.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDDEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFsaWRtaW51aiPhnPTI2/boishakhitv-org.stream/playlist.m3u8?wmsAuthSign=', 'à¦¬à§ˆà¦¶à¦¾à¦–à§€ à¦Ÿà¦¿à¦­à¦¿ à§©', NULL, 1, 0, 115, '2025-06-20 16:04:51'),
(169, 'https://rumble.com/live-hls/6s62fv/playlist.m3u8?', 'https://rumble.com/live-hls/6s62fv/playlist.m3u8?', 'PressTV (Iran)', NULL, 1, 1, 44, '2025-06-20 16:27:34'),
(170, 'https://www.youtube.com/@IsraelEnglishNews', 'https://www.youtube.com/watch?v=zm2zxnU9OIg', 'ILTV Israel News', 'https://alpha.uscreencdn.com/396xnull/images/logotypes/36012/1000.1669811606.png', 0, 0, 43, '2025-06-20 16:35:41'),
(171, 'https://www.btvlive.gov.bd/channel/BTV-News', 'https://www.btvlive.gov.bd/streams/37f2df30-3edf-42f3-a2ee-6185002c841c/BD/d96eb7f4-83c2-4472-9597-3568390a8ebf/index.m3u8', 'BTV News', NULL, 1, 0, 116, '2025-06-30 09:16:27'),
(173, 'https://www.youtube.com/@ChanneliNews', 'https://www.youtube.com/watch?v=o-VdcyMenYM', 'à¦šà§à¦¯à¦¾à¦¨à§‡à¦² à¦†à¦‡', 'https://images.seeklogo.com/logo-png/42/1/channel-i-logo-png_seeklogo-427745.png', 0, 0, 12, '2025-07-01 12:02:18');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `live_streams`
--
ALTER TABLE `live_streams`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `youtube_url` (`youtube_url`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `live_streams`
--
ALTER TABLE `live_streams`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=174;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
