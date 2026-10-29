-- =============================================================================
-- V38: Fix PERM_CHAT_SHARE permission name
-- =============================================================================

UPDATE public.permission
    SET name = 'CHAT_SHARE'
    WHERE name = 'PERM_CHAT_SHARE';
