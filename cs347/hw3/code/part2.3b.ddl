--@(#) script.ddl
 
CREATE SEQUENCE ParentTable.Parents.People_SEQ;
    ---SKIPPED: Class People
  
CREATE TABLE ParentTable.Parents.People
(
	firstName,
	lastName,
	gender,
	age,
	id_People integer,
	fk_Peopleid_People integer NOT NULL,
	PRIMARY KEY(id_People),
	UNIQUE(fk_Peopleid_People),
	FOREIGN KEY(fk_Peopleid_People) REFERENCES ParentTable.Parents.People (id_People)
);  
   ---SKIPPED: Class REMOVED_BY_CUSTOM_TRANSFORMATION
   ---SKIPPED: Class SKIPED_MAPPED_ELEMENTS
   