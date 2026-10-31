CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(254) NOT NULL,
    username   VARCHAR(150) NOT NULL UNIQUE,
    first_name VARCHAR(150) NOT NULL,
    last_name  VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    avatar     VARCHAR(255)
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE auth_tokens (
    token_key VARCHAR(40) PRIMARY KEY,
    user_id   BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE ingredients (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    measurement_unit VARCHAR(64)  NOT NULL,
    CONSTRAINT uq_ingredient UNIQUE (name, measurement_unit)
);
CREATE INDEX ix_ingredients_name ON ingredients (name);

CREATE TABLE recipes (
    id           BIGSERIAL PRIMARY KEY,
    author_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(256) NOT NULL,
    image        VARCHAR(255) NOT NULL,
    text         TEXT         NOT NULL,
    cooking_time INTEGER      NOT NULL CHECK (cooking_time BETWEEN 1 AND 32000)
);
CREATE INDEX ix_recipes_author ON recipes (author_id);

CREATE TABLE recipe_ingredients (
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT  NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    ingredient_id BIGINT  NOT NULL REFERENCES ingredients (id) ON DELETE CASCADE,
    amount        INTEGER NOT NULL CHECK (amount BETWEEN 1 AND 32000),
    CONSTRAINT uq_recipe_ingredient UNIQUE (recipe_id, ingredient_id)
);

CREATE TABLE favorites (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id BIGINT NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorite UNIQUE (user_id, recipe_id)
);
CREATE INDEX ix_favorites_recipe ON favorites (recipe_id);

CREATE TABLE shopping_cart (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id BIGINT NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT uq_shopping_cart UNIQUE (user_id, recipe_id)
);
CREATE INDEX ix_shopping_cart_recipe ON shopping_cart (recipe_id);

CREATE TABLE follows (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_follow UNIQUE (user_id, author_id),
    CONSTRAINT ck_no_self_follow CHECK (user_id <> author_id)
);
CREATE INDEX ix_follows_author ON follows (author_id);
