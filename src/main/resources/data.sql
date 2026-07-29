-- Countries
INSERT INTO master_countries(code, name, is_active) VALUES ('IN','India', TRUE);
INSERT INTO master_countries(code, name, is_active) VALUES ('US','United States', TRUE);
INSERT INTO master_countries(code, name, is_active) VALUES ('GB','United Kingdom', TRUE);
INSERT INTO master_countries(code, name, is_active) VALUES ('SG','Singapore', TRUE);
INSERT INTO master_countries(code, name, is_active) VALUES ('DE','Germany', TRUE);

-- Currencies
INSERT INTO master_currencies(code, name, symbol, is_active) VALUES ('INR','Indian Rupee','₹', TRUE);
INSERT INTO master_currencies(code, name, symbol, is_active) VALUES ('USD','US Dollar','$', TRUE);
INSERT INTO master_currencies(code, name, symbol, is_active) VALUES ('GBP','British Pound','£', TRUE);
INSERT INTO master_currencies(code, name, symbol, is_active) VALUES ('EUR','Euro','€', TRUE);
INSERT INTO master_currencies(code, name, symbol, is_active) VALUES ('SGD','Singapore Dollar','S$', TRUE);

-- Channels (SWIFT removed)
INSERT INTO master_channels(code, description, is_active) VALUES ('ONLINE','Digital banking', TRUE);
INSERT INTO master_channels(code, description, is_active) VALUES ('BRANCH','Branch assisted', TRUE);

-- Customer Types
INSERT INTO master_customer_types(code, description, is_active) VALUES ('RETAIL','Individual', TRUE);
INSERT INTO master_customer_types(code, description, is_active) VALUES ('CORPORATE','Business', TRUE);

-- Corridors
INSERT INTO master_corridors(from_country_code, to_country_code, is_active, created_at)
VALUES ('IN','US', TRUE, CURRENT_TIMESTAMP());

INSERT INTO master_corridors(from_country_code, to_country_code, is_active, created_at)
VALUES ('US','IN', TRUE, CURRENT_TIMESTAMP());

INSERT INTO master_corridors(from_country_code, to_country_code, is_active, created_at)
VALUES ('IN','GB', TRUE, CURRENT_TIMESTAMP());

INSERT INTO master_corridors(from_country_code, to_country_code, is_active, created_at)
VALUES ('IN','SG', TRUE, CURRENT_TIMESTAMP());

INSERT INTO master_corridors(from_country_code, to_country_code, is_active, created_at)
VALUES ('IN','DE', TRUE, CURRENT_TIMESTAMP());

-- BUG-035: Dummy recipient bank beneficiaries for simulation
-- These are pre-seeded demo beneficiaries for customer_id = 0 (system/demo)
-- Real customers can add their own via the beneficiaries page
INSERT INTO beneficiaries(customer_id, nick_name, full_name, account_no, bank_name, swift_code, country_code, currency_code, created_at)
VALUES (0, 'John US (Chase)', 'John Michael Smith', 'US12CHAS0000123456789', 'JPMorgan Chase Bank', 'CHASUS33', 'US', 'USD', CURRENT_TIMESTAMP());

INSERT INTO beneficiaries(customer_id, nick_name, full_name, account_no, bank_name, swift_code, country_code, currency_code, created_at)
VALUES (0, 'Emma UK (NatWest)', 'Emma Louise Johnson', 'GB29NWBK60161331926819', 'NatWest Bank UK', 'NWBKGB2L', 'GB', 'GBP', CURRENT_TIMESTAMP());

INSERT INTO beneficiaries(customer_id, nick_name, full_name, account_no, bank_name, swift_code, country_code, currency_code, created_at)
VALUES (0, 'Hans DE (Deutsche)', 'Hans Friedrich Mueller', 'DE89370400440532013000', 'Deutsche Bank AG', 'DEUTDEDB', 'DE', 'EUR', CURRENT_TIMESTAMP());

INSERT INTO beneficiaries(customer_id, nick_name, full_name, account_no, bank_name, swift_code, country_code, currency_code, created_at)
VALUES (0, 'Wei SG (DBS)', 'Wei Liang Tan', 'SG12DBS0000987654321', 'DBS Bank Singapore', 'DBSSSGSG', 'SG', 'SGD', CURRENT_TIMESTAMP());