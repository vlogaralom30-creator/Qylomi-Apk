-- ========================================================================
-- Idempotent Migration: Video Posts, Constraints & Reels Architecture
-- ========================================================================

-- 1. Safely update posts_type_check to allow 'Video', 'Photo', 'Text', 'PixelArt'
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

-- 2. Idempotent CREATE TABLE for video_posts
CREATE TABLE IF NOT EXISTS public.video_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    title TEXT,
    thumbnail_url TEXT,
    video_url TEXT NOT NULL,
    embed_url TEXT,
    direct_url TEXT,
    platform TEXT,
    duration_seconds INTEGER,
    width INTEGER,
    height INTEGER,
    is_vertical BOOLEAN DEFAULT FALSE,
    category TEXT,
    tags TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Idempotently add missing columns if video_posts was already created earlier
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'direct_url') THEN
        ALTER TABLE public.video_posts ADD COLUMN direct_url TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'embed_url') THEN
        ALTER TABLE public.video_posts ADD COLUMN embed_url TEXT;
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
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'video_posts' AND column_name = 'is_vertical') THEN
        ALTER TABLE public.video_posts ADD COLUMN is_vertical BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 4. High-performance Indexes for Feed & Reels
CREATE INDEX IF NOT EXISTS idx_video_posts_post_id ON public.video_posts(post_id);
CREATE INDEX IF NOT EXISTS idx_video_posts_created_at ON public.video_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_video_posts_is_vertical ON public.video_posts(is_vertical);

-- 5. Row Level Security (RLS) Configuration
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
