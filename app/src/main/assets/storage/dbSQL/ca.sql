DROP TABLE IF EXISTS version;
DROP TABLE IF EXISTS handler_htypes;
DROP TABLE IF EXISTS handlers;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS service_layers;
DROP TABLE IF EXISTS service_groups;
DROP TABLE IF EXISTS gui_class;
DROP TABLE IF EXISTS apps;
CREATE TABLE version (dbVersion INT PRIMARY KEY NOT NULL);
CREATE TABLE properties (key VARCHAR PRIMARY KEY NOT NULL, value VARCHAR);
CREATE TABLE services (id INT PRIMARY KEY NOT NULL, class VARCHAR UNIQUE NOT NULL, name VARCHAR NOT NULL);
CREATE TABLE service_layers (service INT NOT NULL REFERENCES services(id), layer INT NOT NULL, PRIMARY KEY (service, layer));
CREATE TABLE service_groups (service INT NOT NULL REFERENCES services(id), `group` VARCHAR NOT NULL, PRIMARY KEY (service, `group`));
CREATE TABLE handlers (id INT NOT NULL PRIMARY KEY, class VARCHAR NOT NULL, storage BOOLEAN NOT NULL);
CREATE TABLE handler_htypes (handler INT NOT NULL REFERENCES handlers(id), type VARCHAR NOT NULL, PRIMARY KEY(handler, type));
CREATE TABLE gui_class (id INT PRIMARY KEY, cname VARCHAR NOT NULL UNIQUE, cpath VARCHAR NOT NULL, cdata BINARY NOT NULL);
CREATE TABLE apps (id INT PRIMARY KEY, app VARCHAR NOT NULL UNIQUE);
INSERT INTO apps (id, app) VALUES(0, 'Unknown.Application');
-- Insert db version
-- change this value here for new databases AND in MainHandler.java (for resetting/upgrading)
INSERT INTO version VALUES(14);
