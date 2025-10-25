# Development Configuration

This package contains development-only configuration flags for testing and debugging.

## AppDevConfig

The `AppDevConfig` object provides flags to override production behavior for testing purposes.

### Notification Testing Flags

#### `ENABLE_ANNOUNCEMENT_NOTIFICATION`
**Purpose**: Test announcement notifications without changing user preferences

**When to enable**:
- Testing notification content for announcements
- Verifying notification styling and layout
- Testing notification channel behavior
- Debugging announcement notification issues

**What it does**:
- Bypasses `isRssFeedContentNotificationEnabled` user preference check
- Shows notification for ANY new announcements detected by `AnnouncementSyncWorker`
- Uses real announcement content from TRMNL RSS feed
- Still respects POST_NOTIFICATIONS permission (won't crash on Android 13+)

**How to use**:
```kotlin
// In AppDevConfig.kt
const val ENABLE_ANNOUNCEMENT_NOTIFICATION = true  // Enable testing
```

**To trigger**:
1. Enable the flag in `AppDevConfig.kt`
2. Rebuild the app
3. Force announcement sync:
   - Option A: Wait for automatic 4-hour sync interval
   - Option B: Kill and restart app (triggers immediate sync)
   - Option C: Use WorkManager Inspector in Android Studio
4. Check notification tray for new announcement notification

---

#### `ENABLE_BLOG_NOTIFICATION`
**Purpose**: Test blog post notifications without changing user preferences

**When to enable**:
- Testing notification content for blog posts
- Verifying notification styling and layout
- Testing notification channel behavior
- Debugging blog post notification issues

**What it does**:
- Bypasses `isRssFeedContentNotificationEnabled` user preference check
- Shows notification for ANY new blog posts detected by `BlogPostSyncWorker`
- Uses real blog post content from TRMNL RSS feed
- Still respects POST_NOTIFICATIONS permission (won't crash on Android 13+)

**How to use**:
```kotlin
// In AppDevConfig.kt
const val ENABLE_BLOG_NOTIFICATION = true  // Enable testing
```

**To trigger**:
1. Enable the flag in `AppDevConfig.kt`
2. Rebuild the app
3. Force blog post sync:
   - Option A: Wait for automatic 6-hour sync interval
   - Option B: Kill and restart app (triggers immediate sync)
   - Option C: Use WorkManager Inspector in Android Studio
4. Check notification tray for new blog post notification

---

#### `VERBOSE_NOTIFICATION_LOGGING`
**Purpose**: Enable detailed logging for notification debugging

**When to enable**:
- Debugging notification issues
- Understanding notification flow
- Troubleshooting permission problems

**What it does**:
- Logs notification creation attempts
- Logs permission check results
- Logs preference check results
- Logs notification display success/failure

**How to use**:
```kotlin
// In AppDevConfig.kt
const val VERBOSE_NOTIFICATION_LOGGING = true  // Enable verbose logging
```

Then check Logcat with filter: `tag:NotificationHelper OR tag:AnnouncementSyncWorker OR tag:BlogPostSyncWorker`

---

## Testing Workflow

### Quick Test (Recommended)
1. **Enable dev flags** in `AppDevConfig.kt`:
   ```kotlin
   const val ENABLE_ANNOUNCEMENT_NOTIFICATION = true
   const val ENABLE_BLOG_NOTIFICATION = true
   ```

2. **Clear app data** (to force fresh sync):
   ```bash
   adb shell pm clear ink.trmnl.android.buddy
   ```

3. **Rebuild and launch** app:
   ```bash
   ./gradlew installDebug && adb shell am start -n ink.trmnl.android.buddy/.MainActivity
   ```

4. **Check notifications**: Both blog post and announcement notifications should appear after sync completes

### Manual WorkManager Trigger (Advanced)
Using Android Studio's WorkManager Inspector:

1. Open **App Inspection** > **Background Task Inspector** in Android Studio
2. Find `AnnouncementSyncWorker` or `BlogPostSyncWorker`
3. Click **Run Now** to trigger immediate sync
4. Check notification tray

---

## Production Safety

⚠️ **WARNING**: These flags bypass user preferences and should NEVER be enabled in production builds.

**Safety measures**:
- All flags default to `false`
- Clear documentation warns about production usage
- Flags are in dedicated `dev` package (easy to exclude)
- Still respect POST_NOTIFICATIONS permission (won't crash)

**Recommended for production**:
- Add ProGuard/R8 rule to strip dev package in release builds
- Add lint check to fail builds if dev flags are enabled

---

## Troubleshooting

### "No notifications appearing"
1. Check POST_NOTIFICATIONS permission is granted (Android 13+)
2. Check notification channel is enabled in system settings
3. Verify dev flags are enabled in `AppDevConfig.kt`
4. Check Logcat for notification logs
5. Ensure RSS feed has new content (workers only notify on NEW items)

### "How to test with fresh content?"
Clear app database to reset "read" status:
```bash
adb shell pm clear ink.trmnl.android.buddy
```

### "Worker not running?"
Check WorkManager status in App Inspection:
- Workers scheduled after app login
- Check WorkManager constraints (network, battery)
- Check if workers are in `ENQUEUED` or `RUNNING` state

---

## Related Files

- `NotificationHelper.kt` - Centralized notification creation
- `AnnouncementSyncWorker.kt` - Announcement sync worker
- `BlogPostSyncWorker.kt` - Blog post sync worker
- `UserPreferencesRepository.kt` - User preference storage

---

## Best Practices

1. **Always disable flags before committing** (unless intentionally testing)
2. **Use verbose logging** when debugging notification issues
3. **Test both flags independently** and together
4. **Verify production behavior** with flags disabled
5. **Clear app data** between tests for consistent results
