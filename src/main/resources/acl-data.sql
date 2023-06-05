/* 
The participants 
*/
INSERT INTO acl_sid (id, principal, sid) VALUES
(1, 1, 'P1'),
(2, 1, 'P2'),
(3, 1, 'P3'),
(4, 1, 'P4'),
(100, 0, 'ROLE_EDITOR')
;

INSERT INTO acl_class (id, class) VALUES
(1, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.ProcessProxy'),
(2, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.RestrictionProxy');

/* 
every process instance, respectively the input artifact is listed here, one for each task X participant 
*/
INSERT INTO processproxy(id,name) VALUES 
(1,'UserStudy1Prep/868'),
(2,'UserStudy2Prep/881'),
(3,'P1Inst3');

/* 
every process type is listed here, i.e., one for each task 
*/
INSERT INTO restrictionproxy(id,name) VALUES 
(1,'SystemRequirementsAnalysis'),
(2,'Proc2'),
(3,'Proc3'),
(4,'Proc4'),
(5,'Proc5'),
(6,'Proc6'),
(7,'Proc7'),
(8,'Proc8');

/* 
each process type (upper rows) and process instance resp input (lower rows) belongs to the role editor role, nothing else needed 
*/
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES
(1, 2, 1, NULL, 100, 0),
(2, 2, 2, NULL, 100, 0),
(3, 2, 3, NULL, 100, 0),
(4, 2, 4, NULL, 100, 0),
(5, 2, 5, NULL, 100, 0),
(6, 2, 6, NULL, 100, 0),
(7, 2, 7, NULL, 100, 0),
(8, 2, 8, NULL, 100, 0),

(101, 1, 1, NULL, 100, 0),
(102, 1, 2, NULL, 100, 0),
(103, 1, 3, NULL, 100, 0)
;


/* 
for every participant (column 4) we store the access to their input data (8x) via the table above, (column 2), i.e., ids above 100
for these, ace order (column 3) can be 1 as there is only one permission for a single user per input artifact
for every participant (column 4) we set for each process type when they are supposed to work with or without restrictions, i.e., ids 1-8 of the table above
ace order (column 3) needs to count up from 1 to N (participants) per process type (1-8)   
we only need READ writes, hence right part of matrix consists only of four 1s 
*/
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES
(1,   1, 1, 1,          1, 1, 1, 1),
(2, 101, 1, 1,          1, 1, 1, 1),
/* (3,   1, 2, 2,          1, 1, 1, 1), we are not granting restriction access */ 
(4, 102, 1, 2,          1, 1, 1, 1)
;
