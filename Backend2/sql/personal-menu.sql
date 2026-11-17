-- Additive mapping only. No menu/recipe fixture and no inferred TempoLife ingredient mapping.
CREATE TABLE IF NOT EXISTS ingredient_definition (
 ingredient_id VARCHAR(80) PRIMARY KEY, name_id VARCHAR(150) NOT NULL,
 origin VARCHAR(24) NOT NULL, assessment VARCHAR(20) NOT NULL,
 allergens TEXT NOT NULL, source TEXT NOT NULL, review_basis TEXT NOT NULL,
 revision BIGINT NOT NULL, reviewed_by INT NULL, updated_at DATETIME(3) NOT NULL,
 FOREIGN KEY(reviewed_by) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ingredient_alias (
 alias_key VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin PRIMARY KEY,
 ingredient_id VARCHAR(80) NOT NULL, FOREIGN KEY(ingredient_id) REFERENCES ingredient_definition(ingredient_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_composition (
 menu_id INT PRIMARY KEY, completeness VARCHAR(20) NOT NULL,
 recipe_servings DOUBLE NULL, source TEXT NOT NULL, review_hash CHAR(64) NULL,
 FOREIGN KEY(menu_id) REFERENCES content_version(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_ingredient (
 menu_id INT NOT NULL, position INT NOT NULL, ingredient_id VARCHAR(80) NULL,
 unresolved_name VARCHAR(255) NOT NULL, quantity DOUBLE NULL, unit VARCHAR(20) NOT NULL,
 grams_per_unit DOUBLE NULL, conversion_source TEXT NOT NULL,
 food_id VARCHAR(80) NULL, food_version VARCHAR(80) NULL,
 nutrition_verified BOOLEAN NOT NULL, basis_source TEXT NOT NULL,
 PRIMARY KEY(menu_id,position), FOREIGN KEY(menu_id) REFERENCES menu_composition(menu_id),
 FOREIGN KEY(ingredient_id) REFERENCES ingredient_definition(ingredient_id),
 FOREIGN KEY(food_id) REFERENCES food_catalog(food_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_preferences (
 pengguna_id INT PRIMARY KEY, revision BIGINT NOT NULL,
 diet VARCHAR(24) NOT NULL, avoid_status VARCHAR(24) NOT NULL,
 restriction_status VARCHAR(24) NOT NULL, allergens TEXT NOT NULL,
 raw_avoid TEXT NOT NULL, raw_restrictions TEXT NOT NULL, unresolved TEXT NOT NULL,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_preference_ingredient (
 pengguna_id INT NOT NULL, ingredient_id VARCHAR(80) NOT NULL, reason VARCHAR(20) NOT NULL,
 PRIMARY KEY(pengguna_id,ingredient_id,reason), FOREIGN KEY(pengguna_id) REFERENCES menu_preferences(pengguna_id),
 FOREIGN KEY(ingredient_id) REFERENCES ingredient_definition(ingredient_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS menu_meal_snapshot (
 meal_id INT PRIMARY KEY, pengguna_id INT NOT NULL, menu_id INT NOT NULL,
 content_key VARCHAR(80) NOT NULL, content_revision INT NOT NULL,
 preference_revision BIGINT NOT NULL, portions DOUBLE NOT NULL, recipe_servings DOUBLE NOT NULL,
 nutrients TEXT NOT NULL, source TEXT NOT NULL, details MEDIUMTEXT NOT NULL,
 review_token CHAR(64) NOT NULL,
 FOREIGN KEY(meal_id) REFERENCES kebiasaan_makan(id) ON DELETE CASCADE,
 FOREIGN KEY(pengguna_id) REFERENCES pengguna(id), FOREIGN KEY(menu_id) REFERENCES content_version(id),
 INDEX idx_menu_meal_owner(pengguna_id)
) ENGINE=InnoDB;
