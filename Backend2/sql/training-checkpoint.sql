-- Versioned payloads plus permanent terminal rows. Never delete operation tombstones.
CREATE TABLE IF NOT EXISTS training_checkpoint (
 pengguna_id INT NOT NULL,
 operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 status VARCHAR(16) NOT NULL,
 holder_token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 revision BIGINT NOT NULL,
 format_version INT NOT NULL,
 payload MEDIUMBLOB NULL,
 payload_hash CHAR(64) NULL,
 checkpoint_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(pengguna_id,operation_id),
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id),
 INDEX idx_checkpoint_owner_status(pengguna_id,status)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS training_checkpoint_slot (
 pengguna_id INT PRIMARY KEY,
 operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 FOREIGN KEY(pengguna_id,operation_id) REFERENCES training_checkpoint(pengguna_id,operation_id)
) ENGINE=InnoDB;
