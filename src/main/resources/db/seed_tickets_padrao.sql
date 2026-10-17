-- ============================================================
-- Seed: tickets padrão para lançamento de horas
-- Ajuste o e-mail abaixo para o usuário desejado.
-- Limpa registros anteriores do usuário e reinseré os corretos.
-- ============================================================

DO $$
DECLARE
    v_user_id BIGINT;
BEGIN
    SELECT id INTO v_user_id
    FROM users
    WHERE email = 'tulio.amaral@zgsolucoes.com.br';

    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Usuário não encontrado. Verifique o e-mail no script.';
    END IF;

    -- Remove vínculos de eventos antes de excluir os tickets
    UPDATE calendar_events SET ticket_padrao_id = NULL
    WHERE ticket_padrao_id IN (
        SELECT id FROM tickets_padrao_horas WHERE user_id = v_user_id
    );

    -- Remove registros anteriores do usuário para reinserir corrigidos
    DELETE FROM tickets_padrao_horas WHERE user_id = v_user_id;

    INSERT INTO tickets_padrao_horas (user_id, nome, ticket_key, ativo)
    VALUES
        (v_user_id, 'Eventos Agile',          'SET-737',  true),
        (v_user_id, 'Reuniões ZG',             'SET-820',  true),
        (v_user_id, 'Outros',                  'SET-733',  true),
        (v_user_id, 'Lançamento de horas',     'SET-728',  true),
        (v_user_id, 'Reunião de 1:1',          'SET-735',  true),
        (v_user_id, 'Reunião de Refinamento',  'SET-529',  true),
        (v_user_id, 'Estudos',                 'SET-756',  true),
        (v_user_id, 'Apoio dentro da squad',   'SET-764',  true),
        (v_user_id, 'Hacker Rangers',          'SET-834',  true),
        (v_user_id, 'Treinamentos ZG',         'SET-839',  true),
        (v_user_id, 'Reunião Técnica',         'SET-1115', true),
        (v_user_id, 'Apoio fora da squad',     'SET-535',  true),
        (v_user_id, 'Assuntos pessoais',       'SET-727',  true);

    RAISE NOTICE 'Seed concluído para user_id = % (13 tickets)', v_user_id;
END $$;
