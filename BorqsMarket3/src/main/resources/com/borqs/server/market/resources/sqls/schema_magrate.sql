

-- 加入 cmcc_mm_paid
ALTER TABLE `product_pricetags` ADD COLUMN `cmcc_mm_paid` TINYINT NOT NULL  AFTER `google_iab_sku` , ADD COLUMN `cmcc_mm_price` VARCHAR(2048) NOT NULL  AFTER `cmcc_mm_paid` , CHANGE COLUMN `payment_type` `payment_type` TINYINT(4) NOT NULL DEFAULT '1'  AFTER `updated_at` ;

-- 加入 default_publish_channel
ALTER TABLE `apps` ADD COLUMN `default_publish_channel` VARCHAR(45) NOT NULL  AFTER `operator_id` ;
