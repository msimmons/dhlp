select * from foo

CREATE TABLE foo ( id INTEGER IDENTITY, str_col VARCHAR(256), num_col INTEGER)

insert into foo (str_col, num_col) values ('Goodbye', 3)

select * from information_schema.system_tables
