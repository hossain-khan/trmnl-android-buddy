# Testing Notifications

This document explains how to test all notification features during development without waiting for scheduled intervals or specific conditions.

## 📱 Notification Types

The app supports three types of notifications:

1. **Low Battery Alerts** - Weekly checks for devices with low battery
2. **Blog Post Updates** - New blog posts from TRMNL RSS feed (6-hour interval)
3. **Announcement Updates** - New announcements from TRMNL RSS feed (4-hour interval)

---

## 🧪 Testing Strategies

### Overview

Each notification type can be tested using different methods:

| Notification Type | Debug Button | Dev Screen | Dev Config Flags | Manual Trigger |
|------------------|--------------|------------|------------------|----------------|
| Low Battery | ✅ Yes | ✅ Yes | ❌ No | ✅ WorkManager |
| Blog Posts | ❌ No | ✅ Yes | ✅ Yes | ✅ WorkManager |
| Announcements | ❌ No | ✅ Yes | ✅ Yes | ✅ WorkManager |

---

## 🛠️ Using the Development Screen (Recommended)

**The easiest way to test all notifications is through the Development screen.**

### Accessing the Development Screen

The Development screen is **only available in debug builds** and provides comprehensive testing tools.

**How to access:**
1. Build and install a debug APK: `./gradlew installDebug`
2. Open the app and navigate to **Settings**
3. Scroll to the bottom and tap **"Development Tools"** (only visible in debug builds)
4. You'll see the Development screen with multiple testing sections

### Features

The Development screen provides:

#### 1. **Permission Status**
- Shows whether POST_NOTIFICATIONS permission is granted (Android 13+)
- Color-coded status: Green (granted) / Red (denied)
- Quick actions:
  - Request permission button
  - Open system notification settings

#### 2. **Low Battery Notification Testing**
- **Device Count Slider**: Test with 1-5 devices
- **Threshold Slider**: Adjust battery threshold (5-50%)
- **Mock Data**: Uses predefined device names (no API calls)
- **Instant Testing**: Tap button to see notification immediately

**Example workflow:**
```
1. Set device count: 2
2. Set threshold: 30%
3. Tap "Test Low Battery Notification"
4. See notification: "Low Battery: 2 devices"
   Content: "Living Room Display, Kitchen Display are below 30%"
```

#### 3. **RSS Feed Notification Testing**

**Blog Posts:**
- **Post Count Slider**: Test with 1-10 new posts
- **Mock Data**: No RSS feed fetching required
- **Instant Testing**: Tap button to see notification immediately

**Announcements:**
- **Announcement Count Slider**: Test with 1-10 new announcements
- **Mock Data**: No RSS feed fetching required
- **Instant Testing**: Tap button to see notification immediately

**Example workflow:**
```
1. Set blog posts: 3
2. Tap "Test Blog Post Notification"
3. See notification: "New Blog Posts: 3"

4. Set announcements: 2
5. Tap "Test Announcement Notification"
6. See notification: "New Announcements: 2"
```

#### 4. **Worker Triggers**

Manually trigger one-time worker executions with **real API/RSS data**:

- **Trigger Low Battery Worker**: Fetches real device data from TRMNL API
- **Trigger Blog Post Worker**: Fetches real blog posts from RSS feed
- **Trigger Announcement Worker**: Fetches real announcements from RSS feed

**Use cases:**
- Test actual API integration
- Verify worker logic with real data
- Debug network issues
- Test error handling

#### 5. **Development Info**

Helpful reminders about the Development screen:
- Only available in debug builds
- Notification tests use mock data (no API calls)
- Worker triggers use real API/RSS data
- Check logcat for detailed execution logs

### Why Use the Development Screen?

**Advantages over other testing methods:**

✅ **No code changes required** - No need to modify dev flags or worker intervals  
✅ **Instant feedback** - See notifications immediately without waiting  
✅ **Parameter control** - Adjust device counts, thresholds, and post counts on the fly  
✅ **Safe testing** - Mock data prevents API rate limits  
✅ **Permission management** - Built-in permission status and controls  
✅ **Worker testing** - Manual triggers for real API/RSS data testing  

**When to use mock vs real data:**

- **Mock data (Notification Testing)**: Quick UI testing, different notification variations, no network required
- **Real data (Worker Triggers)**: Test actual API integration, verify data parsing, test error handling

### Monitoring Logs

Watch Development screen activity in logcat:

```bash
# Monitor all development screen actions
adb logcat | grep -E "DevelopmentPresenter|NotificationHelper"
```

**Expected log output:**
```
DevelopmentPresenter: Testing low battery notification: 2 devices, 30% threshold
NotificationHelper: Showing low battery notification for 2 device(s): [Living Room Display, Kitchen Display]

DevelopmentPresenter: Testing blog post notification: 3 new posts
NotificationHelper: Showing blog post notification for 3 new post(s)

DevelopmentPresenter: Triggering one-time LowBatteryNotificationWorker
DevelopmentPresenter: Enqueued one-time work request for LowBatteryNotificationWorker
```

---

## 🔋 Testing Low Battery Notifications

### 1. **Debug Test Button (Recommended)**

In **debug builds only**, a "Test Notification Now" button appears in the Settings screen under the Low Battery Alerts section.

**How to use:**
1. Build and install a debug APK: `./gradlew installDebug`
2. Open the app and navigate to **Settings**
3. Enable **Low Battery Notifications** toggle
4. Set your desired **Alert Threshold** (e.g., 50% to increase chances)
5. Tap the **"Test Notification Now (Debug)"** button
6. The worker will run immediately with your current settings
7. Check logcat to see the worker execution: 
   ```bash
   adb logcat | grep "LowBatteryNotificationWorker"
   ```

**What happens:**
- Triggers an immediate `OneTimeWorkRequest` for `LowBatteryNotificationWorker`
- Uses your current threshold setting
- Fetches real device data from TRMNL API
- Shows notification if any devices are below threshold

### 2. **Monitor Worker Execution via Logcat**

Watch real-time logs to see what the worker is doing:

```bash
# Filter for low battery worker logs
adb logcat | grep -E "LowBatteryNotificationWorker|NotificationHelper|WorkerScheduler"
```

**Expected log output:**
```
LowBatteryNotificationWorker: Starting low battery notification check
LowBatteryNotificationWorker: Fetched 3 devices
LowBatteryNotificationWorker: Found 1 devices with low battery
NotificationHelper: Showing low battery notification for 1 device(s): [Living Room Display]
LowBatteryNotificationWorker: Low battery notification check completed successfully
```

### 3. **Lower Threshold for Testing**

Temporarily increase your threshold to 50% so more devices trigger notifications:

1. Go to Settings → Low Battery Alerts
2. Slide the threshold to **50%**
3. Tap "Test Notification Now"
4. Any devices below 50% battery will trigger a notification

### 4. **WorkManager Inspection**

Use Android's WorkManager inspection tools:

**Via adb:**
```bash
# List all scheduled work
adb shell dumpsys jobscheduler | grep ink.trmnl

# Check WorkManager database
adb shell dumpsys activity service WorkManagerService
```

**Via Android Studio:**
1. Go to **View → Tool Windows → App Inspection**
2. Select **Background Task Inspector**
3. View scheduled and running workers
4. Manually trigger or cancel workers

### 5. **Modify Worker Schedule for Faster Testing**

For local testing, you can temporarily reduce the interval:

**In `WorkerSchedulerImpl.scheduleLowBatteryNotification()`:**
```kotlin
// TEMPORARY: Change from 7 days to 15 minutes for testing
val notificationWorkRequest =
    PeriodicWorkRequestBuilder<LowBatteryNotificationWorker>(
        repeatInterval = 15,  // Changed from 7
        repeatIntervalTimeUnit = TimeUnit.MINUTES,  // Changed from DAYS
    )
```

⚠️ **Remember to revert this before committing!**

## 📱 Testing Scenarios

### Scenario 1: Single Device Low Battery
**Setup:**
- Set threshold to 50%
- Ensure at least one device has <50% battery
- Tap "Test Notification Now"

**Expected:**
- Notification: "Low Battery: [Device Name]"
- Content: "Battery level is below 50%"

### Scenario 2: Multiple Devices Low Battery
**Setup:**
- Set threshold to 50%
- Ensure multiple devices have <50% battery
- Tap "Test Notification Now"

**Expected:**
- Notification: "Low Battery: X devices"
- Content: "[Device1], [Device2], [Device3] are below 50%"

### Scenario 3: No Low Battery Devices
**Setup:**
- Set threshold to 5%
- Ensure all devices have >5% battery
- Tap "Test Notification Now"

**Expected:**
- No notification shown
- Log: "No devices with low battery"

### Scenario 4: Network Error Handling
**Setup:**
- Turn off WiFi/mobile data
- Tap "Test Notification Now"

**Expected:**
- Worker retries automatically
- Log: "Network error fetching devices"

### Scenario 5: Notification Permission (Android 13+)
**Setup:**
- On Android 13+ device
- Deny POST_NOTIFICATIONS permission
- Tap "Test Notification Now"

**Expected:**
- No notification shown
- Log: "POST_NOTIFICATIONS permission not granted"

## 🔍 Debugging Tips

### Check Notification Channel
```bash
adb shell dumpsys notification | grep -A 20 "ink.trmnl.android.buddy"
```

### Force Stop and Clear Data
```bash
adb shell pm clear ink.trmnl.android.buddy
```

### Check DataStore Preferences
```bash
adb shell run-as ink.trmnl.android.buddy cat /data/data/ink.trmnl.android.buddy/files/datastore/user_prefs.preferences_pb
```

### Manual Notification Test
You can also manually trigger the notification from code for UI testing:
```kotlin
NotificationHelper.showLowBatteryNotification(
    context = context,
    deviceNames = listOf("Test Device 1", "Test Device 2"),
    thresholdPercent = 20
)
```

## 🚀 Production Behavior

In **release builds**:
- Debug test button is **hidden**
- Worker runs **weekly** (every 7 days)
- Requires **network connectivity** (will wait if offline)
- Uses **REPLACE** policy (updates when settings change)

## 📊 Verifying Feature Works End-to-End

**Complete test flow:**
1. ✅ Enable low battery notifications in Settings
2. ✅ Set threshold (e.g., 30%)
3. ✅ Tap "Test Notification Now"
4. ✅ Verify worker executes (check logcat)
5. ✅ Verify API call succeeds (fetches devices)
6. ✅ Verify notification appears (if devices below threshold)
7. ✅ Tap notification (should dismiss)
8. ✅ Change threshold and test again
9. ✅ Disable notifications and verify worker cancels
10. ✅ Re-enable and verify worker reschedules

## 🐛 Common Issues

### Issue: No notification appears
**Possible causes:**
- All devices above threshold
- Notification permission denied (Android 13+)
- Notification channel disabled in system settings
- Network error preventing API call

**Fix:** Check logcat for specific error messages

### Issue: Worker doesn't run immediately
**Possible causes:**
- Device in Doze mode
- Network constraint not met (no WiFi/data)
- WorkManager quota exhausted

**Fix:** 
- Disable battery optimization for debug builds
- Ensure network connectivity
- Check WorkManager constraints

### Issue: "Test Notification Now" button not visible
**Possible causes:**
- Running release build instead of debug
- BuildConfig.DEBUG is false

**Fix:** Run `./gradlew installDebug` instead of `assembleRelease`

---

## � Testing Blog Post Notifications

Blog post notifications are triggered when new blog posts are detected from the TRMNL RSS feed.

### Method 1: **Development Config Flags (Recommended)**

Use `AppDevConfig` to force notifications regardless of user preference.

**Steps:**
1. **Enable the dev flag** in `app/src/main/java/ink/trmnl/android/buddy/dev/AppDevConfig.kt`:
   ```kotlin
   const val ENABLE_BLOG_NOTIFICATION = true  // Enable testing
   ```

2. **Rebuild and install**:
   ```bash
   ./gradlew installDebug
   ```

3. **Clear app data** (to get fresh content):
   ```bash
   adb shell pm clear ink.trmnl.android.buddy
   ```

4. **Launch app and login** with your API token

5. **Enable RSS Feed Content** in Settings (if not already enabled)

6. **Wait ~10 seconds** for automatic blog post sync to complete

7. **Check notification tray** - you should see a notification with real blog post content!

**What happens:**
- Worker bypasses `isRssFeedContentNotificationEnabled` user preference
- Shows notification for ANY new blog posts detected
- Uses real blog post titles and content from TRMNL RSS feed
- Still respects POST_NOTIFICATIONS permission (Android 13+)

### Method 2: **User Preference (Production Behavior)**

Test the actual user flow:

**Steps:**
1. Build and install app: `./gradlew installDebug`
2. Login with your API token
3. Go to **Settings**
4. Enable **"Enable Blog Posts & Announcements"** toggle
5. Expand and enable **"Notifications for New Content"** toggle
6. Clear app data to force fresh sync: `adb shell pm clear ink.trmnl.android.buddy`
7. Re-login and wait for sync
8. Check notification tray

### Method 3: **Manual WorkManager Trigger**

Force immediate blog post sync:

**Via Android Studio:**
1. Go to **View → Tool Windows → App Inspection**
2. Select **Background Task Inspector**
3. Find `BlogPostSyncWorker`
4. Click **Run Now**
5. Monitor logs: `adb logcat | grep BlogPostSyncWorker`

**Via adb:**
```bash
# Trigger immediate one-time sync (requires WorkManager inspection or custom test code)
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS
```

### Method 4: **Reduce Sync Interval (Temporary)**

For rapid testing, temporarily reduce the sync interval:

**In `WorkerSchedulerImpl.scheduleBlogPostSync()`:**
```kotlin
// TEMPORARY: Change from 6 hours to 15 minutes for testing
val blogPostWorkRequest =
    PeriodicWorkRequestBuilder<BlogPostSyncWorker>(
        repeatInterval = 15,  // Changed from 6
        repeatIntervalTimeUnit = TimeUnit.MINUTES,  // Changed from HOURS
    )
```

⚠️ **Remember to revert this before committing!**

### Monitoring Blog Post Notifications

**Watch logs in real-time:**
```bash
adb logcat | grep -E "BlogPostSyncWorker|NotificationHelper"
```

**Expected log output:**
```
BlogPostSyncWorker: Starting blog post sync
BlogPostSyncWorker: Sync successful. New posts: 2
BlogPostSyncWorker: Dev flag enabled - showing blog post notification for testing
NotificationHelper: Showing blog post notification for 2 new post(s)
```

---

## 📢 Testing Announcement Notifications

Announcement notifications are triggered when new announcements are detected from the TRMNL RSS feed.

### Method 1: **Development Config Flags (Recommended)**

Use `AppDevConfig` to force notifications regardless of user preference.

**Steps:**
1. **Enable the dev flag** in `app/src/main/java/ink/trmnl/android/buddy/dev/AppDevConfig.kt`:
   ```kotlin
   const val ENABLE_ANNOUNCEMENT_NOTIFICATION = true  // Enable testing
   ```

2. **Rebuild and install**:
   ```bash
   ./gradlew installDebug
   ```

3. **Clear app data** (to get fresh content):
   ```bash
   adb shell pm clear ink.trmnl.android.buddy
   ```

4. **Launch app and login** with your API token

5. **Enable RSS Feed Content** in Settings (if not already enabled)

6. **Wait ~10 seconds** for automatic announcement sync to complete

7. **Check notification tray** - you should see a notification with real announcement content!

**What happens:**
- Worker bypasses `isRssFeedContentNotificationEnabled` user preference
- Shows notification for ANY new announcements detected
- Uses real announcement titles and content from TRMNL RSS feed
- Still respects POST_NOTIFICATIONS permission (Android 13+)

### Method 2: **User Preference (Production Behavior)**

Test the actual user flow:

**Steps:**
1. Build and install app: `./gradlew installDebug`
2. Login with your API token
3. Go to **Settings**
4. Enable **"Enable Blog Posts & Announcements"** toggle
5. Expand and enable **"Notifications for New Content"** toggle
6. Clear app data to force fresh sync: `adb shell pm clear ink.trmnl.android.buddy`
7. Re-login and wait for sync
8. Check notification tray

### Method 3: **Manual WorkManager Trigger**

Force immediate announcement sync:

**Via Android Studio:**
1. Go to **View → Tool Windows → App Inspection**
2. Select **Background Task Inspector**
3. Find `AnnouncementSyncWorker`
4. Click **Run Now**
5. Monitor logs: `adb logcat | grep AnnouncementSyncWorker`

### Method 4: **Reduce Sync Interval (Temporary)**

For rapid testing, temporarily reduce the sync interval:

**In `WorkerSchedulerImpl.scheduleAnnouncementSync()`:**
```kotlin
// TEMPORARY: Change from 4 hours to 15 minutes for testing
val announcementWorkRequest =
    PeriodicWorkRequestBuilder<AnnouncementSyncWorker>(
        repeatInterval = 15,  // Changed from 4
        repeatIntervalTimeUnit = TimeUnit.MINUTES,  // Changed from HOURS
    )
```

⚠️ **Remember to revert this before committing!**

### Monitoring Announcement Notifications

**Watch logs in real-time:**
```bash
adb logcat | grep -E "AnnouncementSyncWorker|NotificationHelper"
```

**Expected log output:**
```
AnnouncementSyncWorker: Starting announcement sync
Announcement sync completed successfully. New announcements: 1
AnnouncementSyncWorker: Dev flag enabled - showing announcement notification for testing
NotificationHelper: Showing announcement notification for 1 new announcement(s)
```

---

## 🚀 Testing All Notifications Together

Test all notification types in one go:

**Quick Test Workflow:**

1. **Enable all dev flags** in `AppDevConfig.kt`:
   ```kotlin
   const val ENABLE_ANNOUNCEMENT_NOTIFICATION = true
   const val ENABLE_BLOG_NOTIFICATION = true
   const val VERBOSE_NOTIFICATION_LOGGING = true  // Optional: detailed logs
   ```

2. **Build and install**:
   ```bash
   ./gradlew installDebug
   ```

3. **Clear app data**:
   ```bash
   adb shell pm clear ink.trmnl.android.buddy
   ```

4. **Launch app** and complete login

5. **Enable all notification settings**:
   - Settings → Low Battery Notifications → Enable
   - Settings → Enable Blog Posts & Announcements → Enable
   - Settings → Notifications for New Content → Enable

6. **Trigger all notifications**:
   - **Low Battery**: Tap "Test Notification Now" button in Settings
   - **Blog Posts**: Wait ~10 seconds after login (automatic sync)
   - **Announcements**: Wait ~10 seconds after login (automatic sync)

7. **Monitor all logs**:
   ```bash
   adb logcat | grep -E "LowBatteryNotificationWorker|BlogPostSyncWorker|AnnouncementSyncWorker|NotificationHelper"
   ```

8. **Check notification tray** - you should see up to 3 notifications!

---

## 📊 Testing Scenarios

### Scenario 1: Fresh User (All New Content)
**Setup:**
- Clear app data
- Login for first time
- Enable all notifications

**Expected:**
- Blog post notification (if new posts exist)
- Announcement notification (if new announcements exist)
- Low battery notification (if devices below threshold)

### Scenario 2: No New Content
**Setup:**
- Don't clear app data
- All content already synced and read
- Trigger syncs again

**Expected:**
- No RSS feed notifications (no new content detected)
- Low battery notification only if devices are below threshold

### Scenario 3: Dev Flags vs User Preference
**Setup:**
- Disable "Notifications for New Content" in Settings
- Enable `ENABLE_BLOG_NOTIFICATION` dev flag
- Trigger blog post sync

**Expected:**
- Notification STILL appears (dev flag bypasses user preference)
- Log shows: "Dev flag enabled - showing notification for testing"

### Scenario 4: Android 13+ Permission Denied
**Setup:**
- Android 13+ device
- Deny POST_NOTIFICATIONS permission
- Trigger any notification

**Expected:**
- No notification appears
- Log shows: "POST_NOTIFICATIONS permission not granted"
- App doesn't crash

### Scenario 5: Notification Channel Disabled
**Setup:**
- Enable notifications in app
- Disable "RSS Feed Updates" channel in system settings
- Trigger blog/announcement notification

**Expected:**
- Notification attempts to show but may be blocked by system
- Check: Settings → Apps → TRMNL Buddy → Notifications

---

## 🔍 Advanced Debugging

### Check All Notification Channels
```bash
adb shell dumpsys notification | grep -A 30 "ink.trmnl.android.buddy"
```

**Expected channels:**
- `low_battery_alerts` - Low Battery Alerts
- `rss_feed_updates` - RSS Feed Updates (blog posts + announcements)

### Monitor WorkManager State
```bash
# List all scheduled work
adb shell dumpsys jobscheduler | grep ink.trmnl

# Check WorkManager database
adb shell dumpsys activity service WorkManagerService
```

### Inspect DataStore Preferences
```bash
# View current user preferences
adb shell run-as ink.trmnl.android.buddy cat /data/data/ink.trmnl.android.buddy/files/datastore/user_prefs.preferences_pb
```

### Manual Notification Test (Code)
You can manually trigger notifications from code for UI testing:

```kotlin
// Low battery notification
NotificationHelper.showLowBatteryNotification(
    context = context,
    deviceNames = listOf("Test Device 1", "Test Device 2"),
    thresholdPercent = 20
)

// Blog post notification
NotificationHelper.showBlogPostNotification(
    context = context,
    newPostsCount = 3
)

// Announcement notification
NotificationHelper.showAnnouncementNotification(
    context = context,
    newAnnouncementsCount = 2
)
```

---

## 🐛 Common Issues

### Issue: No RSS feed notifications appear
**Possible causes:**
- No new content in RSS feed (all posts/announcements already seen)
- Dev flags disabled AND user preference disabled
- Network error preventing sync
- RSS feed sync failed

**Fix:**
1. Check logs for sync errors
2. Enable dev flags to bypass user preference
3. Clear app data to reset "read" status
4. Verify network connectivity

### Issue: "Dev flag enabled but still no notification"
**Possible causes:**
- No NEW content detected (unread count didn't increase)
- POST_NOTIFICATIONS permission denied (Android 13+)
- Notification channel disabled in system settings

**Fix:**
1. Clear app data to get fresh content
2. Check permission: `adb shell dumpsys package ink.trmnl.android.buddy | grep POST_NOTIFICATIONS`
3. Check channels: Settings → Apps → TRMNL Buddy → Notifications

### Issue: Worker doesn't run immediately
**Possible causes:**
- Device in Doze mode
- Network constraint not met (no WiFi/data)
- WorkManager quota exhausted
- Battery optimization enabled

**Fix:**
- Disable battery optimization for debug builds
- Ensure network connectivity
- Use "Test Notification Now" button (for low battery)
- Use dev flags with app restart (for RSS feeds)

### Issue: Multiple duplicate notifications
**Possible causes:**
- Dev flags enabled + user preference enabled (double trigger)
- Multiple worker executions (tapping test button multiple times)

**Fix:**
- This is expected behavior when dev flags bypass user preference
- Each notification is independent and intentional

---

## 🎯 Recommended Testing Workflow

**For quick notification testing (Recommended):**

1. Use the **Development Screen** (Settings → Development Tools)
2. Test all notification variations with mock data
3. Adjust parameters (device count, threshold, post count) as needed
4. Verify permission status and request if needed
5. Use worker triggers to test with real API/RSS data

**For production behavior verification:**

1. ✅ **Disable all dev flags** in `AppDevConfig.kt`
2. ✅ Build release-like version: `./gradlew assembleDebug`
3. ✅ Clear app data and reinstall
4. ✅ Login with API token
5. ✅ Enable all notification toggles in Settings
6. ✅ Verify workers are scheduled (check WorkManager Inspector)
7. ✅ Wait for automatic sync OR use Development screen worker triggers
8. ✅ Verify notifications appear (if new content exists)
9. ✅ Disable notification toggles
10. ✅ Verify workers are cancelled
11. ✅ Re-enable toggles
12. ✅ Verify workers reschedule

---

## 📝 Development Config Reference

See `app/src/main/java/ink/trmnl/android/buddy/dev/README.md` for detailed documentation on:

- `ENABLE_ANNOUNCEMENT_NOTIFICATION` - Force announcement notifications
- `ENABLE_BLOG_NOTIFICATION` - Force blog post notifications
- `VERBOSE_NOTIFICATION_LOGGING` - Detailed notification logging

**Production Safety:**
- All flags default to `false`
- Clear warnings in code documentation
- Only affects debug builds (not release)
