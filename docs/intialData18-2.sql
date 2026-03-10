-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               8.4.3 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.8.0.6908
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for nexopos_v4
CREATE DATABASE IF NOT EXISTS `nexopos_v4` /*!40100 DEFAULT CHARACTER SET utf16 COLLATE utf16_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `nexopos_v4`;

-- Dumping structure for table nexopos_v4.ns_failed_jobs
CREATE TABLE IF NOT EXISTS `ns_failed_jobs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `connection` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `queue` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `exception` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `failed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_failed_jobs: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_jobs
CREATE TABLE IF NOT EXISTS `ns_jobs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `queue` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempts` tinyint unsigned NOT NULL,
  `reserved_at` int unsigned DEFAULT NULL,
  `available_at` int unsigned NOT NULL,
  `created_at` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ns_jobs_queue_index` (`queue`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_jobs: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_migrations
CREATE TABLE IF NOT EXISTS `ns_migrations` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `migration` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch` int NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_migrations: ~92 rows (approximately)
INSERT INTO `ns_migrations` (`id`, `migration`, `batch`, `type`) VALUES
	(1, '2018_08_08_100000_create_telescope_entries_table', 1, NULL),
	(2, '2019_12_14_000001_create_personal_access_tokens_table', 1, NULL),
	(3, '2022_10_28_123458_setup_migration_table', 1, NULL),
	(4, '2014_10_13_000000_create_users_table', 0, 'core'),
	(5, '2017_07_28_130434_create_roles_table', 0, 'core'),
	(6, '2017_07_28_130610_create_permissions_table', 0, 'core'),
	(7, '2018_08_08_100000_create_telescope_entries_table', 0, 'core'),
	(8, '2019_08_19_000000_create_failed_jobs_table', 0, 'core'),
	(9, '2020_06_20_000000_create_permissions', 0, 'core'),
	(10, '2020_06_20_000000_create_roles', 0, 'core'),
	(11, '2020_10_11_122857_create_jobs_table', 0, 'core'),
	(12, '2020_11_04_124040_add_new_customers_permission_nov4', 0, 'core'),
	(13, '2020_11_11_151614_nov11_create_nexopos_users_attributes_table', 0, 'core'),
	(14, '2020_11_12_205243_nov12_create_order_permission', 0, 'core'),
	(15, '2020_11_25_203531_nov25_fix_report_permissions_attribution', 0, 'core'),
	(16, '2020_12_08_210001_dec8add_new_permissions', 0, 'core'),
	(17, '2020_12_19_221434_create_nexopos_modules_migrations_table', 0, 'core'),
	(18, '2021_01_07_143635_add_new_customer_permission_janv7_21', 0, 'core'),
	(19, '2021_03_09_165538_create_new_permissions_march_9', 0, 'core'),
	(20, '2021_05_25_175424_update_add_payment_type_permissions', 0, 'core'),
	(21, '2021_05_28_114827_create_new_report_permissions_may28', 0, 'core'),
	(22, '2021_06_24_053134_update_permissions_jun24', 0, 'core'),
	(23, '2021_07_31_153029_update_dashboard_days_report_jul31', 0, 'core'),
	(24, '2014_10_12_000000_create_medias_table', 0, 'create'),
	(25, '2017_12_29_174613_create_options_table', 0, 'create'),
	(26, '2020_06_20_000000_create_customers_groups_table', 0, 'create'),
	(27, '2020_06_20_000000_create_customers_table', 0, 'create'),
	(28, '2020_06_20_000000_create_expenses_categories_table', 0, 'create'),
	(29, '2020_06_20_000000_create_expenses_table', 0, 'create'),
	(30, '2020_06_20_000000_create_orders_addresses_table', 0, 'create'),
	(31, '2020_06_20_000000_create_orders_coupons_table', 0, 'create'),
	(32, '2020_06_20_000000_create_orders_metas_table', 0, 'create'),
	(33, '2020_06_20_000000_create_orders_payment_table', 0, 'create'),
	(34, '2020_06_20_000000_create_orders_products_table', 0, 'create'),
	(35, '2020_06_20_000000_create_orders_table', 0, 'create'),
	(36, '2020_06_20_000000_create_procurements_products_table', 0, 'create'),
	(37, '2020_06_20_000000_create_procurements_table', 0, 'create'),
	(38, '2020_06_20_000000_create_products_categories_table', 0, 'create'),
	(39, '2020_06_20_000000_create_products_gallery_table', 0, 'create'),
	(40, '2020_06_20_000000_create_products_history_table', 0, 'create'),
	(41, '2020_06_20_000000_create_products_metas_table', 0, 'create'),
	(42, '2020_06_20_000000_create_products_table', 0, 'create'),
	(43, '2020_06_20_000000_create_products_taxes_table', 0, 'create'),
	(44, '2020_06_20_000000_create_products_unit_quantities', 0, 'create'),
	(45, '2020_06_20_000000_create_providers_table', 0, 'create'),
	(46, '2020_06_20_000000_create_registers_history_table', 0, 'create'),
	(47, '2020_06_20_000000_create_registers_table', 0, 'create'),
	(48, '2020_06_20_000000_create_rewards_system_rules_table', 0, 'create'),
	(49, '2020_06_20_000000_create_rewards_system_table', 0, 'create'),
	(50, '2020_06_20_000000_create_taxes_table', 0, 'create'),
	(51, '2020_06_20_000000_create_units_group_table', 0, 'create'),
	(52, '2020_06_20_000000_create_units_table', 0, 'create'),
	(53, '2020_08_01_143801_create_customers_coupons_table', 0, 'create'),
	(54, '2020_10_10_224639_create_dashboard_table', 0, 'create'),
	(55, '2020_10_11_074631_create_nexopos_notifications_table', 0, 'create'),
	(56, '2020_10_17_231628_create_nexopos_orders_storage', 0, 'create'),
	(57, '2020_10_29_150642_create_nexopos_expenses_history_table', 0, 'create'),
	(58, '2020_11_17_120204_nov17_add_fields_to_nexopos_orders_products_table', 0, 'create'),
	(59, '2020_12_11_210734_create_nexopos_dashboard_months_table', 0, 'create'),
	(60, '2021_01_23_225101_create_coupons_table', 0, 'create'),
	(61, '2021_01_23_225713_create_customers_rewards_table', 0, 'create'),
	(62, '2021_02_21_144532_create_orders_instalments_table', 0, 'create'),
	(63, '2021_02_23_004748_create_new_instalments_permissions', 0, 'create'),
	(64, '2021_05_25_131104_create_nexopos_payments_types_table', 0, 'create'),
	(65, '2022_01_20_202253_create_user_role_relations_table', 0, 'create'),
	(66, '2022_05_13_142039_create_products_group_items_table', 0, 'create'),
	(67, '2022_10_12_083552_update_register_history_oct12_22', 0, 'create'),
	(68, '2022_10_28_093041_update_expense_table28_oct22', 0, 'create'),
	(69, '2022_11_25_071626_create_users_widgets_table', 0, 'create'),
	(70, '2023_10_31_120602_stock_history_detailed', 0, 'create'),
	(71, '2024_04_29_214452_create_transaction_balance_days_table', 0, 'create'),
	(72, '2024_04_29_214459_create_transaction_balance_months_table', 0, 'create'),
	(73, '2024_09_02_023528_create_accounting_table_actions', 0, 'create'),
	(74, '2025_01_22_113445_create_nexopos_orders_settings_table', 0, 'create'),
	(75, '2022_11_28_000259_v5x_general_database_update', 0, 'update'),
	(76, '2022_11_30_224820_update_orders_coupon_table', 0, 'update'),
	(77, '2023_02_14_123130_update_product_history_table14_fev23', 0, 'update'),
	(78, '2023_03_16_214039_update_expense_table', 0, 'update'),
	(79, '2023_07_30_235454_fix_wrong_purchase_price_32_jul_23', 0, 'update'),
	(80, '2024_01_26_075544_update_expenses_and_accounts', 0, 'update'),
	(81, '2024_01_26_165001_update_transaction_histories', 0, 'update'),
	(82, '2024_02_29_035014_update_add_new_pos_permissions', 0, 'update'),
	(83, '2024_03_12_235545_update_v5_2_0', 0, 'update'),
	(84, '2024_03_25_150121_update_user_widget', 0, 'update'),
	(85, '2024_04_04_234202_update_transactions_adds_missing_field', 0, 'update'),
	(86, '2024_04_18_084130_update_transactions_history_table', 0, 'update'),
	(87, '2024_04_29_110922_update_cash_registers_table', 0, 'update'),
	(88, '2024_06_14_013012_update_to_nexopos_v53', 0, 'update'),
	(89, '2024_06_28_183019_update_add_permission_to_products_unit_conversion', 0, 'update'),
	(90, '2024_07_19_145205_update_nexopos_transactions_accounts', 0, 'update'),
	(91, '2024_07_29_171109_update_orders_total_cogs', 0, 'update'),
	(92, '2025_01_27_093541_remove_deprecated_options', 0, 'update');

-- Dumping structure for table nexopos_v4.ns_nexopos_coupons
CREATE TABLE IF NOT EXISTS `ns_nexopos_coupons` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'discount',
  `discount_value` float NOT NULL DEFAULT '0',
  `valid_until` datetime DEFAULT NULL,
  `minimum_cart_value` float DEFAULT '0',
  `maximum_cart_value` float DEFAULT '0',
  `valid_hours_start` datetime DEFAULT NULL,
  `valid_hours_end` datetime DEFAULT NULL,
  `limit_usage` float NOT NULL DEFAULT '0',
  `groups_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customers_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_coupons: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_coupons_categories
CREATE TABLE IF NOT EXISTS `ns_nexopos_coupons_categories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_id` int NOT NULL,
  `category_id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_coupons_categories: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_coupons_customers
CREATE TABLE IF NOT EXISTS `ns_nexopos_coupons_customers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_id` int NOT NULL,
  `customer_id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_coupons_customers: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_coupons_customers_groups
CREATE TABLE IF NOT EXISTS `ns_nexopos_coupons_customers_groups` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_id` int NOT NULL,
  `group_id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_coupons_customers_groups: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_coupons_products
CREATE TABLE IF NOT EXISTS `ns_nexopos_coupons_products` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_id` int NOT NULL,
  `product_id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_coupons_products: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_customers_account_history
CREATE TABLE IF NOT EXISTS `ns_nexopos_customers_account_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `order_id` int DEFAULT NULL,
  `previous_amount` double NOT NULL DEFAULT '0',
  `amount` double NOT NULL DEFAULT '0',
  `next_amount` double NOT NULL DEFAULT '0',
  `operation` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author` int NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_customers_account_history: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_customers_addresses
CREATE TABLE IF NOT EXISTS `ns_nexopos_customers_addresses` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `first_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pobox` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_customers_addresses: ~2 rows (approximately)
INSERT INTO `ns_nexopos_customers_addresses` (`id`, `customer_id`, `type`, `email`, `first_name`, `last_name`, `phone`, `address_1`, `address_2`, `country`, `city`, `pobox`, `company`, `uuid`, `author`, `created_at`, `updated_at`) VALUES
	(1, 86, 'shipping', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, '2025-02-18 12:40:50', '2025-02-18 12:40:50'),
	(2, 86, 'billing', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, '2025-02-18 12:40:50', '2025-02-18 12:40:50');

-- Dumping structure for table nexopos_v4.ns_nexopos_customers_coupons
CREATE TABLE IF NOT EXISTS `ns_nexopos_customers_coupons` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `usage` int NOT NULL DEFAULT '0',
  `limit_usage` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `coupon_id` int NOT NULL,
  `customer_id` int NOT NULL,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_customers_coupons: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_customers_groups
CREATE TABLE IF NOT EXISTS `ns_nexopos_customers_groups` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `reward_system_id` int DEFAULT '0',
  `minimal_credit_payment` int NOT NULL DEFAULT '0',
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_customers_groups: ~1 rows (approximately)
INSERT INTO `ns_nexopos_customers_groups` (`id`, `name`, `description`, `reward_system_id`, `minimal_credit_payment`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'LadiesMaterial', NULL, NULL, 0, 85, NULL, '2025-02-18 12:40:43', '2025-02-18 12:40:43');

-- Dumping structure for table nexopos_v4.ns_nexopos_customers_rewards
CREATE TABLE IF NOT EXISTS `ns_nexopos_customers_rewards` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `reward_id` int NOT NULL,
  `reward_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `points` float NOT NULL,
  `target` float NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_customers_rewards: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_dashboard_days
CREATE TABLE IF NOT EXISTS `ns_nexopos_dashboard_days` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `total_unpaid_orders` float NOT NULL DEFAULT '0',
  `day_unpaid_orders` float NOT NULL DEFAULT '0',
  `total_unpaid_orders_count` float NOT NULL DEFAULT '0',
  `day_unpaid_orders_count` float NOT NULL DEFAULT '0',
  `total_paid_orders` float NOT NULL DEFAULT '0',
  `day_paid_orders` float NOT NULL DEFAULT '0',
  `total_paid_orders_count` float NOT NULL DEFAULT '0',
  `day_paid_orders_count` float NOT NULL DEFAULT '0',
  `total_partially_paid_orders` float NOT NULL DEFAULT '0',
  `day_partially_paid_orders` float NOT NULL DEFAULT '0',
  `total_partially_paid_orders_count` float NOT NULL DEFAULT '0',
  `day_partially_paid_orders_count` float NOT NULL DEFAULT '0',
  `total_income` float NOT NULL DEFAULT '0',
  `day_income` float NOT NULL DEFAULT '0',
  `total_discounts` float NOT NULL DEFAULT '0',
  `day_discounts` float NOT NULL DEFAULT '0',
  `day_taxes` float NOT NULL DEFAULT '0',
  `total_taxes` float NOT NULL DEFAULT '0',
  `total_wasted_goods_count` float NOT NULL DEFAULT '0',
  `day_wasted_goods_count` float NOT NULL DEFAULT '0',
  `total_wasted_goods` float NOT NULL DEFAULT '0',
  `day_wasted_goods` float NOT NULL DEFAULT '0',
  `total_expenses` float NOT NULL DEFAULT '0',
  `day_expenses` float NOT NULL DEFAULT '0',
  `day_of_year` int NOT NULL DEFAULT '0',
  `range_starts` datetime DEFAULT NULL,
  `range_ends` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_dashboard_days: ~2 rows (approximately)
INSERT INTO `ns_nexopos_dashboard_days` (`id`, `total_unpaid_orders`, `day_unpaid_orders`, `total_unpaid_orders_count`, `day_unpaid_orders_count`, `total_paid_orders`, `day_paid_orders`, `total_paid_orders_count`, `day_paid_orders_count`, `total_partially_paid_orders`, `day_partially_paid_orders`, `total_partially_paid_orders_count`, `day_partially_paid_orders_count`, `total_income`, `day_income`, `total_discounts`, `day_discounts`, `day_taxes`, `total_taxes`, `total_wasted_goods_count`, `day_wasted_goods_count`, `total_wasted_goods`, `day_wasted_goods`, `total_expenses`, `day_expenses`, `day_of_year`, `range_starts`, `range_ends`) VALUES
	(1, 0, 0, 0, 0, 2550, 2550, 3, 3, 0, 0, 0, 0, 1250, 1250, 25, 25, 0, 0, 0, 0, 0, 0, 1250, 1250, 49, '2025-02-18 00:00:00', '2025-02-18 23:59:59'),
	(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, '2025-02-17 00:00:00', '2025-02-17 23:59:59');

-- Dumping structure for table nexopos_v4.ns_nexopos_dashboard_months
CREATE TABLE IF NOT EXISTS `ns_nexopos_dashboard_months` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `month_taxes` float NOT NULL DEFAULT '0',
  `month_unpaid_orders` float NOT NULL DEFAULT '0',
  `month_unpaid_orders_count` float NOT NULL DEFAULT '0',
  `month_paid_orders` float NOT NULL DEFAULT '0',
  `month_paid_orders_count` float NOT NULL DEFAULT '0',
  `month_partially_paid_orders` float NOT NULL DEFAULT '0',
  `month_partially_paid_orders_count` float NOT NULL DEFAULT '0',
  `month_income` float NOT NULL DEFAULT '0',
  `month_discounts` float NOT NULL DEFAULT '0',
  `month_wasted_goods_count` float NOT NULL DEFAULT '0',
  `month_wasted_goods` float NOT NULL DEFAULT '0',
  `month_expenses` float NOT NULL DEFAULT '0',
  `total_wasted_goods` float NOT NULL DEFAULT '0',
  `total_unpaid_orders` float NOT NULL DEFAULT '0',
  `total_unpaid_orders_count` float NOT NULL DEFAULT '0',
  `total_paid_orders` float NOT NULL DEFAULT '0',
  `total_paid_orders_count` float NOT NULL DEFAULT '0',
  `total_partially_paid_orders` float NOT NULL DEFAULT '0',
  `total_partially_paid_orders_count` float NOT NULL DEFAULT '0',
  `total_income` float NOT NULL DEFAULT '0',
  `total_discounts` float NOT NULL DEFAULT '0',
  `total_taxes` float NOT NULL DEFAULT '0',
  `total_wasted_goods_count` float NOT NULL DEFAULT '0',
  `total_expenses` float NOT NULL DEFAULT '0',
  `month_of_year` int NOT NULL DEFAULT '0',
  `range_starts` datetime NOT NULL,
  `range_ends` datetime NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_dashboard_months: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_dashboard_weeks
CREATE TABLE IF NOT EXISTS `ns_nexopos_dashboard_weeks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `total_gross_income` float NOT NULL DEFAULT '0',
  `total_taxes` float NOT NULL DEFAULT '0',
  `total_expenses` float NOT NULL DEFAULT '0',
  `total_net_income` float NOT NULL DEFAULT '0',
  `week_number` int NOT NULL DEFAULT '0',
  `range_starts` datetime DEFAULT NULL,
  `range_ends` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_dashboard_weeks: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_medias
CREATE TABLE IF NOT EXISTS `ns_nexopos_medias` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `extension` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_medias_name_unique` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_medias: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_modules_migrations
CREATE TABLE IF NOT EXISTS `ns_nexopos_modules_migrations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `namespace` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_modules_migrations: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_notifications
CREATE TABLE IF NOT EXISTS `ns_nexopos_notifications` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#',
  `source` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system',
  `dismissable` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_notifications: ~0 rows (approximately)
INSERT INTO `ns_nexopos_notifications` (`id`, `user_id`, `identifier`, `title`, `description`, `url`, `source`, `dismissable`, `created_at`, `updated_at`) VALUES
	(22, 85, 'ns.notifications.workers-disabled', 'Task Scheduling Disabled', 'NexoPOS is unable to schedule background tasks. This might restrict necessary features. Click here to learn how to fix it.', 'https://my.nexopos.com/en/documentation/troubleshooting/workers-or-async-requests-disabled?utm_source=nexopos&utm_campaign=warning&utm_medium=app', 'system', 1, '2025-02-18 17:02:04', '2025-02-18 17:22:07'),
	(23, 85, 'ns.notifications.cron-disabled', 'Cron Disabled', 'Cron jobs aren\'t configured correctly on NexoPOS. This might restrict necessary features. Click here to learn how to fix it.', 'https://my.nexopos.com/en/documentation/troubleshooting/workers-or-async-requests-disabled?utm_source=nexopos&utm_campaign=warning&utm_medium=app', 'system', 1, '2025-02-18 17:02:05', '2025-02-18 17:22:07');

-- Dumping structure for table nexopos_v4.ns_nexopos_options
CREATE TABLE IF NOT EXISTS `ns_nexopos_options` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` text COLLATE utf8mb4_unicode_ci,
  `expire_on` datetime DEFAULT NULL,
  `array` tinyint(1) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_options: ~39 rows (approximately)
INSERT INTO `ns_nexopos_options` (`id`, `user_id`, `key`, `value`, `expire_on`, `array`, `created_at`, `updated_at`) VALUES
	(1, NULL, 'ns_registration_enabled', 'no', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(2, NULL, 'ns_store_name', 'Zeemart Super Bazar', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 12:45:50'),
	(3, NULL, 'ns_pos_allow_decimal_quantities', 'yes', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(4, NULL, 'ns_pos_quick_product', 'yes', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(5, NULL, 'ns_pos_show_quantity', 'yes', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(6, NULL, 'ns_currency_precision', '2', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(7, NULL, 'ns_pos_hide_empty_categories', 'no', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 16:55:07'),
	(8, NULL, 'ns_pos_unit_price_ediable', 'yes', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(9, NULL, 'ns_pos_order_types', '["takeaway","delivery"]', NULL, 1, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(10, NULL, 'ns_pos_registers_default_change_payment_type', '1', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(11, NULL, 'ns_store_language', 'en', NULL, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(12, NULL, 'ns_store_address', 'opposite Royal palace,pkd road', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 17:16:04'),
	(13, NULL, 'ns_store_city', 'yavatmal', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 03:03:35'),
	(14, NULL, 'ns_store_email', 'Opposite Royal Palace,pkd road,Yavatmal', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 17:22:01'),
	(15, NULL, 'ns_currency_symbol', 'INR', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 03:03:35'),
	(16, NULL, 'ns_currency_iso', 'INR', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 09:02:16'),
	(17, NULL, 'ns_currency_position', 'after', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 09:03:03'),
	(18, NULL, 'ns_currency_prefered', 'iso', NULL, 0, '2025-02-18 03:03:35', '2025-02-18 03:03:35'),
	(19, NULL, 'ns_date_format', 'Y-m-d', NULL, 0, '2025-02-18 03:04:50', '2025-02-18 03:04:50'),
	(20, NULL, 'ns_datetime_format', 'Y-m-d H:i', NULL, 0, '2025-02-18 03:04:50', '2025-02-18 03:04:50'),
	(21, NULL, 'ns_datetime_timezone', 'Asia/Calcutta', NULL, 0, '2025-02-18 03:04:50', '2025-02-18 03:04:50'),
	(22, NULL, 'ns_invoice_receipt_template', 'default', NULL, 0, '2025-02-18 12:46:45', '2025-02-18 12:46:45'),
	(23, NULL, 'ns_invoice_receipt_footer', 'Thank You  For Shopping With Us!!!', NULL, 0, '2025-02-18 12:46:45', '2025-02-18 12:46:45'),
	(24, NULL, 'ns_pos_layout', 'clothing_shop', NULL, 0, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(25, NULL, 'ns_pos_complete_sale_audio', 'http://nexopos.test/audio/cash-sound.mp3', NULL, 0, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(26, NULL, 'ns_pos_new_item_audio', 'http://nexopos.test/audio/pop.mp3', NULL, 0, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(27, NULL, 'ns_pos_keyboard_cancel_order', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(28, NULL, 'ns_pos_keyboard_hold_order', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(29, NULL, 'ns_pos_keyboard_create_customer', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(30, NULL, 'ns_pos_keyboard_payment', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(31, NULL, 'ns_pos_keyboard_shipping', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(32, NULL, 'ns_pos_keyboard_note', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(33, NULL, 'ns_pos_keyboard_order_type', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(34, NULL, 'ns_pos_keyboard_fullscreen', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(35, NULL, 'ns_pos_keyboard_quick_search', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(36, NULL, 'ns_pos_keyboard_toggle_merge', '[]', NULL, 1, '2025-02-18 12:47:49', '2025-02-18 12:47:49'),
	(37, NULL, 'ns_pos_printing_document', 'receipt', NULL, 0, '2025-02-18 12:51:53', '2025-02-18 12:51:53'),
	(38, NULL, 'ns_pos_printing_enabled_for', 'all_orders', NULL, 0, '2025-02-18 12:51:53', '2025-02-18 12:51:53'),
	(39, NULL, 'ns_pos_quick_product_default_unit', '1', NULL, 0, '2025-02-18 12:51:53', '2025-02-18 12:51:53'),
	(40, NULL, 'ns_store_phone', '8459232989', NULL, 0, '2025-02-18 17:07:49', '2025-02-18 17:07:49'),
	(41, NULL, 'ns_store_additional', 'Test Additional Info', NULL, 0, '2025-02-18 17:07:49', '2025-02-18 17:07:49'),
	(42, NULL, 'ns_invoice_receipt_column_a', '{store_email}', NULL, 0, '2025-02-18 17:12:39', '2025-02-18 17:20:46'),
	(43, NULL, 'ns_invoice_receipt_column_b', '{order_date}', NULL, 0, '2025-02-18 17:12:39', '2025-02-18 17:12:39');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `description` text COLLATE utf8mb4_unicode_ci,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `process_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `delivery_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `discount` float NOT NULL DEFAULT '0',
  `discount_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `support_instalments` tinyint(1) NOT NULL DEFAULT '1',
  `discount_percentage` float DEFAULT NULL,
  `shipping` float NOT NULL DEFAULT '0',
  `shipping_rate` float NOT NULL DEFAULT '0',
  `shipping_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_without_tax` float NOT NULL DEFAULT '0',
  `subtotal` float NOT NULL DEFAULT '0',
  `total_with_tax` float NOT NULL DEFAULT '0',
  `total_coupons` float NOT NULL DEFAULT '0',
  `total_cogs` float NOT NULL DEFAULT '0',
  `total` float NOT NULL DEFAULT '0',
  `tax_value` float NOT NULL DEFAULT '0',
  `products_tax_value` double NOT NULL DEFAULT '0',
  `tax_group_id` int DEFAULT NULL,
  `tax_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tendered` float NOT NULL DEFAULT '0',
  `change` float NOT NULL DEFAULT '0',
  `final_payment_date` datetime DEFAULT NULL,
  `total_instalments` int NOT NULL DEFAULT '0',
  `customer_id` int NOT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note_visibility` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `register_id` int DEFAULT NULL,
  `voidance_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders: ~3 rows (approximately)
INSERT INTO `ns_nexopos_orders` (`id`, `description`, `code`, `title`, `type`, `payment_status`, `process_status`, `delivery_status`, `discount`, `discount_type`, `support_instalments`, `discount_percentage`, `shipping`, `shipping_rate`, `shipping_type`, `total_without_tax`, `subtotal`, `total_with_tax`, `total_coupons`, `total_cogs`, `total`, `tax_value`, `products_tax_value`, `tax_group_id`, `tax_type`, `tendered`, `change`, `final_payment_date`, `total_instalments`, `customer_id`, `note`, `note_visibility`, `author`, `uuid`, `register_id`, `voidance_reason`, `created_at`, `updated_at`) VALUES
	(1, NULL, '250218-001', NULL, 'takeaway', 'paid', 'not-available', 'not-available', 0, NULL, 1, 0, 0, 0, NULL, 550, 550, 550, 0, 0, 550, 0, 0, NULL, '0', 550, 0, NULL, 0, 86, NULL, 'hidden', 85, NULL, NULL, NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, NULL, '250218-002', NULL, 'takeaway', 'paid', 'not-available', 'not-available', 25, 'flat', 1, 0, 0, 0, NULL, 700, 725, 700, 0, 0, 700, 0, 0, NULL, '0', 700, 0, NULL, 0, 86, NULL, 'hidden', 85, NULL, NULL, NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(3, NULL, '250218-003', NULL, 'takeaway', 'paid', 'not-available', 'not-available', 0, NULL, 1, 0, 0, 0, NULL, 1300, 1300, 1300, 0, 0, 1300, 0, 0, NULL, '0', 1300, 0, NULL, 0, 86, NULL, 'hidden', 85, NULL, NULL, NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_addresses
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_addresses` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pobox` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_addresses: ~6 rows (approximately)
INSERT INTO `ns_nexopos_orders_addresses` (`id`, `order_id`, `type`, `first_name`, `last_name`, `phone`, `address_1`, `email`, `address_2`, `country`, `city`, `pobox`, `company`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 1, 'shipping', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, 1, 'billing', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(3, 2, 'shipping', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(4, 2, 'billing', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(5, 3, 'shipping', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14'),
	(6, 3, 'billing', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 85, NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_count
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_count` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `count` int NOT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_count: ~1 rows (approximately)
INSERT INTO `ns_nexopos_orders_count` (`id`, `count`, `date`) VALUES
	(1, 4, '2025-02-18 00:00:00');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_coupons
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_coupons` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_coupon_id` int NOT NULL,
  `coupon_id` int NOT NULL,
  `order_id` int NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_value` float NOT NULL,
  `minimum_cart_value` float NOT NULL DEFAULT '0',
  `maximum_cart_value` float NOT NULL DEFAULT '0',
  `limit_usage` int NOT NULL DEFAULT '0',
  `value` float NOT NULL DEFAULT '0',
  `author` int NOT NULL,
  `counted` tinyint(1) NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_coupons: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_instalments
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_instalments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `amount` float NOT NULL DEFAULT '0',
  `order_id` int DEFAULT NULL,
  `paid` tinyint(1) NOT NULL DEFAULT '0',
  `payment_id` int DEFAULT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_instalments: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_metas
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_metas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_metas: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_payments
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_payments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `value` float NOT NULL DEFAULT '0',
  `author` int NOT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_payments: ~3 rows (approximately)
INSERT INTO `ns_nexopos_orders_payments` (`id`, `order_id`, `value`, `author`, `identifier`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 1, 550, 85, 'cash-payment', NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, 2, 700, 85, 'cash-payment', NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(3, 3, 1300, 85, 'cash-payment', NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_products
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_products` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal',
  `product_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'product',
  `product_id` int NOT NULL,
  `order_id` int NOT NULL,
  `unit_id` int NOT NULL,
  `unit_quantity_id` int NOT NULL,
  `product_category_id` int NOT NULL,
  `procurement_product_id` int DEFAULT NULL,
  `tax_group_id` int NOT NULL DEFAULT '0',
  `tax_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'sold',
  `return_observations` text COLLATE utf8mb4_unicode_ci,
  `return_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none',
  `discount` float NOT NULL DEFAULT '0',
  `quantity` float NOT NULL,
  `discount_percentage` float NOT NULL DEFAULT '0',
  `unit_price` float NOT NULL DEFAULT '0',
  `price_with_tax` float NOT NULL DEFAULT '0',
  `price_without_tax` float NOT NULL DEFAULT '0',
  `wholesale_tax_value` float NOT NULL DEFAULT '0',
  `sale_tax_value` float NOT NULL DEFAULT '0',
  `tax_value` float NOT NULL DEFAULT '0',
  `rate` double NOT NULL DEFAULT '0',
  `total_price` float NOT NULL DEFAULT '0',
  `total_price_with_tax` float NOT NULL DEFAULT '0',
  `total_price_without_tax` float NOT NULL DEFAULT '0',
  `total_purchase_price` float NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_products: ~3 rows (approximately)
INSERT INTO `ns_nexopos_orders_products` (`id`, `name`, `unit_name`, `mode`, `product_type`, `product_id`, `order_id`, `unit_id`, `unit_quantity_id`, `product_category_id`, `procurement_product_id`, `tax_group_id`, `tax_type`, `uuid`, `status`, `return_observations`, `return_condition`, `discount_type`, `discount`, `quantity`, `discount_percentage`, `unit_price`, `price_with_tax`, `price_without_tax`, `wholesale_tax_value`, `sale_tax_value`, `tax_value`, `rate`, `total_price`, `total_price_with_tax`, `total_price_without_tax`, `total_purchase_price`, `created_at`, `updated_at`) VALUES
	(1, 'regular cotton', 'Dress', 'normal', 'product', 16, 1, 1, 17, 7, NULL, 0, 'disabled', NULL, 'sold', NULL, NULL, 'percentage', 0, 1, 0, 550, 0, 0, 0, 0, 0, 0, 550, 0, 0, 0, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, 'curetia cotton', 'Dress', 'normal', 'product', 17, 2, 1, 18, 7, NULL, 0, 'disabled', NULL, 'sold', NULL, NULL, 'percentage', 0, 1, 0, 725, 0, 0, 0, 0, 0, 0, 725, 0, 0, 0, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(3, 'Shiny Morpankhi', 'Dress', 'normal', 'product', 7, 3, 1, 8, 2, NULL, 0, 'disabled', NULL, 'sold', NULL, NULL, 'percentage', 0, 1, 0, 1300, 0, 0, 0, 0, 0, 0, 1300, 0, 0, 0, '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_products_refunds
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_products_refunds` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `order_refund_id` int NOT NULL,
  `order_product_id` int NOT NULL,
  `unit_id` int NOT NULL,
  `product_id` int NOT NULL,
  `unit_price` float NOT NULL,
  `tax_value` float NOT NULL DEFAULT '0',
  `quantity` float NOT NULL,
  `total_price` float NOT NULL,
  `condition` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_products_refunds: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_refunds
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_refunds` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `author` int NOT NULL,
  `total` float NOT NULL,
  `tax_value` float NOT NULL DEFAULT '0',
  `shipping` float NOT NULL,
  `payment_method` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_refunds: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_settings
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_settings` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_settings: ~6 rows (approximately)
INSERT INTO `ns_nexopos_orders_settings` (`id`, `order_id`, `key`, `value`, `created_at`, `updated_at`) VALUES
	(1, 1, 'ns_pos_price_with_tax', NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, 1, 'ns_pos_vat', NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(3, 2, 'ns_pos_price_with_tax', NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(4, 2, 'ns_pos_vat', NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(5, 3, 'ns_pos_price_with_tax', NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14'),
	(6, 3, 'ns_pos_vat', NULL, '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_storage
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_storage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` int DEFAULT NULL,
  `unit_quantity_id` int DEFAULT NULL,
  `unit_id` int DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `session_identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_storage: ~3 rows (approximately)
INSERT INTO `ns_nexopos_orders_storage` (`id`, `product_id`, `unit_quantity_id`, `unit_id`, `quantity`, `session_identifier`, `created_at`, `updated_at`) VALUES
	(1, 16, 17, 1, 1, 'lkeECamcS1', '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, 17, 18, 1, 1, 'quqrhSuxlp', '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(3, 7, 8, 1, 1, 'J7aA8C2ZSH', '2025-02-18 12:51:14', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_orders_taxes
CREATE TABLE IF NOT EXISTS `ns_nexopos_orders_taxes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tax_id` int DEFAULT NULL,
  `order_id` int DEFAULT NULL,
  `rate` double NOT NULL,
  `tax_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tax_value` double NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_orders_taxes: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_payments_types
CREATE TABLE IF NOT EXISTS `ns_nexopos_payments_types` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `label` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `readonly` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_payments_types: ~3 rows (approximately)
INSERT INTO `ns_nexopos_payments_types` (`id`, `label`, `identifier`, `priority`, `description`, `author`, `active`, `readonly`, `created_at`, `updated_at`) VALUES
	(1, 'Cash', 'cash-payment', 0, NULL, 85, 1, 1, '2025-02-18 03:01:17', '2025-02-18 03:01:17'),
	(2, 'Bank Payment', 'bank-payment', 0, NULL, 85, 1, 1, '2025-02-18 03:01:17', '2025-02-18 03:01:17'),
	(3, 'Customer Account', 'account-payment', 0, NULL, 85, 1, 1, '2025-02-18 03:01:17', '2025-02-18 03:01:17');

-- Dumping structure for table nexopos_v4.ns_nexopos_permissions
CREATE TABLE IF NOT EXISTS `ns_nexopos_permissions` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `namespace` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_permissions_name_unique` (`name`),
  UNIQUE KEY `ns_nexopos_permissions_namespace_unique` (`namespace`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_permissions: ~120 rows (approximately)
INSERT INTO `ns_nexopos_permissions` (`id`, `name`, `namespace`, `description`, `created_at`, `updated_at`) VALUES
	(1, 'Create Users', 'create.users', 'Can create users', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(2, 'Read Users', 'read.users', 'Can read users', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(3, 'Update Users', 'update.users', 'Can update users', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(4, 'Delete Users', 'delete.users', 'Can delete users', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(5, 'Create Roles', 'create.roles', 'Can create roles', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(6, 'Read Roles', 'read.roles', 'Can read roles', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(7, 'Update Roles', 'update.roles', 'Can update roles', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(8, 'Delete Roles', 'delete.roles', 'Can delete roles', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(9, 'Update Core', 'update.core', 'Can update core', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(10, 'Manage Profile', 'manage.profile', 'Can manage profile', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(11, 'Manage Modules', 'manage.modules', 'Can manage module : install, delete, update, migrate, enable, disable', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(12, 'Manage Options', 'manage.options', 'Can manage options : read, update', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(13, 'View Dashboard', 'read.dashboard', 'Can access the dashboard and see metrics', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(14, 'Upload Medias', 'nexopos.upload.medias', 'Let the user upload medias.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(15, 'See Medias', 'nexopos.see.medias', 'Let the user see medias.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(16, 'Delete Medias', 'nexopos.delete.medias', 'Let the user delete medias.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(17, 'Update Medias', 'nexopos.update.medias', 'Let the user update uploaded medias.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(18, 'Create Categories', 'nexopos.create.categories', 'Let the user create products categories.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(19, 'Delete Categories', 'nexopos.delete.categories', 'Let the user delete products categories.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(20, 'Update Categories', 'nexopos.update.categories', 'Let the user update products categories.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(21, 'Read Categories', 'nexopos.read.categories', 'Let the user read products categories.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(22, 'Create Customers', 'nexopos.create.customers', 'Let the user create customers.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(23, 'Delete Customers', 'nexopos.delete.customers', 'Let the user delete customers.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(24, 'Update Customers', 'nexopos.update.customers', 'Let the user update customers.', '2025-02-18 03:01:03', '2025-02-18 03:01:03'),
	(25, 'Read Customers', 'nexopos.read.customers', 'Let the user read customers.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(26, 'Import Customers', 'nexopos.import.customers', 'Let the user import customers.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(27, 'Manage Customer Account History', 'nexopos.customers.manage-account-history', 'Can add, deduct amount from each customers account.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(28, 'Create Customers Groups', 'nexopos.create.customers-groups', 'Let the user create Customers Groups', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(29, 'Delete Customers Groups', 'nexopos.delete.customers-groups', 'Let the user delete Customers Groups', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(30, 'Update Customers Groups', 'nexopos.update.customers-groups', 'Let the user update Customers Groups', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(31, 'Read Customers Groups', 'nexopos.read.customers-groups', 'Let the user read Customers Groups', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(32, 'Create Coupons', 'nexopos.create.coupons', 'Let the user create coupons', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(33, 'Delete Coupons', 'nexopos.delete.coupons', 'Let the user delete coupons', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(34, 'Update Coupons', 'nexopos.update.coupons', 'Let the user update coupons', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(35, 'Read Coupons', 'nexopos.read.coupons', 'Let the user read coupons', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(36, 'Create Transaction Account', 'nexopos.create.transactions-account', 'Let the user create transactions account', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(37, 'Delete Transactions Account', 'nexopos.delete.transactions-account', 'Let the user delete Transaction Account', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(38, 'Update Transactions Account', 'nexopos.update.transactions-account', 'Let the user update Transaction Account', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(39, 'Read Transactions Account', 'nexopos.read.transactions-account', 'Let the user read Transaction Account', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(40, 'Create Transaction', 'nexopos.create.transactions', 'Let the user create transactions', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(41, 'Delete Transaction', 'nexopos.delete.transactions', 'Let the user delete transactions', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(42, 'Update Transaction', 'nexopos.update.transactions', 'Let the user update transactions', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(43, 'Read Transaction', 'nexopos.read.transactions', 'Let the user read transactions', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(44, 'Read Transactions History', 'nexopos.read.transactions-history', 'Give access to the transactions history.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(45, 'Delete Transactions History', 'nexopos.delete.transactions-history', 'Allow to delete an Transactions History.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(46, 'Update Transactions History', 'nexopos.update.transactions-history', 'Allow to the Transactions History.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(47, 'Create Transactions History', 'nexopos.create.transactions-history', 'Allow to create a Transactions History.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(48, 'Create Orders', 'nexopos.create.orders', 'Let the user create orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(49, 'Delete Orders', 'nexopos.delete.orders', 'Let the user delete orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(50, 'Update Orders', 'nexopos.update.orders', 'Let the user update orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(51, 'Read Orders', 'nexopos.read.orders', 'Let the user read orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(52, 'Void Order', 'nexopos.void.orders', 'Let the user void orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(53, 'Refund Order', 'nexopos.refund.orders', 'Let the user refund orders', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(54, 'Make Payment To orders', 'nexopos.make-payment.orders', 'Allow the user to make payments to orders.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(55, 'Create Procurements', 'nexopos.create.procurements', 'Let the user create procurements', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(56, 'Delete Procurements', 'nexopos.delete.procurements', 'Let the user delete procurements', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(57, 'Update Procurements', 'nexopos.update.procurements', 'Let the user update procurements', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(58, 'Read Procurements', 'nexopos.read.procurements', 'Let the user read procurements', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(59, 'Create Providers', 'nexopos.create.providers', 'Let the user create providers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(60, 'Delete Providers', 'nexopos.delete.providers', 'Let the user delete providers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(61, 'Update Providers', 'nexopos.update.providers', 'Let the user update providers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(62, 'Read Providers', 'nexopos.read.providers', 'Let the user read providers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(63, 'Create Products', 'nexopos.create.products', 'Let the user create products', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(64, 'Delete Products', 'nexopos.delete.products', 'Let the user delete products', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(65, 'Update Products', 'nexopos.update.products', 'Let the user update products', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(66, 'Read Products', 'nexopos.read.products', 'Let the user read products', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(67, 'Convert Products Units', 'nexopos.convert.products-units', 'Let the user convert products', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(68, 'Read Product History', 'nexopos.read.products-history', 'Let the user read products history', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(69, 'Adjust Product Stock', 'nexopos.make.products-adjustments', 'Let the user adjust product stock.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(70, 'Create Product Units/Unit Group', 'nexopos.create.products-units', 'Let the user create products units.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(71, 'Read Product Units/Unit Group', 'nexopos.read.products-units', 'Let the user read products units.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(72, 'Update Product Units/Unit Group', 'nexopos.update.products-units', 'Let the user update products units.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(73, 'Delete Product Units/Unit Group', 'nexopos.delete.products-units', 'Let the user delete products units.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(74, 'Create Registers', 'nexopos.create.registers', 'Let the user create registers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(75, 'Delete Registers', 'nexopos.delete.registers', 'Let the user delete registers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(76, 'Update Registers', 'nexopos.update.registers', 'Let the user update registers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(77, 'Read Registers', 'nexopos.read.registers', 'Let the user read registers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(78, 'Read Registers History', 'nexopos.read.registers-history', 'Let the user read registers history', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(79, 'Read Use Registers', 'nexopos.use.registers', 'Let the user use registers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(80, 'Create Rewards', 'nexopos.create.rewards', 'Let the user create Rewards', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(81, 'Delete Rewards', 'nexopos.delete.rewards', 'Let the user delete Rewards', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(82, 'Update Rewards', 'nexopos.update.rewards', 'Let the user update Rewards', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(83, 'Read Rewards', 'nexopos.read.rewards', 'Let the user read Rewards', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(84, 'Create Taxes', 'nexopos.create.taxes', 'Let the user create taxes', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(85, 'Delete Taxes', 'nexopos.delete.taxes', 'Let the user delete taxes', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(86, 'Update Taxes', 'nexopos.update.taxes', 'Let the user update taxes', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(87, 'Read Taxes', 'nexopos.read.taxes', 'Let the user read taxes', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(88, 'See Sale Report', 'nexopos.reports.sales', 'Let you see the sales report', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(89, 'See Products Report', 'nexopos.reports.products-report', 'Let you see the Products report', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(90, 'See Best Report', 'nexopos.reports.best_sales', 'Let you see the best_sales report', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(91, 'See Transaction Report', 'nexopos.reports.transactions', 'Let you see the transactions report', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(92, 'See Yearly Sales', 'nexopos.reports.yearly', 'Allow to see the yearly sales.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(93, 'See Customers', 'nexopos.reports.customers', 'Allow to see the customers', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(94, 'See Inventory Tracking', 'nexopos.reports.inventory', 'Allow to see the inventory', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(95, 'See Customers Statement', 'nexopos.reports.customers-statement', 'Allow to see the customers statement.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(96, 'Read Sales by Payment Types', 'nexopos.reports.payment-types', 'Let the user read the report that shows sales by payment types.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(97, 'Read Low Stock Report', 'nexopos.reports.low-stock', 'Let the user read the report that shows low stock.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(98, 'Read Stock History', 'nexopos.reports.stock-history', 'Let the user read the stock history report.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(99, 'Manage Order Payments', 'nexopos.manage-payments-types', 'Allow to create, update and delete payments type.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(100, 'Edit Purchase Price', 'nexopos.pos.edit-purchase-price', 'Let the user edit the purchase price of products.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(101, 'Edit Order Settings', 'nexopos.pos.edit-settings', 'Let the user edit the order settings.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(102, 'Edit Product Discounts', 'nexopos.pos.products-discount', 'Let the user add discount on products.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(103, 'Edit Cart Discounts', 'nexopos.pos.cart-discount', 'Let the user add discount on cart.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(104, 'POS: Delete Order Products', 'nexopos.pos.delete-order-product', 'Let the user delete order products on POS.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(105, 'Widget: Incomplete Sale Card Widget', 'nexopos.see.incomplete-sale-card-widget', 'Will display a card of current and overall incomplete sales.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(106, 'Widget: Expense Card Widget', 'nexopos.see.expense-card-widget', 'Will display a card of current and overwall expenses.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(107, 'Widget: Sale Card Widget', 'nexopos.see.sale-card-widget', 'Will display current and overall sales.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(108, 'Widget: Best Customers', 'nexopos.see.best-customers-widget', 'Will display all customers with the highest purchases.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(109, 'Widget: Profile', 'nexopos.see.profile-widget', 'Will display a profile widget with user stats.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(110, 'Widget: Orders Chart', 'nexopos.see.orders-chart-widget', 'Will display a chart of weekly sales.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(111, 'Widget: Orders Summary', 'nexopos.see.orders-summary-widget', 'Will display a summary of recent sales.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(112, 'Widget: Best Cashiers', 'nexopos.see.best-cashier-widget', 'Will display all cashiers who performs well.', '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(113, 'Read Cash Flow History', 'nexopos.read.cash-flow-history', 'Allow to the Cash Flow History.', '2025-02-18 03:01:07', '2025-02-18 03:01:07'),
	(114, 'Delete Expense History', 'nexopos.delete.cash-flow-history', 'Allow to delete an expense history.', '2025-02-18 03:01:07', '2025-02-18 03:01:07'),
	(115, 'Manage Customers Account', 'nexopos.customers.manage-account', 'Allow to manage customer virtual deposit account.', '2025-02-18 03:01:07', '2025-02-18 03:01:07'),
	(116, 'Create Products Labels', 'nexopos.create.products-labels', 'Allow the user to create products labels', '2025-02-18 03:01:07', '2025-02-18 03:01:07'),
	(117, 'Create Instalment', 'nexopos.create.orders-instalments', 'Allow the user to create instalments.', '2025-02-18 03:01:15', '2025-02-18 03:01:15'),
	(118, 'Update Instalment', 'nexopos.update.orders-instalments', 'Allow the user to update instalments.', '2025-02-18 03:01:15', '2025-02-18 03:01:15'),
	(119, 'Read Instalment', 'nexopos.read.orders-instalments', 'Allow the user to read instalments.', '2025-02-18 03:01:15', '2025-02-18 03:01:15'),
	(120, 'Delete Instalment', 'nexopos.delete.orders-instalments', 'Allow the user to delete instalments.', '2025-02-18 03:01:15', '2025-02-18 03:01:15');

-- Dumping structure for table nexopos_v4.ns_nexopos_procurements
CREATE TABLE IF NOT EXISTS `ns_nexopos_procurements` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_id` int NOT NULL,
  `value` float NOT NULL DEFAULT '0',
  `cost` float NOT NULL DEFAULT '0',
  `tax_value` float NOT NULL DEFAULT '0',
  `invoice_reference` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `automatic_approval` tinyint(1) DEFAULT '0',
  `delivery_time` datetime DEFAULT NULL,
  `invoice_date` datetime DEFAULT NULL,
  `payment_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `delivery_status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `total_items` int NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_procurements: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_procurements_products
CREATE TABLE IF NOT EXISTS `ns_nexopos_procurements_products` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gross_purchase_price` float NOT NULL DEFAULT '0',
  `net_purchase_price` float NOT NULL DEFAULT '0',
  `procurement_id` int NOT NULL,
  `product_id` int NOT NULL,
  `purchase_price` float NOT NULL DEFAULT '0',
  `quantity` float NOT NULL,
  `available_quantity` float NOT NULL,
  `tax_group_id` int DEFAULT NULL,
  `barcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expiration_date` datetime DEFAULT NULL,
  `tax_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tax_value` float NOT NULL DEFAULT '0',
  `total_purchase_price` float NOT NULL DEFAULT '0',
  `unit_id` int NOT NULL,
  `convert_unit_id` int DEFAULT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_procurements_products: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_products
CREATE TABLE IF NOT EXISTS `ns_nexopos_products` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tax_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tax_group_id` int DEFAULT NULL,
  `tax_value` float NOT NULL DEFAULT '0',
  `product_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'product',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'tangible',
  `accurate_tracking` tinyint(1) NOT NULL DEFAULT '0',
  `auto_cogs` tinyint(1) NOT NULL DEFAULT '1',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available',
  `stock_management` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `barcode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `barcode_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sku` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `thumbnail_id` int DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `parent_id` int NOT NULL DEFAULT '0',
  `unit_group` int NOT NULL,
  `on_expiration` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'prevent_sales',
  `expires` tinyint(1) NOT NULL DEFAULT '0',
  `searchable` tinyint(1) NOT NULL DEFAULT '1',
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products: ~28 rows (approximately)
INSERT INTO `ns_nexopos_products` (`id`, `name`, `tax_type`, `tax_group_id`, `tax_value`, `product_type`, `type`, `accurate_tracking`, `auto_cogs`, `status`, `stock_management`, `barcode`, `barcode_type`, `sku`, `description`, `thumbnail_id`, `category_id`, `parent_id`, `unit_group`, `on_expiration`, `expires`, `searchable`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'Premium Pure Organza', 'inclusive', NULL, 0, 'product', 'materialized', 0, 0, 'available', 'enabled', 'Re51PaRuEv', 'code128', 'dressmaterialhq--premium-pure-organza--MDo3u', NULL, NULL, 1, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 08:49:58', '2025-02-18 09:00:15'),
	(2, 'Pure Digital print', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'SIxZ6yWgxj', 'code128', 'dressmaterialhq--pure-digital-print--Cw7Uj', NULL, NULL, 1, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 09:58:02', '2025-02-18 09:58:02'),
	(3, 'Grey Shimmer', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'YU0Mg4yGhG', 'code128', 'dressmaterialhq--grey-shimmer--TLO0u', NULL, NULL, 1, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 09:59:28', '2025-02-18 09:59:28'),
	(4, 'Pure banarasi', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'QXE72qTQJO', 'code128', 'dressmaterialhq--pure-banarasi--cXIBJ', NULL, NULL, 1, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:00:39', '2025-02-18 10:00:39'),
	(5, 'Muslin pure', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'lj7vtwnQgl', 'code128', 'dressmaterialhq--muslin-pure--x48rg', NULL, NULL, 1, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:06:30', '2025-02-18 10:06:30'),
	(6, 'Roman Silk', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', '4WWJQZoGUK', 'code128', 'tm--roman-silk--uqcoo', NULL, NULL, 2, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:12:00', '2025-02-18 10:12:00'),
	(7, 'Shiny Morpankhi', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'DORNiWKCaR', 'code128', 'tm--shiny-morpankhi--GZa1M', NULL, NULL, 2, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:13:50', '2025-02-18 10:13:50'),
	(8, 'gold crush with inner', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'd9Nf5klCaP', 'code128', 'tm--gold-crush-with-inner--kN5B7', NULL, NULL, 2, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:16:18', '2025-02-18 10:16:18'),
	(9, 'georgette BnW', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'qMY5j9Qki5', 'code128', 'ghazi-fabric--georgette-bnw--KnZ1q', NULL, NULL, 3, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:20:06', '2025-02-18 10:20:06'),
	(10, 'Zarkan with inner gorgette', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', '5DbYPoiPzP', 'code128', 'geargette-heavy-zurqan-with-inner--ghazi-fabric--8POP8', NULL, NULL, 3, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:26:37', '2025-02-18 12:20:01'),
	(11, 'georgette heavy with zurqan', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'vJIHgdWiYa', 'code128', 'ghazi-fabric--georgette-heavy-with-zurqan--2rnOE', NULL, NULL, 3, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:29:35', '2025-02-18 10:29:35'),
	(12, 'pakistani regular', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'RL4rblqWNx', 'code128', 'pakistani--pakistani-regular--E7qwo', NULL, NULL, 5, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:36:50', '2025-02-18 10:36:50'),
	(13, 'pakistani ultra', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'C2bBx87VsO', 'code128', 'pakistani--pakistani-ultra--y1SmP', NULL, NULL, 5, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:45:42', '2025-02-18 10:45:42'),
	(14, 'v s handwork', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'l0EI435V98', 'code128', 'handwork--v-s-handwork--ePz4d', NULL, NULL, 6, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:50:24', '2025-02-18 10:50:24'),
	(15, 'a m hand work', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', '0m3v8sOVPD', 'code128', 'handwork--a-m-hand-work--WtBQU', NULL, NULL, 6, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 10:56:23', '2025-02-18 10:56:23'),
	(16, 'regular cotton', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'MpVeppj21u', 'code128', 'cotton-material--regular-cotton--8zlDx', NULL, NULL, 7, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:04:54', '2025-02-18 11:04:54'),
	(17, 'curetia cotton', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'sD860jxior', 'code128', 'cotton-material--curetia-cotton--zuyTH', NULL, NULL, 7, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:08:08', '2025-02-18 11:08:08'),
	(18, 'broad floral [luckhnowi]', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'mBu1dyDfQd', 'code128', 'sagar-fashion--broad-floral-luckhnowi--9ZoQW', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:16:30', '2025-02-18 11:16:30'),
	(19, 'small floral [ black ]', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'DOboul5qXv', 'code128', 'sagar-fashion--small-floral-black--Fzyj1', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:19:05', '2025-02-18 11:19:05'),
	(20, 'b. f chiffon dupatta', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'IFxl6EJbZQ', 'code128', 'sagar-fashion--b-f-chiffon-dupatta--8pBOX', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:23:07', '2025-02-18 11:23:07'),
	(21, 'LT Cf suit', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'f3niJ9BWff', 'code128', 'sagar-fashion--lt-cf-suit--FVBZO', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:27:38', '2025-02-18 11:27:38'),
	(22, 'White base suit', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'rhUSYAhJUF', 'code128', 'sagar-fashion--white-base-suit--kTVKq', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:31:43', '2025-02-18 11:31:43'),
	(23, 'V neck karachi', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'WTMfp79a7d', 'code128', 'sagar-fashion--v-neck-karachi--JdbxV', NULL, NULL, 8, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:35:08', '2025-02-18 11:35:08'),
	(24, 'ryon plain', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'F5IfbHVeDP', 'code128', 'lko-kurti--ryon-plain--hUxc8', NULL, NULL, 9, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:40:53', '2025-02-18 11:40:53'),
	(25, 'Black mlml', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'GXbuDJJGFD', 'code128', 'lko-kurti--black-mlml--pSoN1', NULL, NULL, 9, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:44:25', '2025-02-18 11:44:25'),
	(26, 'Floral mlml', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', '9dv2tfmMEs', 'code128', 'lko-kurti--floral-mlml--krU7X', NULL, NULL, 9, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:47:31', '2025-02-18 11:47:31'),
	(27, 'Cotton kurti', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'kBPUhgrcRS', 'code128', 'lko-kurti--cotton-kurti--QtKqW', NULL, NULL, 9, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:49:59', '2025-02-18 11:49:59'),
	(28, 'Hvy PR suit', 'inclusive', NULL, 0, 'product', 'materialized', 0, 1, 'available', 'enabled', 'Y1BAwipvGl', 'code128', 'heavy-premium-suit--hvy-pr-suit--pdB8p', NULL, NULL, 10, 0, 1, 'prevent-sales', 0, 1, 85, NULL, '2025-02-18 11:56:15', '2025-02-18 11:56:15');

-- Dumping structure for table nexopos_v4.ns_nexopos_products_categories
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_categories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` int DEFAULT '0',
  `media_id` int NOT NULL DEFAULT '0',
  `preview_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `displays_on_pos` tinyint(1) NOT NULL DEFAULT '1',
  `total_items` int NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_categories: ~9 rows (approximately)
INSERT INTO `ns_nexopos_products_categories` (`id`, `name`, `parent_id`, `media_id`, `preview_url`, `displays_on_pos`, `total_items`, `description`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'DressMaterialHQ', NULL, 0, NULL, 1, 5, NULL, 85, NULL, '2025-02-18 08:42:30', '2025-02-18 10:06:31'),
	(2, 'TM', NULL, 0, NULL, 1, 3, NULL, 85, NULL, '2025-02-18 10:10:24', '2025-02-18 10:16:18'),
	(3, 'ghazi fabric', NULL, 0, NULL, 1, 3, NULL, 85, NULL, '2025-02-18 10:18:31', '2025-02-18 12:20:01'),
	(5, 'pakistani', NULL, 0, NULL, 1, 2, NULL, 85, NULL, '2025-02-18 10:34:48', '2025-02-18 10:45:42'),
	(6, 'handwork', NULL, 0, NULL, 1, 2, NULL, 85, NULL, '2025-02-18 10:47:25', '2025-02-18 10:56:23'),
	(7, 'cotton material', NULL, 0, NULL, 1, 2, NULL, 85, NULL, '2025-02-18 11:03:32', '2025-02-18 11:08:08'),
	(8, 'sagar fashion', NULL, 0, NULL, 1, 6, NULL, 85, NULL, '2025-02-18 11:10:17', '2025-02-18 11:35:08'),
	(9, 'LKO kurti', NULL, 0, NULL, 1, 4, NULL, 85, NULL, '2025-02-18 11:37:44', '2025-02-18 11:49:59'),
	(10, 'Heavy premium suit', NULL, 0, NULL, 1, 1, NULL, 85, NULL, '2025-02-18 11:52:13', '2025-02-18 11:56:15');

-- Dumping structure for table nexopos_v4.ns_nexopos_products_galleries
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_galleries` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_id` int NOT NULL,
  `media_id` int DEFAULT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order` int NOT NULL DEFAULT '0',
  `featured` tinyint(1) NOT NULL DEFAULT '0',
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_galleries: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_products_histories
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_histories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `procurement_id` int DEFAULT NULL,
  `procurement_product_id` int DEFAULT NULL,
  `order_id` int DEFAULT NULL,
  `order_product_id` int DEFAULT NULL,
  `operation_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_id` int NOT NULL,
  `before_quantity` float DEFAULT NULL,
  `quantity` float NOT NULL,
  `after_quantity` float DEFAULT NULL,
  `unit_price` float NOT NULL,
  `total_price` float NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_histories: ~31 rows (approximately)
INSERT INTO `ns_nexopos_products_histories` (`id`, `product_id`, `procurement_id`, `procurement_product_id`, `order_id`, `order_product_id`, `operation_type`, `unit_id`, `before_quantity`, `quantity`, `after_quantity`, `unit_price`, `total_price`, `description`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 1, NULL, NULL, NULL, NULL, 'added', 1, 0, 3, 3, 1800, 5400, '', 85, NULL, '2025-02-18 08:58:08', '2025-02-18 08:58:08'),
	(2, 2, NULL, NULL, NULL, NULL, 'added', 1, 0, 1, 1, 1900, 1900, '', 85, NULL, '2025-02-18 09:58:35', '2025-02-18 09:58:35'),
	(3, 4, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1650, 6600, '', 85, NULL, '2025-02-18 10:01:18', '2025-02-18 10:01:18'),
	(4, 3, NULL, NULL, NULL, NULL, 'added', 1, 0, 1, 1, 1900, 1900, '', 85, NULL, '2025-02-18 10:03:05', '2025-02-18 10:03:05'),
	(5, 6, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 990, 3960, '', 85, NULL, '2025-02-18 10:12:38', '2025-02-18 10:12:38'),
	(6, 7, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1300, 5200, '', 85, NULL, '2025-02-18 10:14:17', '2025-02-18 10:14:17'),
	(7, 8, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1350, 5400, '', 85, NULL, '2025-02-18 10:17:05', '2025-02-18 10:17:05'),
	(8, 9, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1150, 4600, '', 85, NULL, '2025-02-18 10:20:56', '2025-02-18 10:20:56'),
	(9, 11, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1550, 6200, '', 85, NULL, '2025-02-18 10:30:22', '2025-02-18 10:30:22'),
	(10, 12, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 550, 2200, '', 85, NULL, '2025-02-18 10:38:12', '2025-02-18 10:38:12'),
	(11, 13, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 850, 3400, '', 85, NULL, '2025-02-18 12:07:41', '2025-02-18 12:07:41'),
	(12, 19, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1050, 4200, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(13, 18, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1050, 4200, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(14, 17, NULL, NULL, NULL, NULL, 'added', 1, 0, 30, 30, 725, 21750, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(15, 16, NULL, NULL, NULL, NULL, 'added', 1, 0, 30, 30, 550, 16500, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(16, 15, NULL, NULL, NULL, NULL, 'added', 1, 0, 8, 8, 1450, 11600, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(17, 14, NULL, NULL, NULL, NULL, 'added', 1, 0, 6, 6, 1750, 10500, '', 85, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(18, 25, NULL, NULL, NULL, NULL, 'added', 1, 0, 2, 2, 550, 1100, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(19, 24, NULL, NULL, NULL, NULL, 'added', 1, 0, 3, 3, 600, 1800, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(20, 23, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1150, 4600, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(21, 22, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1125, 4500, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(22, 21, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1100, 4400, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(23, 20, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1050, 4200, '', 85, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(24, 28, NULL, NULL, NULL, NULL, 'added', 1, 0, 3, 3, 2250, 6750, '', 85, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(25, 27, NULL, NULL, NULL, NULL, 'added', 1, 0, 3, 3, 450, 1350, '', 85, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(26, 26, NULL, NULL, NULL, NULL, 'added', 1, 0, 1, 1, 750, 750, '', 85, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(27, 5, NULL, NULL, NULL, NULL, 'added', 1, 0, 3, 3, 1150, 3450, '', 85, NULL, '2025-02-18 12:17:31', '2025-02-18 12:17:31'),
	(28, 10, NULL, NULL, NULL, NULL, 'added', 1, 0, 4, 4, 1550, 6200, '', 85, NULL, '2025-02-18 12:20:58', '2025-02-18 12:20:58'),
	(29, 16, NULL, NULL, 1, 1, 'sold', 1, 30, 1, 29, 550, 550, '', 85, NULL, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(30, 17, NULL, NULL, 2, 2, 'sold', 1, 30, 1, 29, 725, 725, '', 85, NULL, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(31, 7, NULL, NULL, 3, 3, 'sold', 1, 4, 1, 3, 1300, 1300, '', 85, NULL, '2025-02-18 12:51:15', '2025-02-18 12:51:15');

-- Dumping structure for table nexopos_v4.ns_nexopos_products_histories_combined
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_histories_combined` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `date` date NOT NULL,
  `product_id` int NOT NULL,
  `unit_id` int NOT NULL,
  `initial_quantity` double NOT NULL DEFAULT '0',
  `sold_quantity` double NOT NULL DEFAULT '0',
  `procured_quantity` double NOT NULL DEFAULT '0',
  `defective_quantity` double NOT NULL DEFAULT '0',
  `final_quantity` double NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_histories_combined: ~28 rows (approximately)
INSERT INTO `ns_nexopos_products_histories_combined` (`id`, `name`, `date`, `product_id`, `unit_id`, `initial_quantity`, `sold_quantity`, `procured_quantity`, `defective_quantity`, `final_quantity`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'Premium Pure Organza', '2025-02-18', 1, 1, 0, 0, 3, 0, 3, NULL, '2025-02-18 08:58:08', '2025-02-18 08:58:08'),
	(2, 'Pure Digital print', '2025-02-18', 2, 1, 0, 0, 1, 0, 1, NULL, '2025-02-18 09:58:35', '2025-02-18 09:58:35'),
	(3, 'Pure banarasi', '2025-02-18', 4, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:01:18', '2025-02-18 10:01:18'),
	(4, 'Grey Shimmer', '2025-02-18', 3, 1, 0, 0, 1, 0, 1, NULL, '2025-02-18 10:03:05', '2025-02-18 10:03:05'),
	(5, 'Roman Silk', '2025-02-18', 6, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:12:38', '2025-02-18 10:12:38'),
	(6, 'Shiny Morpankhi', '2025-02-18', 7, 1, 0, 1, 4, 0, 3, NULL, '2025-02-18 10:14:17', '2025-02-18 12:51:15'),
	(7, 'gold crush with inner', '2025-02-18', 8, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:17:05', '2025-02-18 10:17:05'),
	(8, 'georgette BnW', '2025-02-18', 9, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:20:56', '2025-02-18 10:20:56'),
	(9, 'georgette heavy with zurqan', '2025-02-18', 11, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:30:22', '2025-02-18 10:30:22'),
	(10, 'pakistani regular', '2025-02-18', 12, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 10:38:12', '2025-02-18 10:38:12'),
	(11, 'pakistani ultra', '2025-02-18', 13, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:07:41', '2025-02-18 12:07:41'),
	(12, 'small floral [ black ]', '2025-02-18', 19, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(13, 'broad floral [luckhnowi]', '2025-02-18', 18, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(14, 'curetia cotton', '2025-02-18', 17, 1, 0, 1, 30, 0, 29, NULL, '2025-02-18 12:10:21', '2025-02-18 12:43:08'),
	(15, 'regular cotton', '2025-02-18', 16, 1, 0, 1, 30, 0, 29, NULL, '2025-02-18 12:10:21', '2025-02-18 12:41:37'),
	(16, 'a m hand work', '2025-02-18', 15, 1, 0, 0, 8, 0, 8, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(17, 'v s handwork', '2025-02-18', 14, 1, 0, 0, 6, 0, 6, NULL, '2025-02-18 12:10:21', '2025-02-18 12:10:21'),
	(18, 'Black mlml', '2025-02-18', 25, 1, 0, 0, 2, 0, 2, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(19, 'ryon plain', '2025-02-18', 24, 1, 0, 0, 3, 0, 3, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(20, 'V neck karachi', '2025-02-18', 23, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(21, 'White base suit', '2025-02-18', 22, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(22, 'LT Cf suit', '2025-02-18', 21, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(23, 'b. f chiffon dupatta', '2025-02-18', 20, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:12:08', '2025-02-18 12:12:08'),
	(24, 'Hvy PR suit', '2025-02-18', 28, 1, 0, 0, 3, 0, 3, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(25, 'Cotton kurti', '2025-02-18', 27, 1, 0, 0, 3, 0, 3, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(26, 'Floral mlml', '2025-02-18', 26, 1, 0, 0, 1, 0, 1, NULL, '2025-02-18 12:13:02', '2025-02-18 12:13:02'),
	(27, 'Muslin pure', '2025-02-18', 5, 1, 0, 0, 3, 0, 3, NULL, '2025-02-18 12:17:31', '2025-02-18 12:17:31'),
	(28, 'Zarkan with inner gorgette', '2025-02-18', 10, 1, 0, 0, 4, 0, 4, NULL, '2025-02-18 12:20:58', '2025-02-18 12:20:58');

-- Dumping structure for table nexopos_v4.ns_nexopos_products_metas
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_metas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_metas: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_products_subitems
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_subitems` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` int NOT NULL,
  `product_id` int NOT NULL,
  `unit_id` int NOT NULL,
  `unit_quantity_id` int NOT NULL,
  `sale_price` double NOT NULL DEFAULT '0',
  `quantity` double NOT NULL DEFAULT '0',
  `total_price` double NOT NULL DEFAULT '0',
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_subitems: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_products_taxes
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_taxes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `unit_quantity_id` int NOT NULL,
  `tax_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rate` float NOT NULL,
  `value` float NOT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_taxes: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_products_unit_quantities
CREATE TABLE IF NOT EXISTS `ns_nexopos_products_unit_quantities` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'product',
  `preview_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expiration_date` datetime DEFAULT NULL,
  `unit_id` int NOT NULL,
  `barcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` float NOT NULL,
  `low_quantity` float NOT NULL DEFAULT '0',
  `stock_alert_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `sale_price` float NOT NULL DEFAULT '0',
  `sale_price_edit` float NOT NULL DEFAULT '0',
  `sale_price_without_tax` float NOT NULL DEFAULT '0',
  `sale_price_with_tax` float NOT NULL DEFAULT '0',
  `sale_price_tax` float NOT NULL DEFAULT '0',
  `wholesale_price` float NOT NULL DEFAULT '0',
  `wholesale_price_edit` float NOT NULL DEFAULT '0',
  `wholesale_price_with_tax` float NOT NULL DEFAULT '0',
  `wholesale_price_without_tax` float NOT NULL DEFAULT '0',
  `wholesale_price_tax` float NOT NULL DEFAULT '0',
  `custom_price` float NOT NULL DEFAULT '0',
  `custom_price_edit` float NOT NULL DEFAULT '0',
  `custom_price_with_tax` float NOT NULL DEFAULT '0',
  `custom_price_without_tax` float NOT NULL DEFAULT '0',
  `custom_price_tax` float NOT NULL DEFAULT '0',
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `convert_unit_id` int DEFAULT NULL,
  `cogs` double NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_products_unit_quantities: ~28 rows (approximately)
INSERT INTO `ns_nexopos_products_unit_quantities` (`id`, `product_id`, `type`, `preview_url`, `expiration_date`, `unit_id`, `barcode`, `quantity`, `low_quantity`, `stock_alert_enabled`, `sale_price`, `sale_price_edit`, `sale_price_without_tax`, `sale_price_with_tax`, `sale_price_tax`, `wholesale_price`, `wholesale_price_edit`, `wholesale_price_with_tax`, `wholesale_price_without_tax`, `wholesale_price_tax`, `custom_price`, `custom_price_edit`, `custom_price_with_tax`, `custom_price_without_tax`, `custom_price_tax`, `visible`, `convert_unit_id`, `cogs`, `uuid`, `created_at`, `updated_at`) VALUES
	(2, 1, 'product', '', NULL, 1, 'Re51PaRuEv-2', 3, 10, 1, 1800, 1800, 1800, 1800, 0, 1800, 1800, 1800, 1800, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 08:55:58', '2025-02-18 09:00:15'),
	(3, 2, 'product', '', NULL, 1, 'SIxZ6yWgxj-3', 1, 10, 1, 1900, 1900, 1900, 1900, 0, 1900, 1900, 1900, 1900, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 09:58:02', '2025-02-18 09:58:35'),
	(4, 3, 'product', '', NULL, 1, 'YU0Mg4yGhG-4', 1, 10, 0, 1900, 1900, 1900, 1900, 0, 1900, 1900, 1900, 1900, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 09:59:29', '2025-02-18 10:03:04'),
	(5, 4, 'product', '', NULL, 1, 'QXE72qTQJO-5', 4, 10, 1, 1650, 1650, 1650, 1650, 0, 1650, 1650, 1650, 1650, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:00:39', '2025-02-18 10:01:18'),
	(6, 5, 'product', '', NULL, 1, 'lj7vtwnQgl-6', 3, 10, 1, 1150, 1150, 1150, 1150, 0, 1150, 1150, 1150, 1150, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:06:30', '2025-02-18 12:17:31'),
	(7, 6, 'product', '', NULL, 1, '4WWJQZoGUK-7', 4, 10, 1, 990, 990, 990, 990, 0, 990, 990, 990, 990, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:12:00', '2025-02-18 10:12:37'),
	(8, 7, 'product', '', NULL, 1, 'DORNiWKCaR-8', 3, 10, 0, 1300, 1300, 1300, 1300, 0, 1300, 1300, 1300, 1300, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:13:50', '2025-02-18 12:51:15'),
	(9, 8, 'product', '', NULL, 1, 'd9Nf5klCaP-9', 4, 10, 0, 1350, 1350, 1350, 1350, 0, 1350, 1350, 1350, 1350, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:16:18', '2025-02-18 10:17:05'),
	(10, 9, 'product', '', NULL, 1, 'qMY5j9Qki5-10', 4, 10, 0, 1150, 1150, 1150, 1150, 0, 1150, 1150, 1150, 1150, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:20:06', '2025-02-18 10:20:55'),
	(11, 10, 'product', '', NULL, 1, '5DbYPoiPzP-11', 4, 10, 0, 1550, 1550, 1550, 1550, 0, 1550, 1550, 1550, 1550, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:26:37', '2025-02-18 12:20:58'),
	(12, 11, 'product', '', NULL, 1, 'vJIHgdWiYa-12', 4, 10, 0, 1550, 1550, 1550, 1550, 0, 1550, 1550, 1550, 1550, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:29:35', '2025-02-18 10:30:22'),
	(13, 12, 'product', '', NULL, 1, 'RL4rblqWNx-13', 4, 10, 0, 550, 550, 550, 550, 0, 550, 550, 550, 550, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:36:50', '2025-02-18 10:38:12'),
	(14, 13, 'product', '', NULL, 1, 'C2bBx87VsO-14', 4, 10, 0, 850, 850, 850, 850, 0, 850, 850, 850, 850, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:45:42', '2025-02-18 12:07:41'),
	(15, 14, 'product', '', NULL, 1, 'l0EI435V98-15', 6, 10, 1, 1750, 1750, 1750, 1750, 0, 1750, 1750, 1750, 1750, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:50:24', '2025-02-18 12:10:21'),
	(16, 15, 'product', '', NULL, 1, '0m3v8sOVPD-16', 8, 10, 0, 1450, 1450, 1450, 1450, 0, 1450, 1450, 1450, 1450, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 10:56:23', '2025-02-18 12:10:21'),
	(17, 16, 'product', '', NULL, 1, 'MpVeppj21u-17', 29, 10, 0, 550, 550, 550, 550, 0, 550, 550, 550, 550, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:04:54', '2025-02-18 12:41:37'),
	(18, 17, 'product', '', NULL, 1, 'sD860jxior-18', 29, 10, 0, 725, 725, 725, 725, 0, 725, 725, 725, 725, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:08:08', '2025-02-18 12:43:08'),
	(19, 18, 'product', '', NULL, 1, 'mBu1dyDfQd-19', 4, 10, 0, 1050, 1050, 1050, 1050, 0, 1050, 1050, 1050, 1050, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:16:30', '2025-02-18 12:10:21'),
	(20, 19, 'product', '', NULL, 1, 'DOboul5qXv-20', 4, 10, 0, 1050, 1050, 1050, 1050, 0, 1050, 1050, 1050, 1050, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:19:05', '2025-02-18 12:10:21'),
	(21, 20, 'product', '', NULL, 1, 'IFxl6EJbZQ-21', 4, 10, 0, 1050, 1050, 1050, 1050, 0, 1050, 1050, 1050, 1050, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:23:07', '2025-02-18 12:12:08'),
	(22, 21, 'product', '', NULL, 1, 'f3niJ9BWff-22', 4, 10, 0, 1100, 1100, 1100, 1100, 0, 1100, 1100, 1100, 1100, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:27:38', '2025-02-18 12:12:08'),
	(23, 22, 'product', '', NULL, 1, 'rhUSYAhJUF-23', 4, 10, 0, 1125, 1125, 1125, 1125, 0, 1125, 1125, 1125, 1125, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:31:43', '2025-02-18 12:12:08'),
	(24, 23, 'product', '', NULL, 1, 'WTMfp79a7d-24', 4, 10, 0, 1150, 1150, 1150, 1150, 0, 1150, 1150, 1150, 1150, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:35:08', '2025-02-18 12:12:08'),
	(25, 24, 'product', '', NULL, 1, 'F5IfbHVeDP-25', 3, 10, 1, 600, 600, 600, 600, 0, 600, 600, 600, 600, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:40:53', '2025-02-18 12:12:08'),
	(26, 25, 'product', '', NULL, 1, 'GXbuDJJGFD-26', 2, 10, 0, 550, 550, 550, 550, 0, 550, 550, 550, 550, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:44:25', '2025-02-18 12:12:07'),
	(27, 26, 'product', '', NULL, 1, '9dv2tfmMEs-27', 1, 10, 0, 750, 750, 750, 750, 0, 750, 750, 750, 750, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:47:31', '2025-02-18 12:13:02'),
	(28, 27, 'product', '', NULL, 1, 'kBPUhgrcRS-28', 3, 10, 0, 450, 450, 450, 450, 0, 450, 450, 450, 450, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:49:59', '2025-02-18 12:13:02'),
	(29, 28, 'product', '', NULL, 1, 'Y1BAwipvGl-29', 3, 10, 0, 2250, 2250, 2250, 2250, 0, 2250, 2250, 2250, 2250, 0, 0, 0, 0, 0, 0, 1, NULL, 0, NULL, '2025-02-18 11:56:15', '2025-02-18 12:13:02');

-- Dumping structure for table nexopos_v4.ns_nexopos_providers
CREATE TABLE IF NOT EXISTS `ns_nexopos_providers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `amount_due` float NOT NULL DEFAULT '0',
  `amount_paid` float NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_providers_email_unique` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_providers: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_registers
CREATE TABLE IF NOT EXISTS `ns_nexopos_registers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'closed',
  `description` text COLLATE utf8mb4_unicode_ci,
  `used_by` int DEFAULT NULL,
  `author` int NOT NULL,
  `balance` float NOT NULL DEFAULT '0',
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_registers: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_registers_history
CREATE TABLE IF NOT EXISTS `ns_nexopos_registers_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `register_id` int NOT NULL,
  `payment_id` int DEFAULT NULL,
  `payment_type_id` int NOT NULL DEFAULT '0',
  `order_id` int DEFAULT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author` int NOT NULL,
  `value` float NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `balance_before` float NOT NULL DEFAULT '0',
  `transaction_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `balance_after` float NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_registers_history: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_rewards_system
CREATE TABLE IF NOT EXISTS `ns_nexopos_rewards_system` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `author` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target` float NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  `coupon_id` int DEFAULT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_rewards_system: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_rewards_system_rules
CREATE TABLE IF NOT EXISTS `ns_nexopos_rewards_system_rules` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `from` float NOT NULL,
  `to` float NOT NULL,
  `reward` float NOT NULL,
  `reward_id` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_rewards_system_rules: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_roles
CREATE TABLE IF NOT EXISTS `ns_nexopos_roles` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `namespace` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `reward_system_id` int DEFAULT NULL,
  `minimal_credit_payment` double NOT NULL DEFAULT '0',
  `author` int DEFAULT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_roles_name_unique` (`name`),
  UNIQUE KEY `ns_nexopos_roles_namespace_unique` (`namespace`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_roles: ~5 rows (approximately)
INSERT INTO `ns_nexopos_roles` (`id`, `name`, `namespace`, `description`, `reward_system_id`, `minimal_credit_payment`, `author`, `locked`, `created_at`, `updated_at`) VALUES
	(1, 'User', 'user', 'Basic user role.', NULL, 0, NULL, 1, '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(2, 'Administrator', 'admin', 'Master role which can perform all actions like create users, install/update/delete modules and much more.', NULL, 0, NULL, 1, '2025-02-18 03:01:04', '2025-02-18 03:01:04'),
	(3, 'Store Administrator', 'nexopos.store.administrator', 'Has a control over an entire store of NexoPOS.', NULL, 0, NULL, 1, '2025-02-18 03:01:05', '2025-02-18 03:01:05'),
	(4, 'Store Cashier', 'nexopos.store.cashier', 'Has a control over the sale process.', NULL, 0, NULL, 1, '2025-02-18 03:01:06', '2025-02-18 03:01:06'),
	(5, 'Store Customer', 'nexopos.store.customer', 'Can purchase orders and manage his profile.', NULL, 0, NULL, 1, '2025-02-18 03:01:06', '2025-02-18 03:01:06');

-- Dumping structure for table nexopos_v4.ns_nexopos_role_permission
CREATE TABLE IF NOT EXISTS `ns_nexopos_role_permission` (
  `permission_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`permission_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_role_permission: ~245 rows (approximately)
INSERT INTO `ns_nexopos_role_permission` (`permission_id`, `role_id`) VALUES
	(1, 2),
	(2, 2),
	(3, 2),
	(4, 2),
	(5, 2),
	(6, 2),
	(7, 2),
	(8, 2),
	(9, 2),
	(10, 1),
	(10, 2),
	(10, 3),
	(10, 4),
	(10, 5),
	(11, 2),
	(12, 2),
	(13, 2),
	(13, 3),
	(13, 4),
	(13, 5),
	(14, 2),
	(15, 2),
	(16, 2),
	(17, 2),
	(18, 2),
	(18, 3),
	(19, 2),
	(19, 3),
	(20, 2),
	(20, 3),
	(21, 2),
	(21, 3),
	(22, 2),
	(22, 3),
	(23, 2),
	(23, 3),
	(24, 2),
	(24, 3),
	(25, 2),
	(25, 3),
	(26, 2),
	(26, 3),
	(27, 2),
	(27, 3),
	(27, 4),
	(28, 2),
	(28, 3),
	(29, 2),
	(29, 3),
	(30, 2),
	(30, 3),
	(31, 2),
	(31, 3),
	(32, 2),
	(32, 3),
	(33, 2),
	(33, 3),
	(34, 2),
	(34, 3),
	(35, 2),
	(35, 3),
	(36, 2),
	(36, 3),
	(37, 2),
	(37, 3),
	(38, 2),
	(38, 3),
	(39, 2),
	(39, 3),
	(40, 2),
	(40, 3),
	(41, 2),
	(41, 3),
	(42, 2),
	(42, 3),
	(43, 2),
	(43, 3),
	(44, 2),
	(44, 3),
	(45, 2),
	(45, 3),
	(46, 2),
	(46, 3),
	(47, 2),
	(47, 3),
	(48, 2),
	(48, 3),
	(48, 4),
	(49, 2),
	(49, 3),
	(50, 2),
	(50, 3),
	(50, 4),
	(51, 2),
	(51, 3),
	(51, 4),
	(52, 2),
	(52, 3),
	(52, 4),
	(53, 2),
	(53, 3),
	(53, 4),
	(54, 2),
	(54, 3),
	(54, 4),
	(55, 2),
	(55, 3),
	(56, 2),
	(56, 3),
	(57, 2),
	(57, 3),
	(58, 2),
	(58, 3),
	(59, 2),
	(59, 3),
	(60, 2),
	(60, 3),
	(61, 2),
	(61, 3),
	(62, 2),
	(62, 3),
	(63, 2),
	(63, 3),
	(64, 2),
	(64, 3),
	(65, 2),
	(65, 3),
	(66, 2),
	(66, 3),
	(67, 2),
	(67, 3),
	(68, 2),
	(68, 3),
	(69, 2),
	(69, 3),
	(70, 2),
	(70, 3),
	(71, 2),
	(71, 3),
	(72, 2),
	(72, 3),
	(73, 2),
	(73, 3),
	(74, 2),
	(74, 3),
	(75, 2),
	(75, 3),
	(76, 2),
	(76, 3),
	(77, 2),
	(77, 3),
	(78, 2),
	(78, 3),
	(79, 2),
	(79, 3),
	(80, 2),
	(80, 3),
	(81, 2),
	(81, 3),
	(82, 2),
	(82, 3),
	(83, 2),
	(83, 3),
	(84, 2),
	(84, 3),
	(85, 2),
	(85, 3),
	(86, 2),
	(86, 3),
	(87, 2),
	(87, 3),
	(88, 2),
	(88, 3),
	(89, 2),
	(89, 3),
	(90, 2),
	(90, 3),
	(91, 2),
	(91, 3),
	(92, 2),
	(92, 3),
	(93, 2),
	(93, 3),
	(94, 2),
	(94, 3),
	(95, 2),
	(95, 3),
	(96, 2),
	(96, 3),
	(97, 2),
	(97, 3),
	(98, 2),
	(98, 3),
	(99, 2),
	(99, 3),
	(100, 2),
	(100, 3),
	(100, 4),
	(101, 2),
	(101, 3),
	(101, 4),
	(102, 2),
	(102, 3),
	(102, 4),
	(103, 2),
	(103, 3),
	(103, 4),
	(104, 2),
	(104, 3),
	(104, 4),
	(105, 2),
	(105, 3),
	(106, 2),
	(106, 3),
	(107, 2),
	(107, 3),
	(108, 2),
	(108, 3),
	(109, 1),
	(109, 2),
	(109, 3),
	(109, 4),
	(109, 5),
	(110, 2),
	(110, 3),
	(111, 2),
	(111, 3),
	(112, 2),
	(112, 3),
	(113, 2),
	(113, 3),
	(114, 2),
	(114, 3),
	(115, 2),
	(115, 3),
	(116, 2),
	(116, 3),
	(117, 2),
	(117, 3),
	(118, 2),
	(118, 3),
	(119, 2),
	(119, 3),
	(120, 2),
	(120, 3);

-- Dumping structure for table nexopos_v4.ns_nexopos_taxes
CREATE TABLE IF NOT EXISTS `ns_nexopos_taxes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `rate` float NOT NULL,
  `tax_group_id` int NOT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_taxes: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_taxes_groups
CREATE TABLE IF NOT EXISTS `ns_nexopos_taxes_groups` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_taxes_groups: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` int NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `media_id` int NOT NULL DEFAULT '0',
  `value` float NOT NULL DEFAULT '0',
  `recurring` tinyint(1) NOT NULL DEFAULT '0',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `group_id` int DEFAULT NULL,
  `occurrence` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `occurrence_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scheduled_date` datetime DEFAULT NULL,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions_accounts
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions_accounts` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0',
  `sub_category_id` int DEFAULT NULL,
  `category_identifier` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions_accounts: ~17 rows (approximately)
INSERT INTO `ns_nexopos_transactions_accounts` (`id`, `name`, `account`, `sub_category_id`, `category_identifier`, `description`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'Fixed Assets', '1001-assets-fixed-assets', NULL, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(2, 'Current Assets', '1002-assets-current-assets', NULL, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(3, 'Inventory Account', '1003-assets-inventory-account', NULL, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(4, 'Current Liabilities', '2001-liabilities-current-liabilities', NULL, 'liabilities', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(5, 'Sales Revenues', '4001-revenues-sales-revenues', NULL, 'revenues', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(6, 'Direct Expenses', '5001-expenses-direct-expenses', NULL, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(7, 'Expenses Cash', '1004-assets-expenses-cash', 2, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(8, 'Procurement Cash', '1005-assets-procurement-cash', 2, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(9, 'Procurement Payable', '2002-liabilities-procurement-payable', 4, 'liabilities', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(10, 'Receivables', '1006-assets-receivables', 2, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(11, 'Sales', '1007-assets-sales', 2, 'assets', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(12, 'Refunds', '4002-revenues-refunds', 5, 'revenues', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(13, 'Sales COGS', '5002-expenses-sales-cogs', 6, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(14, 'Operating Expenses', '5003-expenses-operating-expenses', 6, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(15, 'Rent Expenses', '5004-expenses-rent-expenses', 6, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(16, 'Other Expenses', '5005-expenses-other-expenses', 6, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(17, 'Salaries And Wages', '5006-expenses-salaries-and-wages', 6, 'expenses', NULL, 85, NULL, '2025-02-18 03:01:18', '2025-02-18 03:01:18');

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions_actions_rules
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions_actions_rules` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `on` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` enum('increase','decrease') COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` int NOT NULL,
  `do` enum('increase','decrease') COLLATE utf8mb4_unicode_ci NOT NULL,
  `offset_account_id` int NOT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions_actions_rules: ~10 rows (approximately)
INSERT INTO `ns_nexopos_transactions_actions_rules` (`id`, `on`, `action`, `account_id`, `do`, `offset_account_id`, `locked`, `created_at`, `updated_at`) VALUES
	(1, 'procurement_unpaid', 'increase', 3, 'increase', 9, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(2, 'procurement_paid', 'increase', 3, 'decrease', 8, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(3, 'procurement_from_unpaid_to_paid', 'decrease', 9, 'decrease', 8, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(4, 'order_unpaid', 'increase', 10, 'increase', 5, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(5, 'order_from_unpaid_to_paid', 'decrease', 10, 'increase', 11, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(6, 'order_paid', 'increase', 11, 'decrease', 10, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(7, 'order_refunded', 'decrease', 5, 'decrease', 11, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(8, 'order_cogs', 'increase', 17, 'decrease', 3, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(9, 'order_paid_voided', 'decrease', 5, 'decrease', 11, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18'),
	(10, 'order_unpaid_voided', 'decrease', 5, 'decrease', 10, 0, '2025-02-18 03:01:18', '2025-02-18 03:01:18');

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions_balance_days
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions_balance_days` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `opening_balance` double NOT NULL DEFAULT '0',
  `income` double NOT NULL DEFAULT '0',
  `expense` double NOT NULL DEFAULT '0',
  `closing_balance` double NOT NULL DEFAULT '0',
  `date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions_balance_days: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions_balance_months
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions_balance_months` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `opening_balance` double NOT NULL DEFAULT '0',
  `income` double NOT NULL DEFAULT '0',
  `expense` double NOT NULL DEFAULT '0',
  `closing_balance` double NOT NULL DEFAULT '0',
  `date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions_balance_months: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_nexopos_transactions_histories
CREATE TABLE IF NOT EXISTS `ns_nexopos_transactions_histories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` int DEFAULT NULL,
  `operation` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_reflection` tinyint(1) NOT NULL DEFAULT '0',
  `reflection_source_id` int DEFAULT NULL,
  `transaction_account_id` int DEFAULT NULL,
  `procurement_id` int DEFAULT NULL,
  `order_refund_id` int DEFAULT NULL,
  `order_payment_id` int DEFAULT NULL,
  `order_refund_product_id` int DEFAULT NULL,
  `order_id` int DEFAULT NULL,
  `order_product_id` int DEFAULT NULL,
  `register_history_id` int DEFAULT NULL,
  `customer_account_history_id` int DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `value` float NOT NULL DEFAULT '0',
  `trigger_date` datetime DEFAULT NULL,
  `rule_id` int DEFAULT NULL,
  `author` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_transactions_histories: ~12 rows (approximately)
INSERT INTO `ns_nexopos_transactions_histories` (`id`, `transaction_id`, `operation`, `is_reflection`, `reflection_source_id`, `transaction_account_id`, `procurement_id`, `order_refund_id`, `order_payment_id`, `order_refund_product_id`, `order_id`, `order_product_id`, `register_history_id`, `customer_account_history_id`, `name`, `type`, `status`, `value`, `trigger_date`, `rule_id`, `author`, `created_at`, `updated_at`) VALUES
	(1, NULL, 'debit', 0, NULL, 11, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 'Order: 250218-001', 'ns.indirect-transaction', 'active', 550, '2025-02-18 18:11:37', 6, 85, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(2, NULL, 'credit', 1, 1, 10, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 'Order: 250218-001', 'ns.indirect-transaction', 'active', 550, '2025-02-18 18:11:36', NULL, 85, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(3, NULL, 'debit', 0, NULL, 17, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 'COGS: 250218-001', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:11:37', 8, 85, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(4, NULL, 'credit', 1, 3, 3, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 'COGS: 250218-001', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:11:36', NULL, 85, '2025-02-18 12:41:37', '2025-02-18 12:41:37'),
	(5, NULL, 'debit', 0, NULL, 11, NULL, NULL, NULL, NULL, 2, NULL, NULL, NULL, 'Order: 250218-002', 'ns.indirect-transaction', 'active', 700, '2025-02-18 18:13:08', 6, 85, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(6, NULL, 'credit', 1, 5, 10, NULL, NULL, NULL, NULL, 2, NULL, NULL, NULL, 'Order: 250218-002', 'ns.indirect-transaction', 'active', 700, '2025-02-18 18:13:07', NULL, 85, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(7, NULL, 'debit', 0, NULL, 17, NULL, NULL, NULL, NULL, 2, NULL, NULL, NULL, 'COGS: 250218-002', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:13:08', 8, 85, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(8, NULL, 'credit', 1, 7, 3, NULL, NULL, NULL, NULL, 2, NULL, NULL, NULL, 'COGS: 250218-002', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:13:07', NULL, 85, '2025-02-18 12:43:08', '2025-02-18 12:43:08'),
	(9, NULL, 'debit', 0, NULL, 11, NULL, NULL, NULL, NULL, 3, NULL, NULL, NULL, 'Order: 250218-003', 'ns.indirect-transaction', 'active', 1300, '2025-02-18 18:21:14', 6, 85, '2025-02-18 12:51:15', '2025-02-18 12:51:15'),
	(10, NULL, 'credit', 1, 9, 10, NULL, NULL, NULL, NULL, 3, NULL, NULL, NULL, 'Order: 250218-003', 'ns.indirect-transaction', 'active', 1300, '2025-02-18 18:21:14', NULL, 85, '2025-02-18 12:51:15', '2025-02-18 12:51:15'),
	(11, NULL, 'debit', 0, NULL, 17, NULL, NULL, NULL, NULL, 3, NULL, NULL, NULL, 'COGS: 250218-003', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:21:14', 8, 85, '2025-02-18 12:51:15', '2025-02-18 12:51:15'),
	(12, NULL, 'credit', 1, 11, 3, NULL, NULL, NULL, NULL, 3, NULL, NULL, NULL, 'COGS: 250218-003', 'ns.indirect-transaction', 'active', 0, '2025-02-18 18:21:14', NULL, 85, '2025-02-18 12:51:15', '2025-02-18 12:51:15');

-- Dumping structure for table nexopos_v4.ns_nexopos_units
CREATE TABLE IF NOT EXISTS `ns_nexopos_units` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `group_id` int NOT NULL,
  `value` float NOT NULL,
  `preview_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `base_unit` tinyint(1) NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_units_identifier_unique` (`identifier`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_units: ~1 rows (approximately)
INSERT INTO `ns_nexopos_units` (`id`, `name`, `identifier`, `description`, `author`, `group_id`, `value`, `preview_url`, `base_unit`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'Dress', 'suit', NULL, 85, 1, 1, NULL, 0, NULL, '2025-02-18 08:48:56', '2025-02-18 08:48:56');

-- Dumping structure for table nexopos_v4.ns_nexopos_units_groups
CREATE TABLE IF NOT EXISTS `ns_nexopos_units_groups` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `author` int NOT NULL,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_units_groups: ~1 rows (approximately)
INSERT INTO `ns_nexopos_units_groups` (`id`, `name`, `description`, `author`, `uuid`, `created_at`, `updated_at`) VALUES
	(1, 'Peice', NULL, 85, NULL, '2025-02-18 08:47:02', '2025-02-18 08:47:02');

-- Dumping structure for table nexopos_v4.ns_nexopos_users
CREATE TABLE IF NOT EXISTS `ns_nexopos_users` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `author` int DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_id` int DEFAULT NULL,
  `first_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pobox` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activation_expiration` datetime DEFAULT NULL,
  `total_sales_count` int NOT NULL DEFAULT '0',
  `total_sales` float NOT NULL DEFAULT '0',
  `birth_date` datetime DEFAULT NULL,
  `purchases_amount` double NOT NULL DEFAULT '0',
  `owed_amount` double NOT NULL DEFAULT '0',
  `credit_limit_amount` double NOT NULL DEFAULT '0',
  `account_amount` double NOT NULL DEFAULT '0',
  `activation_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remember_token` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_nexopos_users_email_unique` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_users: ~2 rows (approximately)
INSERT INTO `ns_nexopos_users` (`id`, `username`, `active`, `author`, `email`, `password`, `group_id`, `first_name`, `last_name`, `gender`, `phone`, `pobox`, `activation_expiration`, `total_sales_count`, `total_sales`, `birth_date`, `purchases_amount`, `owed_amount`, `credit_limit_amount`, `account_amount`, `activation_token`, `remember_token`, `created_at`, `updated_at`) VALUES
	(85, 'Ghazi', 1, 85, 'sayyedghazi3@gmail.com', '$2y$10$P0FLRjmRtvMGcgLfwmJ2nuiB39YraWOvdEuttaeXNOtY9d/YuvgfO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, 2550, NULL, 0, 0, 0, 0, NULL, NULL, '2025-02-18 03:01:17', '2025-02-18 12:51:14'),
	(86, 'customer-87@nexopos.test', 0, 85, 'customer-87@nexopos.test', '$2y$10$x2rhcK/pqqjyK6qMoipH4upRWgIAltMC/yimVTHDvDlR8Yve7kmza', 1, '9503698738', NULL, NULL, NULL, NULL, NULL, 0, 0, '2025-02-18 18:09:41', 2550, 0, 0, 0, NULL, NULL, '2025-02-18 12:40:50', '2025-02-18 12:51:14');

-- Dumping structure for table nexopos_v4.ns_nexopos_users_attributes
CREATE TABLE IF NOT EXISTS `ns_nexopos_users_attributes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `avatar_link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `theme` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `language` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_users_attributes: ~1 rows (approximately)
INSERT INTO `ns_nexopos_users_attributes` (`id`, `user_id`, `avatar_link`, `theme`, `language`) VALUES
	(1, 85, NULL, NULL, 'en');

-- Dumping structure for table nexopos_v4.ns_nexopos_users_roles_relations
CREATE TABLE IF NOT EXISTS `ns_nexopos_users_roles_relations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_users_roles_relations: ~2 rows (approximately)
INSERT INTO `ns_nexopos_users_roles_relations` (`id`, `role_id`, `user_id`, `created_at`, `updated_at`) VALUES
	(1, 2, 85, '2025-02-18 03:01:17', '2025-02-18 03:01:17'),
	(2, 5, 86, '2025-02-18 12:40:50', '2025-02-18 12:40:50');

-- Dumping structure for table nexopos_v4.ns_nexopos_users_widgets
CREATE TABLE IF NOT EXISTS `ns_nexopos_users_widgets` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `column` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_nexopos_users_widgets: ~8 rows (approximately)
INSERT INTO `ns_nexopos_users_widgets` (`id`, `identifier`, `column`, `class_name`, `position`, `user_id`, `created_at`, `updated_at`) VALUES
	('9e3d724b-cd00-435d-8fc2-4013925d4e40', 'nsBestCustomers', 'first-column', 'App\\Widgets\\BestCustomersWidget', 0, 85, '2025-02-18 03:01:17', '2025-02-18 08:37:39'),
	('9e3d724b-d13c-454f-be1d-a2be0096d0ee', 'nsOrdersSummary', 'first-column', 'App\\Widgets\\OrdersSummaryWidget', 1, 85, '2025-02-18 03:01:17', '2025-02-18 08:37:39'),
	('9e3d724b-d7d2-415c-b94a-001e793fba8a', 'nsProfileWidget', 'second-column', 'App\\Widgets\\ProfileWidget', 0, 85, '2025-02-18 03:01:17', '2025-02-18 08:37:43'),
	('9e3d724b-da02-4bfd-8d70-5a0725f11783', 'nsBestCashiers', 'second-column', 'App\\Widgets\\BestCashiersWidget', 1, 85, '2025-02-18 03:01:17', '2025-02-18 08:37:44'),
	('9e3d724b-e026-4a21-81bf-6ee331460436', 'nsOrdersChart', 'third-column', 'App\\Widgets\\OrdersChartWidget', 0, 85, '2025-02-18 03:01:17', '2025-02-18 08:37:45'),
	('9e3d7ee2-64a6-4bbd-bc49-6d2e89bd7ced', 'nsIncompleteSaleCardWidget', 'third-column', 'App\\Widgets\\IncompleteSaleCardWidget', 1, 85, '2025-02-18 09:06:29', '2025-02-18 09:06:29'),
	('9e3d7eec-04a0-48dc-98b5-b56a304e7fd6', 'nsSaleCardWidget', 'third-column', 'App\\Widgets\\SaleCardWidget', 2, 85, '2025-02-18 09:06:35', '2025-02-18 09:06:35'),
	('9e3d7eec-1624-4974-89ca-0b0691393e5c', 'nsExpenseCardWidget', 'third-column', 'App\\Widgets\\ExpenseCardWidget', 3, 85, '2025-02-18 09:06:35', '2025-02-18 09:06:35');

-- Dumping structure for table nexopos_v4.ns_personal_access_tokens
CREATE TABLE IF NOT EXISTS `ns_personal_access_tokens` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tokenable_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tokenable_id` bigint unsigned NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `abilities` text COLLATE utf8mb4_unicode_ci,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ns_personal_access_tokens_token_unique` (`token`),
  KEY `ns_personal_access_tokens_tokenable_type_tokenable_id_index` (`tokenable_type`,`tokenable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_personal_access_tokens: ~0 rows (approximately)

-- Dumping structure for table nexopos_v4.ns_telescope_entries
CREATE TABLE IF NOT EXISTS `ns_telescope_entries` (
  `sequence` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uuid` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `family_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `should_display_on_index` tinyint(1) NOT NULL DEFAULT '1',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`sequence`),
  UNIQUE KEY `ns_telescope_entries_uuid_unique` (`uuid`),
  KEY `ns_telescope_entries_batch_id_index` (`batch_id`),
  KEY `ns_telescope_entries_family_hash_index` (`family_hash`),
  KEY `ns_telescope_entries_created_at_index` (`created_at`),
  KEY `ns_telescope_entries_type_should_display_on_index_index` (`type`,`should_display_on_index`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_telescope_entries: ~72 rows (approximately)
INSERT INTO `ns_telescope_entries` (`sequence`, `uuid`, `batch_id`, `family_hash`, `should_display_on_index`, `type`, `content`, `created_at`) VALUES
	(1, '9e3da959-ddf3-4a17-b864-46bf5cc84c55', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"missed","key":"ns-core-installed","hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(2, '9e3da959-f252-458e-a72e-6f1541c93849', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select exists (select 1 from information_schema.tables where table_schema = \'nexopos_v4\' and table_name = \'ns_nexopos_options\' and table_type in (\'BASE TABLE\', \'SYSTEM VERSIONED\')) as `exists`","time":"2.34","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\Helpers\\\\App.php","line":32,"hash":"7e2aad867aa292f920ab7ae6656ee5d1","hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(3, '9e3da959-f440-4e77-a5f7-6a7ec1d371af', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"set","key":"ns-core-installed","value":true,"expiration":60,"hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(4, '9e3da959-f81a-450b-b505-6ffc7a4f7a55', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(5, '9e3da959-fbbc-4020-b639-a2e6db2db8a1', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(6, '9e3da959-fc25-4691-b383-e95568b2c270', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(7, '9e3da95a-02d9-480d-8763-1d2b919992ab', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_options` where `user_id` is null","time":"1.05","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\Options.php","line":73,"hash":"7fa0c03ff650c047190cc908c6d90148","hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(8, '9e3da95a-032f-40b9-b80b-b1f9668199be', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Option","count":21,"hostname":"ghazi"}', '2025-02-18 11:05:14'),
	(9, '9e3da95a-075c-4f94-b8b7-3718cca368bd', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(10, '9e3da95a-07c1-4206-a1c8-b04743a5f25a', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(11, '9e3da95a-1f88-4caf-aac7-a9b9d41d4ee4', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(12, '9e3da95a-1ffd-4014-9d58-0762fe203022', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(13, '9e3da95a-217a-4f93-af1c-eaa7c2af1f6f', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_permissions`","time":"1.21","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\CoreService.php","line":271,"hash":"6db72b3a4f8a62988846d2963583bfdd","hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(14, '9e3da95a-21c5-4978-8f98-b5e9bfb5b1cd', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Permission","count":120,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(15, '9e3da95a-29e0-4d4f-9d89-3f38e5c02bfb', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(16, '9e3da95a-2a4c-4052-9612-79204853d223', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(17, '9e3da95a-2b10-44d1-b2d9-11adecfedb28', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(18, '9e3da95a-2b79-4f11-8cc1-5a2ea17fc591', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(19, '9e3da95a-34ec-4569-be4f-d445f0dafb8b', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'event', '{"name":"App\\\\Events\\\\ModulesLoadedEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(20, '9e3da95a-3d98-4570-bb81-2003f505bfd4', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'event', '{"name":"App\\\\Events\\\\ModulesBootedEvent","payload":{"socket":null},"listeners":[{"name":"Closure at C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Providers\\\\AppServiceProvider.php[248:250]","queued":false}],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(21, '9e3da95a-3eb4-41ea-8742-85d716d9b893', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(22, '9e3da95a-3f1a-45d0-917b-eea2484c3e1c', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(23, '9e3da95a-401e-45e0-9d50-b7a9d06942a2', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_roles` where `namespace` = \'admin\' limit 1","time":"0.71","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Models\\\\Role.php","line":91,"hash":"35bbcf889902a0c8034d18d8d715ee4a","hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(24, '9e3da95a-4069-48cb-9678-15fe4c94657a', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Role","count":1,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(25, '9e3da95a-44b6-42cf-9f20-0620bde89cf5', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select `ns_nexopos_users`.*, `ns_nexopos_users_roles_relations`.`role_id` as `laravel_through_key` from `ns_nexopos_users` inner join `ns_nexopos_users_roles_relations` on `ns_nexopos_users_roles_relations`.`user_id` = `ns_nexopos_users`.`id` where `ns_nexopos_users_roles_relations`.`role_id` = 2","time":"0.85","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Providers\\\\TelescopeServiceProvider.php","line":65,"hash":"639d96d7fb840eba35e169e33d87a123","hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(26, '9e3da95a-4506-4ed7-8585-d8cc7e91b1d8', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\User","count":1,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(27, '9e3da95a-569c-4123-be0e-3b456306826a', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'event', '{"name":"App\\\\Events\\\\BeforeStartApiRouteEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(28, '9e3da95a-64a6-421a-a61f-b8ec34d0536a', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'event', '{"name":"App\\\\Events\\\\BeforeStartWebRouteEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(29, '9e3da95a-6658-450d-b6f5-d9b208be8456', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'event', '{"name":"App\\\\Events\\\\WebRoutesLoadedEvent","payload":{"route":"dashboard","socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(30, '9e3da95a-9f53-4a48-93d1-0f27964cfb60', '9e3da95a-b868-47ef-855d-da0405f3b142', NULL, 1, 'request', '{"ip_address":"127.0.0.1","uri":"\\/api\\/procurements\\/products\\/search-procurement-product","method":"POST","controller_action":"App\\\\Http\\\\Controllers\\\\Dashboard\\\\ProcurementController@searchProcurementProduct","middleware":["api","App\\\\Http\\\\Middleware\\\\InstalledStateMiddleware","Illuminate\\\\Routing\\\\Middleware\\\\SubstituteBindings","App\\\\Http\\\\Middleware\\\\ClearRequestCacheMiddleware","auth:sanctum"],"headers":{"host":"nexopos.test","connection":"keep-alive","content-length":"18","x-xsrf-token":"eyJpdiI6IjIxc3l3QUkxWDMycndPVmk5RFNDZHc9PSIsInZhbHVlIjoiZXYrUzNCbHBHV0ZobkJjcnpSMkVrY2VLbXdQbm9UM24yWjhIQkVDeTZiVlF5all0a243THNUb2NoY0E4NFBLY0s5bDFxWkNONUZBbVA2NHkwaWZsclc5VmlEelhoRXkzOVNSVVEwYU1wSm1Kc2xCZEYyK1hiRWdSUFJ1cWdTWFUiLCJtYWMiOiIxZDk5N2FjZTczN2VkMzMyMDhkM2RhMjUzYTU3MjM1ZGRlNjZkMmRhYjA3NGMwMGY1YTg0YWQzMGRhMDY3YmM1IiwidGFnIjoiIn0=","x-requested-with":"XMLHttpRequest","user-agent":"Mozilla\\/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit\\/537.36 (KHTML, like Gecko) Chrome\\/133.0.0.0 Safari\\/537.36 Edg\\/133.0.0.0","accept":"application\\/json, text\\/plain, *\\/*","content-type":"application\\/json","origin":"http:\\/\\/nexopos.test","referer":"http:\\/\\/nexopos.test\\/dashboard\\/products\\/stock-adjustment","accept-encoding":"gzip, deflate","accept-language":"en-US,en;q=0.9,en-IN;q=0.8","cookie":"XSRF-TOKEN=eyJpdiI6IjIxc3l3QUkxWDMycndPVmk5RFNDZHc9PSIsInZhbHVlIjoiZXYrUzNCbHBHV0ZobkJjcnpSMkVrY2VLbXdQbm9UM24yWjhIQkVDeTZiVlF5all0a243THNUb2NoY0E4NFBLY0s5bDFxWkNONUZBbVA2NHkwaWZsclc5VmlEelhoRXkzOVNSVVEwYU1wSm1Kc2xCZEYyK1hiRWdSUFJ1cWdTWFUiLCJtYWMiOiIxZDk5N2FjZTczN2VkMzMyMDhkM2RhMjUzYTU3MjM1ZGRlNjZkMmRhYjA3NGMwMGY1YTg0YWQzMGRhMDY3YmM1IiwidGFnIjoiIn0%3D; nexopos_session=eyJpdiI6IlZlVTBJRURZY09oM2Rkb3VYUmVYWXc9PSIsInZhbHVlIjoiQjhaTW9FSGdQS0tUVW1DamJrbHhuUjN3ZUlPakFKNkhzVDdxaityZXpDczNPUGdZQ3BrOUowWDN3cm5wbUhOQTJuS0xjUjRIQVAwaVk0NzFWTGhJTEJ3ZWUzUUpKT1dobjZsRFl2U1hPNVExbHZiMWQ3UmpSN085dndQSlBGNW4iLCJtYWMiOiIwM2QxYzQyYmIzYmE0ZWFmNTNiOWViNzMzZDBjNzJjYjRhOTVkODVkOGMxYzNiN2U3MjUwNWNlYjM2ODlmMjFkIiwidGFnIjoiIn0%3D"},"payload":{"argument":"reg"},"session":{"_token":"7B55hyr9gjE83Q2IcvJOZ27nRkZL1O5oA7yNa9to","_flash":{"old":[],"new":[]}},"response_headers":{"cache-control":"no-cache, private","date":"Tue, 18 Feb 2025 11:05:14 GMT","content-type":"application\\/json","access-control-allow-origin":"*","set-cookie":"laravel_session=eyJpdiI6InlCcDZNV0ErVjhQZVl2ZVAwL3ZWZXc9PSIsInZhbHVlIjoic0w4eWpIL2lwTGpTa3EzWUxUY2lJYjQ0QXpiZHoxSzNpU1RkT3N4eFVVZEhUNFJFVkJhaWVTSkVaYnl5NDVGRW5sM1VQSEFiT0x6ZUdjNkdFOTVuMzdwZWk4UWVab01qOFZIMVVRUE1Rc1l2enNuWGZSUkpZeEJZbGVCcksrNloiLCJtYWMiOiI5OGQxOWU5MTk5ODNiYmYzM2M2MmZkMTJhMmZkNzlmMjEwNzU3ZTE2OTI5MzM0ZTc4MmNhYzdhYjM0YTBhYzY3IiwidGFnIjoiIn0%3D; expires=Tue, 18 Feb 2025 13:05:14 GMT; Max-Age=7200; path=\\/; httponly; samesite=lax"},"response_status":500,"response":{"status":"error","message":"CSRF token mismatch.","previous":"http:\\/\\/nexopos.test\\/dashboard\\/products\\/stock-adjustment"},"duration":1111,"memory":32,"hostname":"ghazi"}', '2025-02-18 16:35:14'),
	(31, '9e3dcc9d-4ab1-48f8-9513-2f5c0c5ec425', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"missed","key":"ns-core-installed","hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(32, '9e3dcc9d-78e9-4fd6-b8f8-a28f9c40b408', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select exists (select 1 from information_schema.tables where table_schema = \'nexopos_v4\' and table_name = \'ns_nexopos_options\' and table_type in (\'BASE TABLE\', \'SYSTEM VERSIONED\')) as `exists`","time":"2.07","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\Helpers\\\\App.php","line":32,"hash":"7e2aad867aa292f920ab7ae6656ee5d1","hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(33, '9e3dcc9d-7bcc-42ee-a511-af52d5d66986', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"set","key":"ns-core-installed","value":true,"expiration":60,"hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(34, '9e3dcc9d-8942-430f-92bc-42364584f9f2', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(35, '9e3dcc9d-8cec-4746-a335-414922e5a384', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(36, '9e3dcc9d-8ddb-4312-a84f-8c75313efdf1', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(37, '9e3dcc9d-9637-400c-89ef-8483452cc242', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_options` where `user_id` is null","time":"0.94","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\Options.php","line":73,"hash":"7fa0c03ff650c047190cc908c6d90148","hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(38, '9e3dcc9d-9692-46e1-b6a6-5947b3bfaaf2', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Option","count":21,"hostname":"ghazi"}', '2025-02-18 12:43:50'),
	(39, '9e3dcc9d-9cae-48b0-b7bd-703c94ea3af2', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(40, '9e3dcc9d-9d20-4bd5-8777-55cd9c661348', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(41, '9e3dcc9d-cdc8-441c-ab3c-4ccdcad1d415', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(42, '9e3dcc9d-ce36-42ab-b92c-22f105ae866e', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(43, '9e3dcc9d-d3c6-42ac-b75c-7752c8b04ce6', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_permissions`","time":"8.00","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Services\\\\CoreService.php","line":271,"hash":"6db72b3a4f8a62988846d2963583bfdd","hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(44, '9e3dcc9d-d40f-47ab-868b-6829c13bec8c', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Permission","count":120,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(45, '9e3dcc9d-e080-4ca6-8aba-dbe118cf2054', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(46, '9e3dcc9d-e189-4bcf-839b-b698591dc5b5', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(47, '9e3dcc9d-ec7c-4c22-a738-13b549a773a5', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(48, '9e3dcc9d-ece5-4b9d-8225-8db76d6323a1', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(49, '9e3dcc9d-fada-4916-9a9b-65f0f7ca8e99', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\ModulesLoadedEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 18:13:50'),
	(50, '9e3dcc9e-1860-4676-812f-a2b5539783e1', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\ModulesBootedEvent","payload":{"socket":null},"listeners":[{"name":"Closure at C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Providers\\\\AppServiceProvider.php[248:250]","queued":false}],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(51, '9e3dcc9e-1971-4264-b05e-06585be403e0', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(52, '9e3dcc9e-1a8d-4585-a034-1fe0c5f51919', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(53, '9e3dcc9e-1bcf-4318-a022-fc56f2ed9162', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_roles` where `namespace` = \'admin\' limit 1","time":"0.71","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Models\\\\Role.php","line":91,"hash":"35bbcf889902a0c8034d18d8d715ee4a","hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(54, '9e3dcc9e-1c19-4a54-8077-633a6f167aed', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Role","count":1,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(55, '9e3dcc9e-21ee-4707-9cd1-55e9bc0946ec', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select `ns_nexopos_users`.*, `ns_nexopos_users_roles_relations`.`role_id` as `laravel_through_key` from `ns_nexopos_users` inner join `ns_nexopos_users_roles_relations` on `ns_nexopos_users_roles_relations`.`user_id` = `ns_nexopos_users`.`id` where `ns_nexopos_users_roles_relations`.`role_id` = 2","time":"0.87","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Providers\\\\TelescopeServiceProvider.php","line":65,"hash":"639d96d7fb840eba35e169e33d87a123","hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(56, '9e3dcc9e-2238-4a98-9eab-bdd4bb00cd3a', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\User","count":3,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(57, '9e3dcc9e-3869-49f0-a271-23a065f1662d', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\BeforeStartApiRouteEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(58, '9e3dcc9e-412a-4cd6-baae-47b1e30aad45', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\BeforeStartWebRouteEvent","payload":{"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(59, '9e3dcc9e-487b-4d6b-b66c-08aad177c1c0', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\WebRoutesLoadedEvent","payload":{"route":"dashboard","socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(60, '9e3dcc9e-6b86-4f4d-87e5-c09009503e75', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_users` where `id` = 85 limit 1","time":"1.21","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\public\\\\index.php","line":17,"hash":"ee7c24482f5330a1d5b1b78b8ee2974a","hostname":"ghazi"}', '2025-02-18 18:13:51'),
	(61, '9e3dcc9e-6e9c-47a4-ac0d-cda544150487', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_users_attributes` where `ns_nexopos_users_attributes`.`user_id` = 85 and `ns_nexopos_users_attributes`.`user_id` is not null limit 1","time":"0.68","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Http\\\\Middleware\\\\LoadLangMiddleware.php","line":22,"hash":"0cb95079e536fadefd0915c6fd1244ca","hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(62, '9e3dcc9e-6eee-4c83-a0fd-aeb92cdf8403', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\UserAttribute","count":1,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(63, '9e3dcc9e-6fb5-44c2-98a3-3d58e55a8677', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\LocaleDefinedEvent","payload":{"locale":"en","socket":null},"listeners":[{"name":"Closure at C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Providers\\\\LocalizationServiceProvider.php[20:22]","queued":false}],"broadcast":false,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(64, '9e3dcc9e-71ac-422b-a84d-a6127974f397', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\InstalledStateBeforeCheckedEvent","payload":{"next":{"class":"Closure","properties":[]},"request":{"class":"Illuminate\\\\Http\\\\Request","properties":{"attributes":[],"request":[],"query":[],"server":[],"files":[],"cookies":[],"headers":[]}},"socket":null},"listeners":[],"broadcast":false,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(65, '9e3dcc9e-722a-4cb3-a4f1-fe49ac6f7ad8', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(66, '9e3dcc9e-7292-451a-abee-9bc7d79cb7d9', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"hit","key":"ns-core-installed","value":true,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(67, '9e3dcc9e-7438-4a20-89b1-e1640871ce19', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_orders` order by `created_at` desc limit 10","time":"1.10","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Http\\\\Controllers\\\\DashboardController.php","line":53,"hash":"add78fb1886025a84c971b7c006f0546","hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(68, '9e3dcc9e-7496-44ba-be33-83a10ce89f5f', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'model', '{"action":"retrieved","model":"App\\\\Models\\\\Order","count":2,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(69, '9e3dcc9e-7552-40c8-a326-41662cad32d4', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'query', '{"connection":"mysql","bindings":[],"sql":"select * from `ns_nexopos_users` where `ns_nexopos_users`.`id` in (85)","time":"0.75","slow":false,"file":"C:\\\\laragon\\\\www\\\\nexopos\\\\app\\\\Http\\\\Controllers\\\\DashboardController.php","line":53,"hash":"55896bef56d7295125d62636691a25c0","hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(70, '9e3dcc9e-864d-4beb-9b3f-150813c2ec4c', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'cache', '{"type":"forget","key":"ns-core-installed","hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(71, '9e3dcc9e-87ac-4511-9bad-8d3d8ed0fede', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'event', '{"name":"App\\\\Events\\\\ResponseReadyEvent","payload":{"response":{"class":"Illuminate\\\\Http\\\\JsonResponse","properties":{"headers":[],"original":[{"id":2,"description":null,"code":"250218-002","title":null,"type":"takeaway","payment_status":"paid","process_status":"not-available","delivery_status":"not-available","discount":25,"discount_type":"flat","support_instalments":true,"discount_percentage":0,"shipping":0,"shipping_rate":0,"shipping_type":null,"total_without_tax":700,"subtotal":725,"total_with_tax":700,"total_coupons":0,"total_cogs":0,"total":700,"tax_value":0,"products_tax_value":0,"tax_group_id":null,"tax_type":"0","tendered":700,"change":0,"final_payment_date":null,"total_instalments":0,"customer_id":86,"note":null,"note_visibility":"hidden","author":85,"uuid":null,"register_id":null,"voidance_reason":null,"created_at":"2025-02-18 18:13:08","updated_at":"2025-02-18 18:13:08","user":{"id":85,"username":"Ghazi","active":true,"author":85,"email":"sayyedghazi3@gmail.com","group_id":null,"first_name":null,"last_name":null,"gender":null,"phone":null,"pobox":null,"activation_expiration":null,"total_sales_count":2,"total_sales":1250,"birth_date":null,"purchases_amount":0,"owed_amount":0,"credit_limit_amount":0,"account_amount":0,"activation_token":null,"created_at":"2025-02-18T03:01:17.000000Z","updated_at":"2025-02-18T12:43:08.000000Z"}},{"id":1,"description":null,"code":"250218-001","title":null,"type":"takeaway","payment_status":"paid","process_status":"not-available","delivery_status":"not-available","discount":0,"discount_type":null,"support_instalments":true,"discount_percentage":0,"shipping":0,"shipping_rate":0,"shipping_type":null,"total_without_tax":550,"subtotal":550,"total_with_tax":550,"total_coupons":0,"total_cogs":0,"total":550,"tax_value":0,"products_tax_value":0,"tax_group_id":null,"tax_type":"0","tendered":550,"change":0,"final_payment_date":null,"total_instalments":0,"customer_id":86,"note":null,"note_visibility":"hidden","author":85,"uuid":null,"register_id":null,"voidance_reason":null,"created_at":"2025-02-18 18:11:37","updated_at":"2025-02-18 18:11:37","user":{"id":85,"username":"Ghazi","active":true,"author":85,"email":"sayyedghazi3@gmail.com","group_id":null,"first_name":null,"last_name":null,"gender":null,"phone":null,"pobox":null,"activation_expiration":null,"total_sales_count":2,"total_sales":1250,"birth_date":null,"purchases_amount":0,"owed_amount":0,"credit_limit_amount":0,"account_amount":0,"activation_token":null,"created_at":"2025-02-18T03:01:17.000000Z","updated_at":"2025-02-18T12:43:08.000000Z"}}],"exception":null}},"socket":null},"listeners":[{"name":"App\\\\Listeners\\\\ResponseReadyEventListener@handle","queued":false}],"broadcast":false,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51'),
	(72, '9e3dcc9e-8ab9-482d-bb6f-5b5820e15cb0', '9e3dcc9e-8de1-4f29-b1c8-8a0fcc2481d7', NULL, 1, 'request', '{"ip_address":"127.0.0.1","uri":"\\/api\\/dashboard\\/recent-orders","method":"GET","controller_action":"App\\\\Http\\\\Controllers\\\\DashboardController@getRecentsOrders","middleware":["api","App\\\\Http\\\\Middleware\\\\InstalledStateMiddleware","Illuminate\\\\Routing\\\\Middleware\\\\SubstituteBindings","App\\\\Http\\\\Middleware\\\\ClearRequestCacheMiddleware","auth:sanctum"],"headers":{"host":"nexopos.test","connection":"keep-alive","x-xsrf-token":"eyJpdiI6ImhML1Y0MjFFdFlDSlZudFdHTldaSEE9PSIsInZhbHVlIjoid0ZUdFFNT205eVUvakJ6d2xWS1N5OEhUa013enlEMlQ3dEtidkxURE5PTmlweUNWR08waXV1OVYxa09MTUlieFlUeWJ5QnNrdCtseG1RUkYrOFY4L2g5UUc3Nll0aGs1YkhSOTBINUZCNWE5QWphdU9EMzFOSGZMUzhuaDc4WmQiLCJtYWMiOiJiMGFhMjUzNTgwNzU0ZjA2MTU0OTczODA1ZDA0YjcwNGRmODFhM2FmOTI5NWY3YmNiNGYyYTUwYjFkZjIwODFhIiwidGFnIjoiIn0=","x-requested-with":"XMLHttpRequest","user-agent":"Mozilla\\/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit\\/537.36 (KHTML, like Gecko) Chrome\\/132.0.0.0 Safari\\/537.36","accept":"application\\/json, text\\/plain, *\\/*","referer":"http:\\/\\/nexopos.test\\/dashboard","accept-encoding":"gzip, deflate","accept-language":"en-US,en;q=0.9","cookie":"XSRF-TOKEN=eyJpdiI6ImhML1Y0MjFFdFlDSlZudFdHTldaSEE9PSIsInZhbHVlIjoid0ZUdFFNT205eVUvakJ6d2xWS1N5OEhUa013enlEMlQ3dEtidkxURE5PTmlweUNWR08waXV1OVYxa09MTUlieFlUeWJ5QnNrdCtseG1RUkYrOFY4L2g5UUc3Nll0aGs1YkhSOTBINUZCNWE5QWphdU9EMzFOSGZMUzhuaDc4WmQiLCJtYWMiOiJiMGFhMjUzNTgwNzU0ZjA2MTU0OTczODA1ZDA0YjcwNGRmODFhM2FmOTI5NWY3YmNiNGYyYTUwYjFkZjIwODFhIiwidGFnIjoiIn0%3D; nexopos_session=eyJpdiI6IktpeDVvYlo4Vzh6K3lndkhzNENPV3c9PSIsInZhbHVlIjoicjJ2Q3Mva3diUW9vSjBCakEzWVl1dldKWDFxZnNVdEVFbllWQlJTd0tkQU9ndnpaWmsxTm9aUnhFUkJzQmE0TWJGdk81c2JUSXdlYnhLY1lwYmhscEVlcTB3eDZUZ3Y0b0Jucm9tRkV3ZlIwdDBMbnhtbTcyL21ZQ1JYVWlmTWwiLCJtYWMiOiJkYzcyZmM4ZGQzMjYwNmMyY2Y2ZjdhMWIwNWJmYTU1ZGE5MDRhMThiMDgxMjk2ZWM1MWJlYmJhMjhmM2Q1ODgwIiwidGFnIjoiIn0%3D"},"payload":[],"session":{"_token":"cBF1u2BykKuZs8BqPXj1PGEYQajMiKdp0O1er1nC","_previous":{"url":"http:\\/\\/nexopos.test\\/dashboard"},"_flash":{"old":[],"new":[]},"login_web_59ba36addc2b2f9401580f014c7f58ea4e30989d":85,"password_hash_web":"$2y$10$P0FLRjmRtvMGcgLfwmJ2nuiB39YraWOvdEuttaeXNOtY9d\\/YuvgfO"},"response_headers":{"cache-control":"no-cache, private","date":"Tue, 18 Feb 2025 12:43:51 GMT","content-type":"application\\/json","access-control-allow-origin":"*","set-cookie":"XSRF-TOKEN=eyJpdiI6Ii9KbHp0UFdQanEreXpMaWoyUkZLNUE9PSIsInZhbHVlIjoicXBMYVB3MzFsdm51SVRBdHpqNVFqNVBLNkNBZnRvRjRkZEhQZDV5NmNFMi9jeGNTRjcyeFVFT1RaclJFU25ROUdyeEh4ZDduRkM2eDl1cklqTjd2NmRPcGdQT1gyTmN6cm9OTFpZTklTSkswdmh1WEs2bmEvcE0rRW5ML3FzWGwiLCJtYWMiOiJjMGRlNDg5NDJiM2I0ZDRiM2I4ZWQwODRhZmFmN2NhOWU5NWI0NGE1OWI0NDFkZDBhMmQxZjAyYjRiZGFkZDVlIiwidGFnIjoiIn0%3D; expires=Tue, 18 Feb 2025 14:43:51 GMT; Max-Age=7200; path=\\/; samesite=lax, nexopos_session=eyJpdiI6InpoVXZpN214dnpTank5R3ZtdnBHL0E9PSIsInZhbHVlIjoidHAzTm12ekJ2YVh2SlozTGdoSVNYWXIyZVlyN1VQcmxNR2NzelpsT3hkQzZSdUQxb1V1M1I3WHpKOXVVbVFlamJ0YjM0OTlvei9wTGJ1cVA0RUhpOE9TVUhCS2NyeHU3NnVnQmJUNEdVZ05YSW9UaWV3RU1yK3crcjRxazQycnciLCJtYWMiOiIxMWY3MTgxOGUwOWE4N2Q4NzlkMWJhYWMzZWE0M2Y4MjU4MTdlMzUzNzBmMjJlYmUwMWIyZTUyYjlhNjAyYWVhIiwidGFnIjoiIn0%3D; expires=Tue, 18 Feb 2025 14:43:51 GMT; Max-Age=7200; path=\\/; httponly; samesite=lax"},"response_status":200,"response":[{"id":2,"description":null,"code":"250218-002","title":null,"type":"takeaway","payment_status":"paid","process_status":"not-available","delivery_status":"not-available","discount":25,"discount_type":"flat","support_instalments":true,"discount_percentage":0,"shipping":0,"shipping_rate":0,"shipping_type":null,"total_without_tax":700,"subtotal":725,"total_with_tax":700,"total_coupons":0,"total_cogs":0,"total":700,"tax_value":0,"products_tax_value":0,"tax_group_id":null,"tax_type":"0","tendered":700,"change":0,"final_payment_date":null,"total_instalments":0,"customer_id":86,"note":null,"note_visibility":"hidden","author":85,"uuid":null,"register_id":null,"voidance_reason":null,"created_at":"2025-02-18 18:13:08","updated_at":"2025-02-18 18:13:08","user":{"id":85,"username":"Ghazi","active":true,"author":85,"email":"sayyedghazi3@gmail.com","group_id":null,"first_name":null,"last_name":null,"gender":null,"phone":null,"pobox":null,"activation_expiration":null,"total_sales_count":2,"total_sales":1250,"birth_date":null,"purchases_amount":0,"owed_amount":0,"credit_limit_amount":0,"account_amount":0,"activation_token":null,"created_at":"2025-02-18T03:01:17.000000Z","updated_at":"2025-02-18T12:43:08.000000Z"}},{"id":1,"description":null,"code":"250218-001","title":null,"type":"takeaway","payment_status":"paid","process_status":"not-available","delivery_status":"not-available","discount":0,"discount_type":null,"support_instalments":true,"discount_percentage":0,"shipping":0,"shipping_rate":0,"shipping_type":null,"total_without_tax":550,"subtotal":550,"total_with_tax":550,"total_coupons":0,"total_cogs":0,"total":550,"tax_value":0,"products_tax_value":0,"tax_group_id":null,"tax_type":"0","tendered":550,"change":0,"final_payment_date":null,"total_instalments":0,"customer_id":86,"note":null,"note_visibility":"hidden","author":85,"uuid":null,"register_id":null,"voidance_reason":null,"created_at":"2025-02-18 18:11:37","updated_at":"2025-02-18 18:11:37","user":{"id":85,"username":"Ghazi","active":true,"author":85,"email":"sayyedghazi3@gmail.com","group_id":null,"first_name":null,"last_name":null,"gender":null,"phone":null,"pobox":null,"activation_expiration":null,"total_sales_count":2,"total_sales":1250,"birth_date":null,"purchases_amount":0,"owed_amount":0,"credit_limit_amount":0,"account_amount":0,"activation_token":null,"created_at":"2025-02-18T03:01:17.000000Z","updated_at":"2025-02-18T12:43:08.000000Z"}}],"duration":2266,"memory":32,"hostname":"ghazi","user":{"id":85,"name":null,"email":"sayyedghazi3@gmail.com"}}', '2025-02-18 18:13:51');

-- Dumping structure for table nexopos_v4.ns_telescope_entries_tags
CREATE TABLE IF NOT EXISTS `ns_telescope_entries_tags` (
  `entry_uuid` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  KEY `ns_telescope_entries_tags_entry_uuid_tag_index` (`entry_uuid`,`tag`),
  KEY `ns_telescope_entries_tags_tag_index` (`tag`),
  CONSTRAINT `ns_telescope_entries_tags_entry_uuid_foreign` FOREIGN KEY (`entry_uuid`) REFERENCES `ns_telescope_entries` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_telescope_entries_tags: ~22 rows (approximately)
INSERT INTO `ns_telescope_entries_tags` (`entry_uuid`, `tag`) VALUES
	('9e3da95a-032f-40b9-b80b-b1f9668199be', 'App\\Models\\Option'),
	('9e3da95a-21c5-4978-8f98-b5e9bfb5b1cd', 'App\\Models\\Permission'),
	('9e3da95a-4069-48cb-9678-15fe4c94657a', 'App\\Models\\Role'),
	('9e3da95a-4506-4ed7-8585-d8cc7e91b1d8', 'App\\Models\\User'),
	('9e3dcc9d-9692-46e1-b6a6-5947b3bfaaf2', 'App\\Models\\Option'),
	('9e3dcc9d-d40f-47ab-868b-6829c13bec8c', 'App\\Models\\Permission'),
	('9e3dcc9e-1c19-4a54-8077-633a6f167aed', 'App\\Models\\Role'),
	('9e3dcc9e-2238-4a98-9eab-bdd4bb00cd3a', 'App\\Models\\User'),
	('9e3dcc9e-6e9c-47a4-ac0d-cda544150487', 'Auth:85'),
	('9e3dcc9e-6eee-4c83-a0fd-aeb92cdf8403', 'App\\Models\\UserAttribute'),
	('9e3dcc9e-6eee-4c83-a0fd-aeb92cdf8403', 'Auth:85'),
	('9e3dcc9e-6fb5-44c2-98a3-3d58e55a8677', 'Auth:85'),
	('9e3dcc9e-71ac-422b-a84d-a6127974f397', 'Auth:85'),
	('9e3dcc9e-722a-4cb3-a4f1-fe49ac6f7ad8', 'Auth:85'),
	('9e3dcc9e-7292-451a-abee-9bc7d79cb7d9', 'Auth:85'),
	('9e3dcc9e-7438-4a20-89b1-e1640871ce19', 'Auth:85'),
	('9e3dcc9e-7496-44ba-be33-83a10ce89f5f', 'App\\Models\\Order'),
	('9e3dcc9e-7496-44ba-be33-83a10ce89f5f', 'Auth:85'),
	('9e3dcc9e-7552-40c8-a326-41662cad32d4', 'Auth:85'),
	('9e3dcc9e-864d-4beb-9b3f-150813c2ec4c', 'Auth:85'),
	('9e3dcc9e-87ac-4511-9bad-8d3d8ed0fede', 'Auth:85'),
	('9e3dcc9e-8ab9-482d-bb6f-5b5820e15cb0', 'Auth:85');

-- Dumping structure for table nexopos_v4.ns_telescope_monitoring
CREATE TABLE IF NOT EXISTS `ns_telescope_monitoring` (
  `tag` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table nexopos_v4.ns_telescope_monitoring: ~0 rows (approximately)

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
