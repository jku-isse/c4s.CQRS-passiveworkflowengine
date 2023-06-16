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

/*
The two types of permissions
*/
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
every process type is listed here twice, i.e., one for each task no Repair, with Repair, and with Repair and Restriction 
(without repair and with restriction makes no sense and is not modelled) 
*/
INSERT INTO restrictionproxy(id,name) VALUES 
(1,'SystemRequirementsAnalysis_REPAIR'),
(11,'SystemRequirementsAnalysis_RESTRICTION'),
(2,'Proc2_REPAIR'),
(12,'Proc2_RESTRICTION'),
(3,'Proc3_REPAIR'),
(13,'Proc4_RESTRICTION'),
(4,'Proc4_REPAIR'),
(14,'Proc4_RESTRICTION'),
(5,'Proc6_REPAIR'),
(15,'Proc5_RESTRICTION'),
(6,'Proc6_REPAIR'),
(16,'Proc6_RESTRICTION'),
(7,'Proc7_REPAIR'),
(17,'Proc7_RESTRICTION'),
(8,'Proc8_REPAIR'),
(18,'Proc8_RESTRICTION'),
(9,'Proc9_REPAIR'),
(19,'Proc9_RESTRICTION')
;

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
(9, 2, 9, NULL, 100, 0),

(11, 2, 11, NULL, 100, 0),
(12, 2, 12, NULL, 100, 0),
(13, 2, 13, NULL, 100, 0),
(14, 2, 14, NULL, 100, 0),
(15, 2, 15, NULL, 100, 0),
(16, 2, 16, NULL, 100, 0),
(17, 2, 17, NULL, 100, 0),
(18, 2, 18, NULL, 100, 0),
(19, 2, 19, NULL, 100, 0),

(101, 1, 1, NULL, 100, 0),
(102, 1, 2, NULL, 100, 0),
(103, 1, 3, NULL, 100, 0)
;


/* 
for every participant (column 4) we store the access to their input data (9x) via the table above, (column 2), i.e., ids above 100
for these, ace order (column 3) can be 1 as there is only one permission for a single user per input artifact
for every participant (column 4) we set for each process type when they are supposed to work with or without restrictions, i.e., ids 1-9,11-19 of the table above
ace order (column 3) needs to count up from 1 to N (participants) per process type (1-9)   
we only need READ writes, hence right part of matrix consists only of four 1s 
*/
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES
(1,   1, 1, 1,          1, 1, 1, 1),
(2,  11, 1, 1,          1, 1, 1, 1),
(3, 101, 1, 1,          1, 1, 1, 1),
/* (4,   1, 2, 2,          1, 1, 1, 1), we are not granting restriction access */ 
(5, 102, 1, 2,          1, 1, 1, 1)
;
