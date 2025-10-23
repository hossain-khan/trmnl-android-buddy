# Testing Low Battery Notifications

This document explains how to test the low battery notification feature during development without waiting for weekly checks or actual low battery conditions.

## 🧪 Testing Strategies

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

## 📝 Notes

- The debug button only appears when `BuildConfig.DEBUG == true`
- The button is only visible when low battery notifications are enabled
- Each tap enqueues a new immediate work request
- Multiple taps will create multiple work executions (they run serially)
- Test button does NOT affect the scheduled weekly worker
