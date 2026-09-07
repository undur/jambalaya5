-- First migration: the widget table
CREATE TABLE widget (
	id integer PRIMARY KEY,
	name varchar(100) NOT NULL
);
INSERT INTO widget (id, name) VALUES (1, 'first');
