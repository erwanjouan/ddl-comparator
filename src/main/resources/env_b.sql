-- Environment B DDL (has some differences from A)

CREATE TABLE users (
    id       INT          NOT NULL AUTO_INCREMENT,
    email    VARCHAR(255) NOT NULL,
    name     VARCHAR(255),            -- type changed: VARCHAR(100) -> VARCHAR(255)
    phone    VARCHAR(20),             -- new column
    PRIMARY KEY (id),
    UNIQUE INDEX uq_email (email),
    INDEX idx_name (name)             -- new index
);

CREATE TABLE products (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    price       DECIMAL(12,2),        -- type changed: DECIMAL(10,2) -> DECIMAL(12,2)
    category_id INT,
    PRIMARY KEY (id)
    -- INDEX idx_category removed
);

CREATE TABLE orders (
    id         INT  NOT NULL AUTO_INCREMENT,
    user_id    INT  NOT NULL,
    product_id INT  NOT NULL,
    quantity   INT  DEFAULT 1,
    status     VARCHAR(50) DEFAULT 'PENDING',  -- new column
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user (user_id),            -- type changed: INDEX -> UNIQUE INDEX
    INDEX idx_product (product_id),
    CONSTRAINT fk_order_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE SET NULL,  -- ON DELETE changed
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- New table only in env B
CREATE TABLE audit_log (
    id         INT          NOT NULL AUTO_INCREMENT,
    table_name VARCHAR(100) NOT NULL,
    action     VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_table (table_name)
);
