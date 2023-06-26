INSERT INTO acl_class (id, class) VALUES
(1, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.ProcessProxy'),
(2, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.RestrictionProxy');
 
INSERT INTO acl_sid (id, principal, sid) VALUES 
(1, 1, 'P1'),
(2, 1, 'P2'),
(3, 1, 'P3'),
(999, 1, 'dev'),
(1000, 0, 'ROLE_EDITOR');

 
INSERT INTO processproxy(id,name) VALUES 
(101, 'Task2::Task1::Task3'),
(102, 'Task3::Task2::Task1'),
(103, 'Task1::Task2::Task3'),
(201, 'UserStudy1Prep/882'),
(202, 'UserStudy1Prep/883'),
(203, 'UserStudy1Prep/884'),
(204, 'UserStudy1Prep/885'),
(205, 'UserStudy1Prep/886'),
(206, 'UserStudy1Prep/887'),
(207, 'UserStudy1Prep/868'),
(208, 'UserStudy1Prep/888'),
(209, 'UserStudy1Prep/889'),
(9999, '*');
 
INSERT INTO restrictionproxy(id,name) VALUES  
(1, 'Task1_REPAIR'),
(2, 'Task1_RESTRICTION'),
(3, 'Task2_REPAIR'),
(4, 'Task2_RESTRICTION'),
(5, 'Task3_REPAIR'),
(6, 'Task3_RESTRICTION'),
(9999, '*');
 
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES 
(1, 2, 1, NULL, 1000, 0), 
(2, 2, 2, NULL, 1000, 0),
(3, 2, 3, NULL, 1000, 0), 
(4, 2, 4, NULL, 1000, 0),
(5, 2, 5, NULL, 1000, 0), 
(6, 2, 6, NULL, 1000, 0),
(101, 1, 101, NULL, 1000, 0),
(102, 1, 102, NULL, 1000, 0),
(103, 1, 103, NULL, 1000, 0),
(201, 1, 201, NULL, 1000, 0),
(202, 1, 202, NULL, 1000, 0),
(203, 1, 203, NULL, 1000, 0),
(204, 1, 204, NULL, 1000, 0),
(205, 1, 205, NULL, 1000, 0),
(206, 1, 206, NULL, 1000, 0),
(207, 1, 207, NULL, 1000, 0),
(208, 1, 208, NULL, 1000, 0),
(209, 1, 209, NULL, 1000, 0),
(9998, 1, 9999, NULL, 1000, 0),
(9999, 2, 9999, NULL, 1000, 0);
 
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES 
(1, 201, 1, 1,     1, 1, 1, 1),
(2, 202, 1, 1,     1, 1, 1, 1),
(3, 203, 1, 1,     1, 1, 1, 1),
(4, 204, 1, 2,     1, 1, 1, 1),
(5, 205, 1, 2,     1, 1, 1, 1),
(6, 206, 1, 2,     1, 1, 1, 1),
(7, 207, 1, 3,     1, 1, 1, 1),
(8, 208, 1, 3,     1, 1, 1, 1),
(9, 209, 1, 3,     1, 1, 1, 1),
(10, 101, 10, 1,     1, 1, 1, 1),
(11, 102, 11, 2,     1, 1, 1, 1),
(12, 103, 12, 3,     1, 1, 1, 1),
(13, 3, 13, 1,     1, 1, 1, 1),
(14, 5, 14, 1,     1, 1, 1, 1),
(15, 6, 15, 1,     1, 1, 1, 1),
(16, 3, 16, 2,     1, 1, 1, 1),
(17, 4, 17, 2,     1, 1, 1, 1),
(18, 5, 18, 2,     1, 1, 1, 1),
(19, 1, 19, 3,     1, 1, 1, 1),
(20, 3, 20, 3,     1, 1, 1, 1),
(21, 4, 21, 3,     1, 1, 1, 1),
(9998, 9998, 1, 999,     1, 1, 1, 1),
(9999, 9999, 1, 999,     1, 1, 1, 1);
