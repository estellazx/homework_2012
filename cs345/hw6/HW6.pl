%Name: Shun Zhang
%EID : sz4554

% 1. student(STUDENT_ID, _, 'Fred', LAST_NAME, _, _, _, _, _), enrollment(STUDENT_ID, SECTION_ID, _, _).
% 2. student(STUDENT_ID, _, 'Fred', LAST_NAME, _, _, _, _, _), enrollment(STUDENT_ID, SECTION_ID, _, _), section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _).
% 3. student(STUDENT_ID, _, 'Fred', LAST_NAME, _, _, _, _, _), enrollment(STUDENT_ID, SECTION_ID, _, _), section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _), course(COURSE_NO, DESCRIPTION, _, _).
% 4. student(STUDENT_ID, _, 'Fred', LAST_NAME, _, _, _, _, _), enrollment(STUDENT_ID, SECTION_ID, _, _), section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _), course(COURSE_NO, DESCRIPTION, _, _), instructor(INSTRUCTOR_ID, _, I_FIRST_NAME, I_LAST_NAME, _, _, _).
% 5. section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _), course(COURSE_NO, DESCRIPTION, _, _), instructor(INSTRUCTOR_ID, _, 'Nina', 'Schorin', _, _, _).
% 6. section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _), course(COURSE_NO, 'Hands-On Windows', _, _), instructor(INSTRUCTOR_ID, _, FIRST_NAME, LAST_NAME, _, _, _).
% 7. student(_STUDENT_ID, _, P_FIRSTNAME, P_LASTNAME, _, _, _, _, _), section(SECTION_ID, COURSE_NO, _, _, _, INSTRUCTOR_ID, _), course(COURSE_NO, 'Hands-On Windows', _, _), instructor(INSTRUCTOR_ID, _, 'Anita', 'Morris', _, _, _), enrollment(_STUDENT_ID, SECTION_ID, _, _).
% 8. course(_, DESCRIPTION, _, PREREQUISITE), course(PREREQUISITE, _, _, _).
% 9. course(_, DESCRIPTION, COST, _), COST < 1100.
% 10.
prereq(P, Q) :- course(Q, _, _, P).
prereq(P, Q) :- course(X, _, _, P), prereq(X, Q).

% prereq(X,430).
