--@(#) script.ddl
 
CREATE TABLE StudentClassTable.StudentClass.Class
(
	name,
	id_Class integer,
	PRIMARY KEY(id_Class)
);  
  
CREATE SEQUENCE StudentClassTable.StudentClass.GradeLine_SEQ;

CREATE SEQUENCE StudentClassTable.StudentClass.Student_SEQ;

CREATE SEQUENCE StudentClassTable.StudentClass.Class_SEQ;
   ---SKIPPED: Class REMOVED_BY_CUSTOM_TRANSFORMATION
  
CREATE TABLE StudentClassTable.StudentClass.Student
(
	firstName,
	lastName,
	id_Student integer,
	PRIMARY KEY(id_Student)
);  
    
CREATE TABLE StudentClassTable.StudentClass.GradeLine
(
	grade,
	id_GradeLine integer,
	fk_Studentid_Student integer NOT NULL,
	fk_Classid_Class integer NOT NULL,
	PRIMARY KEY(id_GradeLine),
	FOREIGN KEY(fk_Studentid_Student) REFERENCES StudentClassTable.StudentClass.Student (id_Student),
	FOREIGN KEY(fk_Classid_Class) REFERENCES StudentClassTable.StudentClass.Class (id_Class)
);  
  