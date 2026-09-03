-- Fix verify_profile_pin to return TABLE instead of JSON
-- RETURNS JSON causes PostgREST to double-encode the value, breaking decodeSingle<PinVerifyResult>()
-- RETURNS TABLE maps column names directly to JSON keys, works correctly with Supabase Kotlin SDK

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP FUNCTION IF EXISTS public.verify_profile_pin(INT, TEXT);

CREATE OR REPLACE FUNCTION public.verify_profile_pin(p_profile_id INT, p_pin TEXT)
RETURNS TABLE(unlocked BOOLEAN, retry_after_seconds INT, message TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
    v_pin_hash TEXT;
BEGIN
    SELECT * INTO v_profile FROM public.profiles
    WHERE user_id = auth.uid() AND profile_index = p_profile_id;
    IF NOT FOUND THEN
        RETURN QUERY SELECT false::BOOLEAN, 0::INT, 'Profile not found'::TEXT;
        RETURN;
    END IF;
    IF NOT v_profile.pin_enabled THEN
        RETURN QUERY SELECT true::BOOLEAN, 0::INT, NULL::TEXT;
        RETURN;
    END IF;
    IF v_profile.pin_locked_until IS NOT NULL AND v_profile.pin_locked_until > now() THEN
        RETURN QUERY SELECT false::BOOLEAN,
            EXTRACT(EPOCH FROM (v_profile.pin_locked_until - now()))::INT,
            'Profile is locked'::TEXT;
        RETURN;
    END IF;
    v_pin_hash := encode(digest(p_profile_id::TEXT || v_profile.pin_salt || p_pin, 'sha256'), 'hex');
    IF v_pin_hash = v_profile.pin_hash THEN
        RETURN QUERY SELECT true::BOOLEAN, 0::INT, NULL::TEXT;
    ELSE
        RETURN QUERY SELECT false::BOOLEAN, 0::INT, 'Incorrect PIN'::TEXT;
    END IF;
END;
$$;
