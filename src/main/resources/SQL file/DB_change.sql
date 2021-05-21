ALTER TABLE `bnb_latest_price_tbl`
CHANGE COLUMN `uuid` `uuid` VARCHAR(24) NOT NULL COLLATE 'utf8_general_ci' FIRST,
CHANGE COLUMN `source` `source` VARCHAR(32) NOT NULL COLLATE 'utf8_general_ci' AFTER `uuid`,
CHANGE COLUMN `contract_type` `contract_type` VARCHAR(32) NOT NULL COLLATE 'utf8_general_ci' AFTER `source`;
    
ALTER TABLE `btc_latest_price_tbl`
CHANGE COLUMN `uuid` `uuid` VARCHAR(24) NOT NULL COLLATE 'utf8_general_ci' FIRST,
CHANGE COLUMN `source` `source` VARCHAR(32) NOT NULL COLLATE 'utf8_general_ci' AFTER `uuid`,
CHANGE COLUMN `contract_type` `contract_type` VARCHAR(32) NOT NULL COLLATE 'utf8_general_ci' AFTER `source`;

ALTER TABLE `bnb_latest_price_tbl`
	DROP INDEX `idx_price_time`,
	ADD INDEX `idx_time_source_contract_type` (`time`, `source`, `contract_type`) USING BTREE;
    
ALTER TABLE `btc_latest_price_tbl`
	DROP INDEX `idx_price_time`,
	ADD INDEX `idx_time_source_contract_type` (`time`, `source`, `contract_type`) USING BTREE;