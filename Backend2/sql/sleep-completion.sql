-- Additive, no seed. Private plans, sound choices, delivery claims and evaluation outbox.
CREATE TABLE IF NOT EXISTS sleep_active_plan (
 pengguna_id INT PRIMARY KEY, schedule_id INT NULL, generation BIGINT NOT NULL,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id),
 FOREIGN KEY(schedule_id) REFERENCES jadwal_tidur(id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_sound_preference (
 pengguna_id INT PRIMARY KEY, enabled BOOLEAN NOT NULL, volume DOUBLE NOT NULL,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_alarm_claim (
 pengguna_id INT NOT NULL, occurrence DATETIME NOT NULL, kind VARCHAR(20) NOT NULL,
 schedule_identity VARCHAR(255) NOT NULL, claimed_at DATETIME(3) NOT NULL,
 PRIMARY KEY(pengguna_id,occurrence,kind), FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_record_receipt (
 pengguna_id INT NOT NULL, operation_id CHAR(36) NOT NULL, sleep_id INT NULL,
 started_at DATETIME NOT NULL, ended_at DATETIME NOT NULL,
 PRIMARY KEY(pengguna_id,operation_id), FOREIGN KEY(pengguna_id) REFERENCES pengguna(id),
 FOREIGN KEY(sleep_id) REFERENCES sleep_actual(id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_evaluation_request (
 pengguna_id INT NOT NULL, wake_day DATE NOT NULL, revision BIGINT NOT NULL,
 state VARCHAR(24) NOT NULL, source_hash CHAR(64) NULL, result_id BIGINT NULL,
 message TEXT NOT NULL, updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(pengguna_id,wake_day), FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_evaluation_result (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, pengguna_id INT NOT NULL, wake_day DATE NOT NULL,
 revision BIGINT NOT NULL, source_hash CHAR(64) NOT NULL, state VARCHAR(24) NOT NULL,
 summary TEXT NOT NULL, details TEXT NOT NULL, stale BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(3) NOT NULL, UNIQUE KEY uq_sleep_evaluation(pengguna_id,wake_day,revision),
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
