-- Local seed data for Keycloak 26.8.1 / PostgreSQL.
-- Run after Keycloak creates its tables and imports the lms realm.
-- Stop Keycloak before execution; start it again after COMMIT.
BEGIN;

DO $seed$
DECLARE
    target_realm_id varchar(36);
    target_role_id varchar(36);
    target_user_id varchar(36);
    created_user_id varchar(36);
    seed record;
    created_at bigint := (extract(epoch FROM current_timestamp) * 1000)::bigint;
BEGIN
    SELECT id INTO target_realm_id FROM public.realm WHERE name = 'lms';
    IF target_realm_id IS NULL THEN
        RAISE EXCEPTION 'Realm lms is missing. Start Keycloak with the realm import first.';
    END IF;

    FOR seed IN
        SELECT * FROM (VALUES
            ('student', 'ROLE_USER', '{"value":"sUpnljqXkgCld1+KyQoNTPRLgauNnhKM0zkpL9rGn44=","salt":"cpbGpoDJjFM5hnTdcrdnBQ==","additionalParameters":{}}'),
            ('teacher', 'ROLE_TEACHER', '{"value":"uED6dhiOt2bNQs4WNy0cDlAmnrdBxIhGNWHoW8gtuAk=","salt":"X+dZKN0vqfA7kmKKGjyXOA==","additionalParameters":{}}'),
            ('lms-admin', 'ROLE_ADMIN', '{"value":"qQawhT89Axal7jENkHZRLvlKvWrY5WcX5KtCPjOAfJ8=","salt":"HmWyNKnvIBAvRUc/tOBsFQ==","additionalParameters":{}}')
        ) AS initial_users(username, role_name, password_secret)
    LOOP
        INSERT INTO public.keycloak_role
            (id, name, client_role, realm_id, realm, client_realm_constraint)
        VALUES
            (gen_random_uuid()::text, seed.role_name, false,
             target_realm_id, target_realm_id, target_realm_id)
        ON CONFLICT (name, client_realm_constraint) DO NOTHING;

        SELECT id INTO STRICT target_role_id
        FROM public.keycloak_role
        WHERE name = seed.role_name AND realm_id = target_realm_id AND client_role = false;

        INSERT INTO public.user_entity
            (id, username, email, email_constraint, email_verified, enabled,
             first_name, last_name, realm_id, created_timestamp, not_before)
        VALUES
            (gen_random_uuid()::text, seed.username, seed.username || '@example.com',
             seed.username || '@example.com', true, true,
             seed.username, 'Demo', target_realm_id, created_at, 0)
        ON CONFLICT (realm_id, username) DO NOTHING
        RETURNING id INTO created_user_id;

        SELECT id INTO STRICT target_user_id
        FROM public.user_entity
        WHERE username = seed.username AND realm_id = target_realm_id;

        -- Existing accounts keep their credentials.
        IF created_user_id IS NOT NULL THEN
            INSERT INTO public.credential
                (id, type, user_id, created_date, secret_data, credential_data, priority)
            VALUES
                (gen_random_uuid()::text, 'password', target_user_id, created_at,
                 seed.password_secret,
                 '{"hashIterations":600000,"algorithm":"pbkdf2-sha256","additionalParameters":{}}', 0);
        END IF;

        INSERT INTO public.user_role_mapping (role_id, user_id)
        VALUES (target_role_id, target_user_id)
        ON CONFLICT (role_id, user_id) DO NOTHING;
    END LOOP;
END $seed$;

COMMIT;
