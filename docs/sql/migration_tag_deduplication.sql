-- Idempotent tag deduplication
-- Convert all tags to lowercase and remove duplicates for all posts

UPDATE public.posts
SET tags = (
  SELECT array_agg(DISTINCT lower(t))
  FROM unnest(tags) AS t
)
WHERE tags IS NOT NULL;
