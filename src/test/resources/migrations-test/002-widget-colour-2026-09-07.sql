ALTER TABLE widget ADD COLUMN colour varchar(20);
UPDATE widget SET colour = 'red' WHERE id = 1;
