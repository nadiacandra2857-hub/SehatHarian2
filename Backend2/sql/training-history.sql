-- Additive, idempotent migration. No backfill inferred from activity descriptions.
CREATE TABLE IF NOT EXISTS training_session (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 pengguna_id INT NOT NULL, operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 payload_hash CHAR(64) NOT NULL, activity_id INT NOT NULL,
 started_at DATETIME(3) NOT NULL, ended_at DATETIME(3) NOT NULL,
 active_nanos BIGINT NOT NULL, stopped BOOLEAN NOT NULL,
 program_id INT NULL, program_version INT NULL, program_name VARCHAR(255) NOT NULL,
 UNIQUE KEY uk_training_operation(pengguna_id,operation_id),
 UNIQUE KEY uk_training_activity(activity_id),
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id),
 FOREIGN KEY(activity_id) REFERENCES aktivitas(id),
 INDEX idx_training_owner_end(pengguna_id,ended_at,id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS training_step (
 session_id BIGINT NOT NULL, position INT NOT NULL,
 movement_ref VARCHAR(255) NOT NULL, movement_name VARCHAR(255) NOT NULL,
 media_ref TEXT NOT NULL, source_ref TEXT NOT NULL, category VARCHAR(255) NOT NULL,
 repetitions BOOLEAN NOT NULL, target INT NOT NULL, active_nanos BIGINT NOT NULL,
 outcome VARCHAR(30) NOT NULL, repetition_confirmed BOOLEAN NULL,
 confirmed_repetitions INT NULL,
 PRIMARY KEY(session_id,position), FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB;
