--@(#) script.ddl
 
CREATE TABLE SantaTable.Santa.Child
(
	name,
	address,
	born date,
	id_Child integer,
	PRIMARY KEY(id_Child)
);  
  
CREATE SEQUENCE SantaTable.Santa.Delivered_SEQ;

CREATE SEQUENCE SantaTable.Santa.YearCase_SEQ;

CREATE SEQUENCE SantaTable.Santa.Child_SEQ;

CREATE SEQUENCE SantaTable.Santa.Performance_SEQ;

CREATE SEQUENCE SantaTable.Santa.Wish_SEQ;
   ---SKIPPED: Class REMOVED_BY_CUSTOM_TRANSFORMATION
    
CREATE TABLE SantaTable.Santa.YearCase
(
	year,
	id_YearCase integer,
	fk_Childid_Child integer NOT NULL,
	PRIMARY KEY(id_YearCase),
	FOREIGN KEY(fk_Childid_Child) REFERENCES SantaTable.Santa.Child (id_Child)
);  
  
CREATE TABLE SantaTable.Santa.Delivered
(
	item,
	id_Delivered integer,
	fk_YearCaseid_YearCase integer NOT NULL,
	PRIMARY KEY(id_Delivered),
	UNIQUE(fk_YearCaseid_YearCase),
	FOREIGN KEY(fk_YearCaseid_YearCase) REFERENCES SantaTable.Santa.YearCase (id_YearCase)
);  
  
CREATE TABLE SantaTable.Santa.Performance
(
	niceOrNaughty,
	id_Performance integer,
	fk_YearCaseid_YearCase integer NOT NULL,
	PRIMARY KEY(id_Performance),
	UNIQUE(fk_YearCaseid_YearCase),
	FOREIGN KEY(fk_YearCaseid_YearCase) REFERENCES SantaTable.Santa.YearCase (id_YearCase)
);  
  
CREATE TABLE SantaTable.Santa.Wish
(
	item,
	id_Wish integer,
	fk_YearCaseid_YearCase integer NOT NULL,
	PRIMARY KEY(id_Wish),
	FOREIGN KEY(fk_YearCaseid_YearCase) REFERENCES SantaTable.Santa.YearCase (id_YearCase)
);  
  