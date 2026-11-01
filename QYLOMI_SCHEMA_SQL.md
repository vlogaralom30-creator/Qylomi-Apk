# Qylomi — Full Supabase Schema (SQL)

> **Project:** Qylomi (`ffbmozwsasbhevtehbit`) · **Region:** ap-southeast-1 · **Postgres:** 17
> **Updated:** 2026-11-01 (Full Schema with Idempotent Migrations & Security Audit Fixes)
> **Contents:** extensions → schemas → 39 tables → indexes → functions → triggers → RLS → storage

---

## 0. Extensions & Schemas

```sql
create extension if not exists pg_cron            with schema pg_catalog;
create extension if not exists pg_net             with schema extensions;
create extension if not exists pg_stat_statements with schema extensions;
create extension if not exists pg_trgm            with schema public;
create extension if not exists pgcrypto           with schema extensions;
create extension if not exists "uuid-ossp"        with schema extensions;

create schema if not exists private;

set check_function_bodies = off;
```

---

## 1. Tables

```sql
-- PROFILES ------------------------------------------------------------
create table if not exists public.profiles (
  id             uuid primary key references auth.users(id) on delete cascade,
  username       text not null unique check (char_length(username) between 3 and 30),
  display_name   text not null,
  bio            text,
  website        text,
  avatar_url     text,
  cover_url      text,
  headline       text,
  age            integer,
  gender         text,
  location       text,
  work           text,
  education      text,
  skills         text[],
  social_links   jsonb default '{}'::jsonb,
  points         integer not null default 0,
  creator_rank   text default 'Novice Explorer',
  creator_level  integer default 1,
  badges         jsonb default '[]'::jsonb,
  points_history jsonb default '[]'::jsonb,
  trackers_count integer not null default 0,
  role           text not null default 'user' check (role in ('user','creator','admin')),
  created_at     timestamptz not null default timezone('utc', now()),
  updated_at     timestamptz not null default timezone('utc', now())
);

-- FOLLOWS -------------------------------------------------------------
create table if not exists public.follows (
  id           uuid primary key default gen_random_uuid(),
  follower_id  uuid not null references public.profiles(id) on delete cascade,
  following_id uuid not null references public.profiles(id) on delete cascade,
  created_at   timestamptz not null default timezone('utc', now()),
  constraint unique_follower_following unique (follower_id, following_id)
);

-- GROUPS --------------------------------------------------------------
create table if not exists public.groups (
  id            uuid primary key default gen_random_uuid(),
  name          text not null,
  slug          text not null unique,
  description   text,
  avatar_url    text,
  cover_url     text,
  privacy       text not null default 'public' check (privacy in ('public','restricted','private')),
  category      text not null default 'general',
  rules         jsonb default '[]'::jsonb,
  creator_id    uuid not null references public.profiles(id) on delete cascade,
  members_count integer default 1,
  posts_count   integer default 0,
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);

create table if not exists public.group_members (
  id        uuid primary key default gen_random_uuid(),
  group_id  uuid not null references public.groups(id)   on delete cascade,
  user_id   uuid not null references public.profiles(id) on delete cascade,
  role      text not null default 'member' check (role in ('admin','moderator','member')),
  status    text not null default 'active' check (status in ('active','pending','banned')),
  joined_at timestamptz default now(),
  constraint group_members_group_id_user_id_key unique (group_id, user_id)
);

-- POSTS & SOCIAL ------------------------------------------------------
create table if not exists public.posts (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references public.profiles(id) on delete cascade,
  group_id            uuid references public.groups(id) on delete set null,
  type                text not null check (type in (
                        'Post','AI Prompt','Code','Question','Tip','Trick','Idea',
                        'Tutorial','Project','Update','Resource','Learning','Discovery',
                        'General','Video','Photo','Text','PixelArt')),
  title               text not null check (char_length(title) between 3 and 250),
  content             text not null,
  external_url        text,
  appreciations_count integer not null default 0,
  replies_count       integer not null default 0,
  relays_count        integer not null default 0,
  video_preview_urls  text[] default '{}'::text[],
  attachments         jsonb  default '[]'::jsonb,
  pixel_art           jsonb,
  allow_comments      boolean not null default true,
  is_pinned           boolean not null default false,
  pinned_at           timestamptz,
  search_vector       tsvector generated always as (
                        to_tsvector('simple',
                          coalesce(title,'') || ' ' || left(coalesce(content,''), 10000))
                      ) stored,
  created_at          timestamptz not null default timezone('utc', now()),
  updated_at          timestamptz not null default timezone('utc', now())
);

create table if not exists public.tags (
  id          uuid primary key default gen_random_uuid(),
  name        text not null unique check (name ~* '^[a-z0-9_-]+$'),
  posts_count integer not null default 0,
  created_at  timestamptz not null default timezone('utc', now())
);

create table if not exists public.post_tags (
  post_id uuid not null references public.posts(id) on delete cascade,
  tag_id  uuid not null references public.tags(id)  on delete cascade,
  primary key (post_id, tag_id)
);

create table if not exists public.post_reactions (
  id            uuid primary key default gen_random_uuid(),
  post_id       uuid not null references public.posts(id)    on delete cascade,
  user_id       uuid not null references public.profiles(id) on delete cascade,
  reaction_type text not null default 'appreciate',
  created_at    timestamptz not null default timezone('utc', now()),
  constraint unique_post_user_reaction unique (post_id, user_id, reaction_type)
);

create table if not exists public.comments (
  id          uuid primary key default gen_random_uuid(),
  post_id     uuid not null references public.posts(id)    on delete cascade,
  user_id     uuid not null references public.profiles(id) on delete cascade,
  parent_id   uuid references public.comments(id) on delete cascade,
  content     text not null check (char_length(content) between 1 and 3000),
  likes_count integer not null default 0,
  created_at  timestamptz not null default timezone('utc', now()),
  updated_at  timestamptz not null default timezone('utc', now())
);

create table if not exists public.comment_likes (
  id         uuid primary key default gen_random_uuid(),
  comment_id uuid not null references public.comments(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz default now(),
  unique (comment_id, user_id)
);

create table if not exists public.post_relays (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id)    on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  quote      text,
  created_at timestamptz not null default timezone('utc', now()),
  constraint unique_post_relay unique (post_id, user_id)
);

create table if not exists public.saved_posts (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id)    on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  constraint unique_post_user_saved unique (post_id, user_id)
);

create table if not exists public.hidden_posts (
  user_id    uuid not null references public.profiles(id) on delete cascade,
  post_id    uuid not null references public.posts(id)    on delete cascade,
  created_at timestamptz default now(),
  primary key (user_id, post_id)
);

create table if not exists public.notifications (
  id           uuid primary key default gen_random_uuid(),
  recipient_id uuid not null references public.profiles(id) on delete cascade,
  actor_id     uuid not null references public.profiles(id) on delete cascade,
  type         text not null check (type in ('appreciate','reply','relay','track')),
  post_id      uuid references public.posts(id)    on delete cascade,
  comment_id   uuid references public.comments(id) on delete cascade,
  read         boolean not null default false,
  created_at   timestamptz not null default timezone('utc', now())
);

create table if not exists public.reports (
  id          uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  post_id     uuid not null references public.posts(id)    on delete cascade,
  comment_id  uuid references public.comments(id) on delete cascade,
  reason      text not null,
  details     text,
  status      text not null default 'pending' check (status in ('pending','reviewed','dismissed')),
  created_at  timestamptz not null default timezone('utc', now())
);

create table if not exists public.user_events (
  id         bigint generated always as identity primary key,
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  post_id    uuid not null references public.posts(id) on delete cascade,
  event_type text not null check (event_type in
               ('impression','open','like','comment','relay','bookmark','share','hide')),
  created_at timestamptz not null default now()
);

create table if not exists public.playlists (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  title       text not null check (char_length(title) between 1 and 200),
  description text default '',
  cover_url   text,
  is_public   boolean not null default true,
  items       jsonb not null default '[]'::jsonb,
  created_at  timestamptz not null default timezone('utc', now()),
  updated_at  timestamptz not null default timezone('utc', now())
);

-- TYPED POST EXTENSIONS -----------------------------------------------
create table if not exists public.video_posts (
  id               uuid primary key default gen_random_uuid(),
  post_id          uuid not null references public.posts(id) on delete cascade,
  title            text,
  thumbnail_url    text,
  video_url        text not null,
  embed_url        text,
  direct_url       text,
  platform         text default 'direct',
  external_id      text,
  category         text,
  tags             text[] default '{}'::text[],
  duration_seconds integer,
  width            integer,
  height           integer,
  is_vertical      boolean default false,
  created_at       timestamptz default now()
);

create table if not exists public.photo_posts (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  caption    text,
  photo_urls text[] not null,
  category   text,
  tags       text[] default '{}'::text[],
  created_at timestamptz default now()
);

create table if not exists public.text_posts (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  caption    text not null,
  bg_color   text default 'preset_default',
  font_style text default 'default',
  category   text,
  tags       text[] default '{}'::text[],
  created_at timestamptz default now()
);

-- MONETIZATION & WALLET -----------------------------------------------
create table if not exists public.wallet_transactions (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references public.profiles(id) on delete cascade,
  action          text not null,
  points          integer not null,
  balance_after   integer not null default 0,
  description     text not null,
  reference_id    text,
  reference_type  text,
  idempotency_key text,
  created_at      timestamptz not null default timezone('utc', now())
);

create table if not exists public.monetization_stats (
  user_id               uuid primary key references public.profiles(id) on delete cascade,
  status                text not null default 'not_eligible' check (status in ('not_eligible','eligible','pending_review','approved','suspended')),
  total_views           bigint not null default 0,
  total_watch_seconds   bigint not null default 0,
  total_monetized_views bigint not null default 0,
  total_earnings_usd    numeric not null default 0.00,
  payout_method         text,
  payout_details        text,
  applied_at            timestamptz,
  approved_at           timestamptz,
  created_at            timestamptz not null default timezone('utc', now()),
  updated_at            timestamptz not null default timezone('utc', now())
);

create table if not exists public.payout_requests (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references public.profiles(id) on delete cascade,
  amount_usd numeric not null check (amount_usd >= 10.00),
  method     text not null,
  status     text not null default 'pending' check (status in ('pending','processed','rejected')),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- WATCH / SERIES ------------------------------------------------------
create table if not exists public.content_categories (
  id         uuid primary key default gen_random_uuid(),
  slug       text not null unique check (slug ~* '^[a-z0-9-]+$'),
  name       text not null,
  sort_order integer not null default 0,
  is_active  boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.series (
  id                 uuid primary key default gen_random_uuid(),
  slug               text not null unique check (slug ~* '^[a-z0-9-]+$'),
  title              text not null check (char_length(title) between 1 and 200),
  original_title     text,
  description        text,
  poster_url         text,
  banner_url         text,
  trailer_url        text,
  category_id        uuid references public.content_categories(id) on delete set null,
  genres             text[] not null default '{}'::text[],
  languages          text[] not null default '{}'::text[],
  subtitle_languages text[] not null default '{}'::text[],
  rating             numeric check (rating between 0 and 10),
  age_rating         text,
  studio             text,
  is_mature          boolean not null default false,
  status             text not null default 'ongoing' check (status in ('ongoing','completed','upcoming')),
  release_year       integer check (release_year between 1900 and 2100),
  total_episodes     integer check (total_episodes >= 0),
  is_published       boolean not null default false,
  is_featured        boolean not null default false,
  is_blocked         boolean not null default false,
  blocked_reason     text,
  created_by         uuid references public.profiles(id) on delete set null,
  avg_user_rating    numeric not null default 0,
  ratings_count      integer not null default 0,
  likes_count        integer not null default 0,
  comments_count     integer not null default 0,
  episodes_count     integer not null default 0,
  views_count        bigint not null default 0,
  last_episode_at    timestamptz,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);

create table if not exists public.series_seasons (
  id            uuid primary key default gen_random_uuid(),
  series_id     uuid not null references public.series(id) on delete cascade,
  season_number integer not null check (season_number >= 0),
  title         text,
  description   text,
  poster_url    text,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  constraint unique_series_season unique (series_id, season_number)
);

create table if not exists public.episodes (
  id               uuid primary key default gen_random_uuid(),
  series_id        uuid not null references public.series(id) on delete cascade,
  season_number    integer not null default 1 check (season_number >= 0),
  episode_number   integer not null check (episode_number >= 0),
  title            text,
  description      text,
  thumbnail_url    text,
  duration_seconds integer check (duration_seconds >= 0),
  air_date         date,
  intro_start_sec  integer check (intro_start_sec >= 0),
  intro_end_sec    integer check (intro_end_sec >= 0),
  is_published     boolean not null default true,
  likes_count      integer not null default 0,
  comments_count   integer not null default 0,
  views_count      bigint not null default 0,
  created_by       uuid references public.profiles(id) on delete set null,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  constraint unique_series_season_episode unique (series_id, season_number, episode_number)
);

create table if not exists public.episode_sources (
  id          uuid primary key default gen_random_uuid(),
  episode_id  uuid not null references public.episodes(id) on delete cascade,
  label       text not null default 'Server 1',
  url         text not null check (url ~* '^https?://'),
  platform    text,
  language    text,
  quality     text,
  priority    integer not null default 0,
  is_active   boolean not null default true,
  fail_count  integer not null default 0,
  last_ok_at  timestamptz,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table if not exists public.source_reports (
  id         uuid primary key default gen_random_uuid(),
  source_id  uuid not null references public.episode_sources(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  reason     text not null check (reason in ('broken','wrong_episode','copyright','other')),
  details    text check (char_length(details) <= 1000),
  status     text not null default 'open' check (status in ('open','handled')),
  handled_by uuid references public.profiles(id) on delete set null,
  handled_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.series_shelves (
  id          uuid primary key default gen_random_uuid(),
  slug        text not null unique check (slug ~* '^[a-z0-9-]+$'),
  title       text not null,
  type        text not null check (type in ('manual','genre','category','auto_trending','latest_episodes','top_rated','hero')),
  layout      text not null default 'row' check (layout in ('row','hero','top10','grid')),
  config      jsonb not null default '{}'::jsonb,
  category_id uuid references public.content_categories(id) on delete cascade,
  position    integer not null default 0,
  is_active   boolean not null default true,
  max_items   integer not null default 15 check (max_items between 1 and 50),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table if not exists public.series_shelf_items (
  shelf_id   uuid not null references public.series_shelves(id) on delete cascade,
  series_id  uuid not null references public.series(id)          on delete cascade,
  position   integer not null default 0,
  created_at timestamptz not null default now(),
  primary key (shelf_id, series_id)
);

create table if not exists public.series_likes (
  series_id  uuid not null references public.series(id)   on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (series_id, user_id)
);

create table if not exists public.episode_likes (
  episode_id uuid not null references public.episodes(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (episode_id, user_id)
);

create table if not exists public.series_ratings (
  series_id  uuid not null references public.series(id)   on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  rating     integer not null check (rating between 1 and 10),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (series_id, user_id)
);

create table if not exists public.series_watchlist (
  user_id    uuid not null references public.profiles(id) on delete cascade,
  series_id  uuid not null references public.series(id)   on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, series_id)
);

create table if not exists public.watch_progress (
  user_id      uuid not null references public.profiles(id) on delete cascade,
  episode_id   uuid not null references public.episodes(id) on delete cascade,
  series_id    uuid not null references public.series(id)   on delete cascade,
  position_sec integer not null default 0 check (position_sec >= 0),
  duration_sec integer check (duration_sec >= 0),
  completed    boolean not null default false,
  updated_at   timestamptz not null default now(),
  primary key (user_id, episode_id)
);

create table if not exists public.watch_views (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid references public.profiles(id) on delete set null,
  viewer_key text,
  series_id  uuid not null references public.series(id)   on delete cascade,
  episode_id uuid references public.episodes(id)          on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.series_comments (
  id          uuid primary key default gen_random_uuid(),
  series_id   uuid not null references public.series(id) on delete cascade,
  episode_id  uuid references public.episodes(id) on delete cascade,
  user_id     uuid not null references public.profiles(id) on delete cascade,
  parent_id   uuid references public.series_comments(id) on delete cascade,
  content     text not null check (char_length(content) between 1 and 3000),
  likes_count integer not null default 0,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table if not exists public.series_comment_likes (
  comment_id uuid not null references public.series_comments(id) on delete cascade,
  user_id    uuid not null references public.profiles(id)       on delete cascade,
  created_at timestamptz not null default now(),
  primary key (comment_id, user_id)
);
```

---

## 2. Functions & Triggers

```sql
-- AUTOMATIC PROFILE ON SIGNUP -----------------------------------------
create or replace function public.handle_new_user()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  insert into public.profiles (
    id, username, display_name, avatar_url, role, points, creator_rank, creator_level
  )
  values (
    new.id,
    coalesce(
      nullif(new.raw_user_meta_data->>'username', ''),
      'user_' || substring(new.id::text from 1 for 8)
    ),
    coalesce(
      nullif(new.raw_user_meta_data->>'display_name', ''),
      nullif(new.raw_user_meta_data->>'full_name', ''),
      'Qylomi User'
    ),
    new.raw_user_meta_data->>'avatar_url',
    'user', 0, 'Novice Explorer', 1
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- FREEZE NON-ADMIN PROFILE STATS --------------------------------------
create or replace function public.freeze_profile_protected_fields()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
declare
  caller_role text;
begin
  select role into caller_role from public.profiles where id = (select auth.uid());
  if (caller_role is null or caller_role != 'admin') and (auth.role() != 'service_role') then
    new.points         := old.points;
    new.creator_rank   := old.creator_rank;
    new.creator_level  := old.creator_level;
    new.badges         := old.badges;
    new.points_history := old.points_history;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_freeze_profile_stats on public.profiles;
create trigger trg_freeze_profile_stats
  before update on public.profiles
  for each row execute function public.freeze_profile_protected_fields();

-- TRACKERS COUNT TRIGGER ----------------------------------------------
create or replace function public.update_profile_trackers_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT') then
    update public.profiles set trackers_count = trackers_count + 1 where id = new.following_id;
  elsif (TG_OP = 'DELETE') then
    update public.profiles set trackers_count = greatest(0, trackers_count - 1) where id = old.following_id;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_follows_trackers_count on public.follows;
create trigger trg_follows_trackers_count
  after insert or delete on public.follows
  for each row execute function public.update_profile_trackers_count();

-- POST COUNTERS & TRIGGERS --------------------------------------------
create or replace function public.update_post_reaction_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT') then
    update public.posts set appreciations_count = appreciations_count + 1 where id = new.post_id;
  elsif (TG_OP = 'DELETE') then
    update public.posts set appreciations_count = greatest(0, appreciations_count - 1) where id = old.post_id;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_post_reaction_count on public.post_reactions;
create trigger trg_post_reaction_count
  after insert or delete on public.post_reactions
  for each row execute function public.update_post_reaction_count();

create or replace function public.update_post_comment_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT') then
    update public.posts set replies_count = replies_count + 1 where id = new.post_id;
  elsif (TG_OP = 'DELETE') then
    update public.posts set replies_count = greatest(0, replies_count - 1) where id = old.post_id;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_post_comment_count on public.comments;
create trigger trg_post_comment_count
  after insert or delete on public.comments
  for each row execute function public.update_post_comment_count();

create or replace function public.update_post_relay_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT') then
    update public.posts set relays_count = relays_count + 1 where id = new.post_id;
  elsif (TG_OP = 'DELETE') then
    update public.posts set relays_count = greatest(0, relays_count - 1) where id = old.post_id;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_post_relay_count on public.post_relays;
create trigger trg_post_relay_count
  after insert or delete on public.post_relays
  for each row execute function public.update_post_relay_count();

-- TAG & GROUP POST COUNTERS -------------------------------------------
create or replace function public.update_tag_posts_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT') then
    update public.tags set posts_count = posts_count + 1 where id = new.tag_id;
  elsif (TG_OP = 'DELETE') then
    update public.tags set posts_count = greatest(0, posts_count - 1) where id = old.tag_id;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_post_tags_count on public.post_tags;
create trigger trg_post_tags_count
  after insert or delete on public.post_tags
  for each row execute function public.update_tag_posts_count();

create or replace function public.update_group_posts_count()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  if (TG_OP = 'INSERT' and new.group_id is not null) then
    update public.groups set posts_count = posts_count + 1 where id = new.group_id;
  elsif (TG_OP = 'DELETE' and old.group_id is not null) then
    update public.groups set posts_count = greatest(0, posts_count - 1) where id = old.group_id;
  elsif (TG_OP = 'UPDATE' and coalesce(old.group_id, '00000000-0000-0000-0000-000000000000'::uuid) != coalesce(new.group_id, '00000000-0000-0000-0000-000000000000'::uuid)) then
    if (old.group_id is not null) then
      update public.groups set posts_count = greatest(0, posts_count - 1) where id = old.group_id;
    end if;
    if (new.group_id is not null) then
      update public.groups set posts_count = posts_count + 1 where id = new.group_id;
    end if;
  end if;
  return null;
end;
$$;

drop trigger if exists trg_group_posts_count on public.posts;
create trigger trg_group_posts_count
  after insert or update or delete on public.posts
  for each row execute function public.update_group_posts_count();

-- UPDATED_AT TIMESTAMP TRIGGERS ---------------------------------------
create or replace function public.set_updated_at_column()
returns trigger
security definer
set search_path = ''
language plpgsql as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

drop trigger if exists trg_profiles_set_updated_at on public.profiles;
create trigger trg_profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at_column();

drop trigger if exists trg_posts_set_updated_at on public.posts;
create trigger trg_posts_set_updated_at
  before update on public.posts
  for each row execute function public.set_updated_at_column();

drop trigger if exists trg_playlists_set_updated_at on public.playlists;
create trigger trg_playlists_set_updated_at
  before update on public.playlists
  for each row execute function public.set_updated_at_column();

drop trigger if exists trg_groups_set_updated_at on public.groups;
create trigger trg_groups_set_updated_at
  before update on public.groups
  for each row execute function public.set_updated_at_column();

-- RPC: NOTIFICATIONS --------------------------------------------------
create or replace function public.send_notification(
  p_recipient_id uuid,
  p_type text,
  p_post_id uuid default null,
  p_comment_id uuid default null
)
returns void
security definer
set search_path = ''
language plpgsql as $$
declare
  v_actor_id uuid := (select auth.uid());
begin
  if v_actor_id is null then
    raise exception 'Authentication required';
  end if;
  if v_actor_id = p_recipient_id then
    return;
  end if;

  insert into public.notifications (recipient_id, actor_id, type, post_id, comment_id)
  values (p_recipient_id, v_actor_id, p_type, p_post_id, p_comment_id);
end;
$$;

-- RPC: WALLET TRANSACTIONS --------------------------------------------
create or replace function public.process_creator_wallet_transaction(
  p_user_id uuid,
  p_action text,
  p_points integer,
  p_description text,
  p_reference_id text default null,
  p_reference_type text default null,
  p_idempotency_key text default null
)
returns jsonb
security definer
set search_path = ''
language plpgsql as $$
declare
  v_current_points integer;
  v_new_points integer;
  v_new_rank text;
  v_new_level integer;
begin
  if p_idempotency_key is not null then
    if exists (select 1 from public.wallet_transactions where idempotency_key = p_idempotency_key) then
      select points into v_current_points from public.profiles where id = p_user_id;
      return jsonb_build_object('success', true, 'idempotent', true, 'balance', v_current_points);
    end if;
  end if;

  select points into v_current_points from public.profiles where id = p_user_id for update;
  if not found then
    raise exception 'User profile not found';
  end if;

  v_new_points := greatest(0, v_current_points + p_points);

  if v_new_points >= 4000 then v_new_rank := 'Master Craftsman'; v_new_level := 5;
  elsif v_new_points >= 1500 then v_new_rank := 'Community Mentor'; v_new_level := 4;
  elsif v_new_points >= 600 then v_new_rank := 'Idea Pioneer'; v_new_level := 3;
  elsif v_new_points >= 200 then v_new_rank := 'Active Contributor'; v_new_level := 2;
  else v_new_rank := 'Novice Explorer'; v_new_level := 1;
  end if;

  update public.profiles
  set points = v_new_points,
      creator_rank = v_new_rank,
      creator_level = v_new_level
  where id = p_user_id;

  insert into public.wallet_transactions (
    user_id, action, points, balance_after, description, reference_id, reference_type, idempotency_key
  )
  values (
    p_user_id, p_action, p_points, v_new_points, p_description, p_reference_id, p_reference_type, p_idempotency_key
  );

  return jsonb_build_object(
    'success', true,
    'balance', v_new_points,
    'rank', v_new_rank,
    'level', v_new_level
  );
end;
$$;

-- RPC: INCREMENT MONETIZATION STATS -----------------------------------
create or replace function public.increment_monetization_stats(
  p_user_id uuid,
  p_views bigint default 1,
  p_watch_seconds bigint default 0
)
returns void
security definer
set search_path = ''
language plpgsql as $$
declare
  v_trackers integer;
  v_views bigint;
  v_earnings numeric;
begin
  insert into public.monetization_stats (user_id, total_views, total_watch_seconds, total_monetized_views)
  values (p_user_id, p_views, p_watch_seconds, p_views)
  on conflict (user_id) do update set
    total_views = public.monetization_stats.total_views + p_views,
    total_watch_seconds = public.monetization_stats.total_watch_seconds + p_watch_seconds,
    total_monetized_views = public.monetization_stats.total_monetized_views + p_views;

  select coalesce(trackers_count, 0) into v_trackers from public.profiles where id = p_user_id;
  select total_views into v_views from public.monetization_stats where user_id = p_user_id;

  v_earnings := (v_views / 1000.0) * 1.00;

  update public.monetization_stats
  set total_earnings_usd = v_earnings,
      status = case
        when status in ('approved', 'suspended') then status
        when v_trackers >= 500 and v_views >= 10000 then 'eligible'
        else 'not_eligible'
      end
  where user_id = p_user_id;
end;
$$;
```

---

## 3. RLS Security Policies

```sql
-- ENABLE RLS ----------------------------------------------------------
alter table public.profiles enable row level security;
alter table public.follows enable row level security;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.posts enable row level security;
alter table public.tags enable row level security;
alter table public.post_tags enable row level security;
alter table public.post_reactions enable row level security;
alter table public.comments enable row level security;
alter table public.comment_likes enable row level security;
alter table public.post_relays enable row level security;
alter table public.saved_posts enable row level security;
alter table public.hidden_posts enable row level security;
alter table public.notifications enable row level security;
alter table public.reports enable row level security;
alter table public.user_events enable row level security;
alter table public.playlists enable row level security;
alter table public.video_posts enable row level security;
alter table public.photo_posts enable row level security;
alter table public.text_posts enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.monetization_stats enable row level security;
alter table public.payout_requests enable row level security;
alter table public.content_categories enable row level security;
alter table public.series enable row level security;
alter table public.series_seasons enable row level security;
alter table public.episodes enable row level security;
alter table public.episode_sources enable row level security;
alter table public.source_reports enable row level security;
alter table public.series_shelves enable row level security;
alter table public.series_shelf_items enable row level security;
alter table public.series_likes enable row level security;
alter table public.episode_likes enable row level security;
alter table public.series_ratings enable row level security;
alter table public.series_watchlist enable row level security;
alter table public.watch_progress enable row level security;
alter table public.watch_views enable row level security;
alter table public.series_comments enable row level security;
alter table public.series_comment_likes enable row level security;

-- PROFILES POLICIES ---------------------------------------------------
drop policy if exists "Profiles are viewable by everyone" on public.profiles;
create policy "Profiles are viewable by everyone" on public.profiles
  for select using (true);

drop policy if exists "Users can insert own profile" on public.profiles;
create policy "Users can insert own profile" on public.profiles
  for insert with check ((select auth.uid()) = id);

drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile" on public.profiles
  for update using ((select auth.uid()) = id);

-- POSTS POLICIES ------------------------------------------------------
drop policy if exists "Posts are viewable by everyone" on public.posts;
create policy "Posts are viewable by everyone" on public.posts
  for select using (true);

drop policy if exists "Users can insert own posts" on public.posts;
create policy "Users can insert own posts" on public.posts
  for insert with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update own posts" on public.posts;
create policy "Users can update own posts" on public.posts
  for update using ((select auth.uid()) = user_id);

drop policy if exists "Users can delete own posts" on public.posts;
create policy "Users can delete own posts" on public.posts
  for delete using ((select auth.uid()) = user_id);

-- TYPED POST EXTENSIONS POLICIES (video_posts, photo_posts, text_posts)
drop policy if exists "Video posts visible to all" on public.video_posts;
create policy "Video posts visible to all" on public.video_posts for select using (true);

drop policy if exists "Video posts insert owner" on public.video_posts;
create policy "Video posts insert owner" on public.video_posts
  for insert with check (
    exists (select 1 from public.posts p where p.id = video_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Video posts update owner" on public.video_posts;
create policy "Video posts update owner" on public.video_posts
  for update using (
    exists (select 1 from public.posts p where p.id = video_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Video posts delete owner" on public.video_posts;
create policy "Video posts delete owner" on public.video_posts
  for delete using (
    exists (select 1 from public.posts p where p.id = video_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Photo posts visible to all" on public.photo_posts;
create policy "Photo posts visible to all" on public.photo_posts for select using (true);

drop policy if exists "Photo posts insert owner" on public.photo_posts;
create policy "Photo posts insert owner" on public.photo_posts
  for insert with check (
    exists (select 1 from public.posts p where p.id = photo_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Photo posts update owner" on public.photo_posts;
create policy "Photo posts update owner" on public.photo_posts
  for update using (
    exists (select 1 from public.posts p where p.id = photo_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Photo posts delete owner" on public.photo_posts;
create policy "Photo posts delete owner" on public.photo_posts
  for delete using (
    exists (select 1 from public.posts p where p.id = photo_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Text posts visible to all" on public.text_posts;
create policy "Text posts visible to all" on public.text_posts for select using (true);

drop policy if exists "Text posts insert owner" on public.text_posts;
create policy "Text posts insert owner" on public.text_posts
  for insert with check (
    exists (select 1 from public.posts p where p.id = text_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Text posts update owner" on public.text_posts;
create policy "Text posts update owner" on public.text_posts
  for update using (
    exists (select 1 from public.posts p where p.id = text_posts.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Text posts delete owner" on public.text_posts;
create policy "Text posts delete owner" on public.text_posts
  for delete using (
    exists (select 1 from public.posts p where p.id = text_posts.post_id and p.user_id = (select auth.uid()))
  );

-- COMMENTS POLICIES ---------------------------------------------------
drop policy if exists "Authenticated users can write comments" on public.comments;
drop policy if exists "Comments visible to all" on public.comments;
create policy "Comments visible to all" on public.comments for select using (true);

drop policy if exists "Users can create comments" on public.comments;
create policy "Users can create comments" on public.comments
  for insert with check (
    (select auth.uid()) = user_id
    and exists (select 1 from public.posts p where p.id = comments.post_id and p.allow_comments = true)
  );

drop policy if exists "Users can update own comments" on public.comments;
create policy "Users can update own comments" on public.comments
  for update using ((select auth.uid()) = user_id);

drop policy if exists "Users can delete own comments" on public.comments;
create policy "Users can delete own comments" on public.comments
  for delete using ((select auth.uid()) = user_id);

-- MONETIZATION & WALLET POLICIES --------------------------------------
drop policy if exists "Monetization stats read own" on public.monetization_stats;
create policy "Monetization stats read own" on public.monetization_stats
  for select using ((select auth.uid()) = user_id);

drop policy if exists "Users can insert or update their own monetization stats" on public.monetization_stats;

drop policy if exists "Wallet transactions read own" on public.wallet_transactions;
create policy "Wallet transactions read own" on public.wallet_transactions
  for select using ((select auth.uid()) = user_id);

drop policy if exists "Users can insert wallet transactions" on public.wallet_transactions;

-- POST TAGS POLICIES --------------------------------------------------
drop policy if exists "Post tags visible to all" on public.post_tags;
create policy "Post tags visible to all" on public.post_tags for select using (true);

drop policy if exists "Post owner can attach tags" on public.post_tags;
create policy "Post owner can attach tags" on public.post_tags
  for insert with check (
    exists (select 1 from public.posts p where p.id = post_tags.post_id and p.user_id = (select auth.uid()))
  );

drop policy if exists "Post owner can delete tags" on public.post_tags;
create policy "Post owner can delete tags" on public.post_tags
  for delete using (
    exists (select 1 from public.posts p where p.id = post_tags.post_id and p.user_id = (select auth.uid()))
  );

-- NOTIFICATIONS POLICIES ----------------------------------------------
drop policy if exists "Users can view own notifications" on public.notifications;
create policy "Users can view own notifications" on public.notifications
  for select using ((select auth.uid()) = recipient_id);

drop policy if exists "Users can update own notifications" on public.notifications;
create policy "Users can update own notifications" on public.notifications
  for update using ((select auth.uid()) = recipient_id);

drop policy if exists "Users can insert notifications" on public.notifications;
```

---

## 4. Storage Configuration

```sql
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('qylomi-images', 'qylomi-images', true, 10485760, array['image/jpeg','image/png','image/webp','image/gif'])
on conflict (id) do update set
  public = true,
  file_size_limit = 10485760,
  allowed_mime_types = array['image/jpeg','image/png','image/webp','image/gif'];

drop policy if exists "Images bucket public select" on storage.objects;
create policy "Images bucket public select" on storage.objects
  for select using (bucket_id = 'qylomi-images');

drop policy if exists "Authenticated users can upload images" on storage.objects;
create policy "Authenticated users can upload images" on storage.objects
  for insert with check (bucket_id = 'qylomi-images' and auth.role() = 'authenticated');
```
