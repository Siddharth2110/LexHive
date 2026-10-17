-- Library members for the predefined login users in SecurityConfig.
-- A member user is linked to their row here by email.
INSERT INTO members (name, email) VALUES
    ('siddharth', 'siddharth@lexhive.com'),
    ('varshney', 'varshney@lexhive.com')
ON CONFLICT (email) DO NOTHING;
