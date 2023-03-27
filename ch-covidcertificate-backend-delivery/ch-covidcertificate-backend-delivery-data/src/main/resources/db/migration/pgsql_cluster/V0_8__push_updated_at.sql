/*
 * Remove all existing push registrations since we don't know how old they are.
 * The apps will re-register themselves periodically.
 */

alter table t_push_registration add column updated_at timestamp with time zone NOT NULL DEFAULT now();