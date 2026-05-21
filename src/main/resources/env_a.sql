-- Environment A DDL

CREATE TABLE users (
    id       INT          NOT NULL AUTO_INCREMENT,
    email    VARCHAR(255) NOT NULL,
    name     VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE INDEX uq_email (email)
);

CREATE TABLE products (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    price       DECIMAL(10,2),
    category_id INT,
    PRIMARY KEY (id),
    INDEX idx_category (category_id)
);

CREATE TABLE orders (
    id         INT  NOT NULL AUTO_INCREMENT,
    user_id    INT  NOT NULL,
    product_id INT  NOT NULL,
    quantity   INT  DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_user (user_id),
    INDEX idx_product (product_id),
    CONSTRAINT fk_order_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);
