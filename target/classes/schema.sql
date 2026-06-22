-- Table structure for Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    api_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table structure for URLs
CREATE TABLE IF NOT EXISTS urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(20) NOT NULL UNIQUE,
    custom_alias VARCHAR(50) UNIQUE,
    clicks INT DEFAULT 0,
    qr_code TEXT,
    expiry_date TIMESTAMP NULL,
    click_limit INT NULL,
    password VARCHAR(100) NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    CONSTRAINT fk_url_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table structure for Click Analytics
CREATE TABLE IF NOT EXISTS click_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_id BIGINT NOT NULL,
    click_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    referrer VARCHAR(255),
    country VARCHAR(100),
    device VARCHAR(50),
    browser VARCHAR(50),
    ip_address VARCHAR(45),
    CONSTRAINT fk_analytics_url FOREIGN KEY (url_id) REFERENCES urls(id) ON DELETE CASCADE
);
