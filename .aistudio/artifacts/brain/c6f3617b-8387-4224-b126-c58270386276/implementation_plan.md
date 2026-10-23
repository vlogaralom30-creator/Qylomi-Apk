# Implementation Plan: Relay & Notifications System

## 1. Goal
Implement a real "Relay" (repost) feature and a complete "Notifications" system in the QSocial app.

## 2. SQL Migration (Run once in Supabase SQL Editor)
```sql
-- 1. Partial unique index for notifications to avoid spamming
CREATE UNIQUE INDEX IF NOT EXISTS uniq_notif_event 
ON public.notifications (recipient_id, actor_id, type, post_id, comment_id) 
WHERE read = false;

-- 2. Update send_notification RPC to handle conflict (optional, or rely on application logic)
-- Assuming existing RPC is used.
```

## 3. Data Layer
- **`RelayRepository.kt`**:
  - `toggleRelay(postId, quote)`: Perform Insert/Delete.
  - `getRelayedPostIds()`: Fetch IDs for current user.
- **`NotificationRepository.kt`**:
  - `notify(recipientId, type, postId, commentId)`: Call RPC `send_notification`.
  - `getNotifications()`: Fetch list.
  - `markAsRead(notificationId)`: Update read status.

## 4. UI Layer
- **`RelayBottomSheet.kt`**: Preview post, quote field, Relay/Cancel buttons.
- **`NotificationsScreen.kt`**: Rebuild to show real notification items (Appreciate, Reply, Relay), with navigation to post/profile.
- **`PostCard.kt`**: Update Relay button to open `RelayBottomSheet`.
- **`MainActivity.kt`**: Badge logic for unread notifications.

## 5. Verification
- `compile_applet` after each major module.
