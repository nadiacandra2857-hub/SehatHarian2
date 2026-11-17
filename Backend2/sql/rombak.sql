-- Additive v2 tables. Never reinterpret or rewrite legacy schedules/records.
CREATE TABLE IF NOT EXISTS food_catalog (
 food_id VARCHAR(80) PRIMARY KEY, name VARCHAR(255) NOT NULL, category VARCHAR(255),
 nutrients TEXT NOT NULL, source TEXT NOT NULL, version VARCHAR(80) NOT NULL, license VARCHAR(80) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS food_snapshot (
 meal_id INT PRIMARY KEY, pengguna_id INT NOT NULL, grams DECIMAL(10,2) NOT NULL,
 nutrients TEXT NOT NULL, food_id VARCHAR(80), source TEXT NOT NULL, version VARCHAR(80) NOT NULL,
 FOREIGN KEY(meal_id) REFERENCES kebiasaan_makan(id) ON DELETE CASCADE,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id), INDEX idx_food_owner(pengguna_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sleep_actual (
 id INT AUTO_INCREMENT PRIMARY KEY, pengguna_id INT NOT NULL,
 started_at DATETIME NOT NULL, ended_at DATETIME NOT NULL,
 trigger_name VARCHAR(40), FOREIGN KEY(pengguna_id) REFERENCES pengguna(id),
 INDEX idx_sleep_owner(pengguna_id, ended_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS habit_preferences (
 pengguna_id INT PRIMARY KEY, hydration_ml INT NULL, diet VARCHAR(255), avoid_foods TEXT,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS content_version (
 id INT AUTO_INCREMENT PRIMARY KEY, content_key VARCHAR(80) NOT NULL, kind VARCHAR(20) NOT NULL,
 title VARCHAR(150) NOT NULL, body TEXT NOT NULL, state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 revision INT NOT NULL, created_by INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_content_revision(content_key,revision), FOREIGN KEY(created_by) REFERENCES pengguna(id),
 INDEX idx_content_state(kind,state)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS content_step (
 program_id INT NOT NULL, position INT NOT NULL, movement_id INT NOT NULL,
 target INT NOT NULL, repetitions BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(program_id, position), FOREIGN KEY(program_id) REFERENCES content_version(id),
 FOREIGN KEY(movement_id) REFERENCES content_version(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS habit_challenge (
 id INT AUTO_INCREMENT PRIMARY KEY, pengguna_id INT NOT NULL, content_id INT NOT NULL,
 tanggal DATE NOT NULL, category VARCHAR(20) NOT NULL, target DECIMAL(10,2) NOT NULL,
 reason VARCHAR(255) NOT NULL, completed_at TIMESTAMP NULL,
 UNIQUE KEY uk_challenge_day(pengguna_id,content_id,tanggal),
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id), FOREIGN KEY(content_id) REFERENCES content_version(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS content_guard (id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO content_guard(id) VALUES(1);
