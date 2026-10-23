-- ========================================================================
-- Idempotent Migration: Dedicated YouTube Architecture & Video Schema
-- ========================================================================

-- 1. Introspect existing constraints and types (informational)
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'posts_type_check';

-- 2. Safely verify and extend posts_type_check constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'posts_type_check'
    ) THEN
        ALTER TABLE public.posts DROP CONSTRAINT posts_type_check;
    END IF;

    ALTER TABLE public.posts
        ADD CONSTRAINT posts_type_check
        CHECK (type IN (
            'Post', 'AI Prompt', 'Code', 'Question', 'Tip', 'Trick',
            'Idea', 'Tutorial', 'Project', 'Update', 'Resource',
            'Learning', 'Discovery', 'General', 'Video', 'Photo', 'Text', 'Pixel Art', 'PixelArt'
        ));
END $$;

-- 3. Idempotently create public.video_posts table
CREATE TABLE IF NOT EXISTS public.video_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    title TEXT,
    thumbnail_url TEXT,
    video_url TEXT NOT NULL,
    embed_url TEXT,
    platform TEXT DEFAULT 'direct',
    category TEXT,
    tags TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Idempotently add all extended columns (including external_id for YouTube ID)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'external_id') THEN
        ALTER TABLE public.video_posts ADD COLUMN external_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'is_vertical') THEN
        ALTER TABLE public.video_posts ADD COLUMN is_vertical BOOLEAN DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'direct_url') THEN
        ALTER TABLE public.video_posts ADD COLUMN direct_url TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'duration_seconds') THEN
        ALTER TABLE public.video_posts ADD COLUMN duration_seconds INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'width') THEN
        ALTER TABLE public.video_posts ADD COLUMN width INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'height') THEN
        ALTER TABLE public.video_posts ADD COLUMN height INTEGER;
    END IF;
END $$;

-- 5. High-performance Indexes for video lookup, platform queries & external_id
CREATE INDEX IF NOT EXISTS idx_video_posts_post_id ON public.video_posts(post_id);
CREATE INDEX IF NOT EXISTS idx_video_posts_platform ON public.video_posts(platform);
CREATE INDEX IF NOT EXISTS idx_video_posts_external_id ON public.video_posts(external_id);
CREATE INDEX IF NOT EXISTS idx_video_posts_created_at ON public.video_posts(created_at DESC);

-- 6. Row Level Security (RLS) Configuration with safe UUID/Text casting
ALTER TABLE public.video_posts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "video_posts_select_all" ON public.video_posts;
CREATE POLICY "video_posts_select_all" ON public.video_posts
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "video_posts_insert_auth" ON public.video_posts;
CREATE POLICY "video_posts_insert_auth" ON public.video_posts
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' OR auth.role() = 'anon'
    );

DROP POLICY IF EXISTS "video_posts_update_owner" ON public.video_posts;
CREATE POLICY "video_posts_update_owner" ON public.video_posts
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM public.posts
            WHERE posts.id = video_posts.post_id
            AND (posts.user_id::text = auth.uid()::text OR auth.role() = 'anon')
        )
    );

DROP POLICY IF EXISTS "video_posts_delete_owner" ON public.video_posts;
CREATE POLICY "video_posts_delete_owner" ON public.video_posts
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.posts
            WHERE posts.id = video_posts.post_id
            AND (posts.user_id::text = auth.uid()::text OR auth.role() = 'anon')
        )
    );
