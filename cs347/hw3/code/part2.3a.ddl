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
	PRIMARY KEY(id_People)
);  
   