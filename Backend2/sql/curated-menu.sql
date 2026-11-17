-- Explicit CMS import receipts only; never a seed or a startup content import.
CREATE TABLE IF NOT EXISTS curated_menu_import (
 import_id BIGINT AUTO_INCREMENT PRIMARY KEY,
 item_key VARCHAR(150) NOT NULL, source_version VARCHAR(80) NOT NULL,
 payload_hash CHAR(64) NOT NULL, menu_id INT NOT NULL,
 UNIQUE KEY uq_curated_version(item_key,source_version),
 UNIQUE KEY uq_curated_menu(menu_id),
 FOREIGN KEY(menu_id) REFERENCES content_version(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_presentation (
 menu_id INT PRIMARY KEY, image_path VARCHAR(255) NOT NULL, caption VARCHAR(255) NOT NULL,
 FOREIGN KEY(menu_id) REFERENCES content_version(id)
) ENGINE=InnoDB;
