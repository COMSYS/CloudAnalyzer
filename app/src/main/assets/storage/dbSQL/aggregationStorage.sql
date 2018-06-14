DROP TABLE IF EXISTS aggregation_data;
DROP TABLE IF EXISTS aggregation_statistics;
DROP TABLE IF EXISTS service_sets_data;
DROP TABLE IF EXISTS service_set_members;
CREATE TABLE aggregation_data (service INT NOT NULL, handler INT NOT NULL, region INT NOT NULL, app INT NOT NULL, day INT NOT NULL, year INT NOT NULL, direction INT NOT NULL, importance INT NOT NULL, link INT NOT NULL, protocol INT NOT NULL, packets INT NOT NULL, bytes INT NOT NULL, PRIMARY KEY(service, handler, region, app, day, year, direction, importance, link, protocol));
CREATE TABLE aggregation_statistics (name VARCHAR NOT NULL, day INT NOT NULL, year INT NOT NULL, app INT NOT NULL, direction INT NOT NULL, importance INT NOT NULL, link INT NOT NULL, protocol INT NOT NULL, value INT NOT NULL, PRIMARY KEY(name, day, year, app, direction, importance, link, protocol));
CREATE TABLE service_sets_data (id INT NOT NULL, app INT NOT NULL, day INT NOT NULL, year INT NOT NULL, direction INT NOT NULL, importance INT NOT NULL, link INT NOT NULL, protocol INT NOT NULL, packets INT NOT NULL, bytes INT NOT NULL, PRIMARY KEY(id, app, day, year, direction, importance, link, protocol));
CREATE TABLE service_set_members (`set` INT NOT NULL, service INT NOT NULL, PRIMARY KEY(`set`, service));
--
-- define some views for output in the gui
DROP VIEW IF EXISTS aggregation_view;
DROP VIEW IF EXISTS service_sets_view;
CREATE VIEW aggregation_view AS SELECT apps.id AS appID, apps.app AS appName, services.id AS serviceID, services.name AS serviceName, aggregation_data.region AS regionID, aggregation_data.day, aggregation_data.year, aggregation_data.direction, aggregation_data.importance, aggregation_data.link, aggregation_data.protocol, aggregation_data.bytes FROM aggregation_data JOIN apps ON aggregation_data.app = apps.id JOIN services ON aggregation_data.service = services.id WHERE aggregation_data.handler = -1;
CREATE VIEW service_sets_view AS SELECT service_sets_data.app AS appID, apps.app AS appName, service_sets_data.id AS serviceSet, services.id AS serviceID, services.name AS serviceName, service_sets_data.day, service_sets_data.year, service_sets_data.direction, service_sets_data.importance, service_sets_data.link, service_sets_data.protocol, service_sets_data.bytes FROM service_sets_data JOIN apps ON service_sets_data.app = apps.id JOIN service_set_members ON service_sets_data.id = service_set_members.`set` JOIN services ON service_set_members.service = services.id;
