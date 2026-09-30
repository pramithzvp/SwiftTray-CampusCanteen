-- Schema reference for the ER diagram and viva.
-- Hibernate creates/updates these tables when the application starts.
CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL
);
CREATE TABLE IF NOT EXISTS menu_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL,
  category VARCHAR(30) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  available BOOLEAN NOT NULL,
  active BOOLEAN NOT NULL,
  version BIGINT
);
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  version BIGINT,
  CONSTRAINT fk_order_student FOREIGN KEY (student_id) REFERENCES users(id),
  INDEX idx_order_status_id (status,id)
);
CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_line_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
