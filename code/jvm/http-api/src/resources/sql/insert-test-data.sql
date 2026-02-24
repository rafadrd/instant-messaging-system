INSERT INTO dbo.users (username, password_validation)
VALUES ('antonio', '$2a$10$/eBal.V3hZTALYPpu4iTR.IDi81E2gIkhy1Lz2RlscwerU5IfrMBW'),
       ('diogo', '$2a$10$/eBal.V3hZTALYPpu4iTR.IDi81E2gIkhy1Lz2RlscwerU5IfrMBW'),
       ('rafael', '$2a$10$/eBal.V3hZTALYPpu4iTR.IDi81E2gIkhy1Lz2RlscwerU5IfrMBW'),
       ('miguel', '$2a$10$/eBal.V3hZTALYPpu4iTR.IDi81E2gIkhy1Lz2RlscwerU5IfrMBW');

INSERT INTO dbo.channels (name, owner_id, is_public)
VALUES ('public1', 1, TRUE),
       ('public2', 2, TRUE),
       ('private1', 1, FALSE),
       ('private2', 2, FALSE);

INSERT INTO dbo.invitations (token, created_by, channel_id, access_type, expires_at, status)
VALUES ('token1', 1, 3, 'READ_ONLY', NOW() + INTERVAL '1 day', 'PENDING'),    -- antonio invites diogo to private1
       ('token2', 2, 4, 'READ_WRITE', NOW() + INTERVAL '1 day', 'ACCEPTED'),  -- diogo invites rafael to private2
       ('token3', 2, 4, 'READ_ONLY', NOW() + INTERVAL '2 days', 'ACCEPTED'),  -- diogo invites antonio to private2
       ('token4', 1, 3, 'READ_WRITE', NOW() + INTERVAL '2 days', 'REJECTED'); -- antonio invites miguel to private1

INSERT INTO dbo.channel_members (user_id, channel_id, access_type)
VALUES (1, 1, 'READ_WRITE'), -- antonio in public1
       (2, 1, 'READ_WRITE'), -- diogo in public1
       (2, 2, 'READ_WRITE'), -- diogo in public2
       (1, 2, 'READ_WRITE'), -- antonio in public2
       (2, 4, 'READ_WRITE'), -- diogo in private2 (owner)
       (1, 3, 'READ_WRITE'), -- antonio in private1 (owner)
       (3, 4, 'READ_ONLY');  -- rafael in private2 (read-only)

INSERT INTO dbo.messages (content, user_id, channel_id)
VALUES ('ola!', 1, 1),             -- antonio in public1
       ('muito importante', 2, 1), -- diogo in public1
       ('como é que vamos', 2, 2), -- diogo in public2
       ('como é que tamos', 1, 3), -- antonio in private1
       ('zeze', 2, 4);             -- diogo in private2
