INSERT INTO acl_class (id, class) VALUES
(1, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.ProcessProxy'),
(2, 'at.jku.isse.passiveprocessengine.frontend.security.persistence.RestrictionProxy');
 
INSERT INTO acl_sid (id, principal, sid) VALUES 
(1, 1, 'P1'),
(990, 1, 'repaironly'),
(999, 1, 'dev'),
(1000, 0, 'ROLE_EDITOR');
 
INSERT INTO processproxy(id,name) VALUES 
(9999, '*');
 
INSERT INTO restrictionproxy(id,name) VALUES  
(9997, '+'),
(9998, '*');
 
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES 
(9997, 2, 9997, NULL, 1000, 0),
(9998, 2, 9998, NULL, 1000, 0),
(9999, 1, 9999, NULL, 1000, 0);
 
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid,           mask, granting, audit_success, audit_failure) VALUES 
(9996, 9999, 1, 990,     1, 1, 1, 1),
(9997, 9997, 1, 990,     1, 1, 1, 1),
(9998, 9998, 1, 999,     1, 1, 1, 1),
(9999, 9999, 2, 999,     1, 1, 1, 1);
