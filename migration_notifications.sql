-- Run this script in the Supabase SQL Editor
-- Partial unique index for notifications to avoid spamming
CREATE UNIQUE INDEX IF NOT EXISTS uniq_notif_event 
ON public.notifications (recipient_id, actor_id, type, post_id, comment_id) 
WHERE read = false;
