# Calendar provider overview

The Calendar Provider is a repository for a user's calendar events. The Calendar Provider API allows you to perform query, insert, update, and delete operations on calendars, events, attendees, reminders, and so on.

The Calendar Provider API can be used by applications and sync adapters. The rules vary depending on what type of program is making the calls. This document focuses primarily on using the Calendar Provider API as an application. For a discussion of how sync adapters are different, see[Sync Adapters](https://developer.android.com/identity/providers/calendar-provider#sync-adapter).

Normally, to read or write calendar data, an application's manifest must include the proper permissions, described in[User Permissions](https://developer.android.com/identity/providers/calendar-provider#manifest). To make performing common operations easier, the Calendar Provider offers a set of intents, as described in[Calendar Intents](https://developer.android.com/identity/providers/calendar-provider#intents). These intents take users to the Calendar application to insert, view, and edit events. The user interacts with the Calendar application and then returns to the original application. Thus your application doesn't need to request permissions, nor does it need to provide a user interface to view or create events.

## Basics

[Content providers](https://developer.android.com/guide/topics/providers/content-providers)store data and make it accessible to applications. The content providers offered by the Android platform (including the Calendar Provider) typically expose data as a set of tables based on a relational database model, where each row is a record and each column is data of a particular type and meaning. Through the Calendar Provider API, applications and sync adapters can get read/write access to the database tables that hold a user's calendar data.

Every content provider exposes a public URI (wrapped as a[Uri](https://developer.android.com/reference/android/net/Uri)object) that uniquely identifies its data set. A content provider that controls multiple data sets (multiple tables) exposes a separate URI for each one. All URIs for providers begin with the string "content://". This identifies the data as being controlled by a content provider. The Calendar Provider defines constants for the URIs for each of its classes (tables). These URIs have the format*<class>*`.CONTENT_URI`. For example,[Events.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract.Events#CONTENT_URI).

Figure 1 shows a graphical representation of the Calendar Provider data model. It shows the main tables and the fields that link them to each other.
![Calendar Provider Data Model](https://developer.android.com/static/images/providers/datamodel.png)

**Figure 1.**Calendar Provider data model.

A user can have multiple calendars, and different calendars can be associated with different types of accounts (Google Calendar, Exchange, and so on).

The[CalendarContract](https://developer.android.com/reference/android/provider/CalendarContract)defines the data model of calendar and event related information. This data is stored in a number of tables, listed below.

|                                                   Table (Class)                                                   |                                                                                                                                                                                                                                                                                     Description                                                                                                                                                                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CalendarContract.Calendars](https://developer.android.com/reference/android/provider/CalendarContract.Calendars) | This table holds the calendar-specific information. Each row in this table contains the details for a single calendar, such as the name, color, sync information, and so on.                                                                                                                                                                                                                                                                                                                                                                                                        |
| [CalendarContract.Events](https://developer.android.com/reference/android/provider/CalendarContract.Events)       | This table holds the event-specific information. Each row in this table has the information for a single event---for example, event title, location, start time, end time, and so on. The event can occur one-time or can recur multiple times. Attendees, reminders, and extended properties are stored in separate tables. They each have an[EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID)that references the[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)in the Events table. |
| [CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances) | This table holds the start and end time for each occurrence of an event. Each row in this table represents a single event occurrence. For one-time events there is a 1:1 mapping of instances to events. For recurring events, multiple rows are automatically generated that correspond to multiple occurrences of that event.                                                                                                                                                                                                                                                     |
| [CalendarContract.Attendees](https://developer.android.com/reference/android/provider/CalendarContract.Attendees) | This table holds the event attendee (guest) information. Each row represents a single guest of an event. It specifies the type of guest and the guest's attendance response for the event.                                                                                                                                                                                                                                                                                                                                                                                          |
| [CalendarContract.Reminders](https://developer.android.com/reference/android/provider/CalendarContract.Reminders) | This table holds the alert/notification data. Each row represents a single alert for an event. An event can have multiple reminders. The maximum number of reminders per event is specified in[MAX_REMINDERS](https://developer.android.com/reference/android/provider/CalendarContract.CalendarColumns#MAX_REMINDERS), which is set by the sync adapter that owns the given calendar. Reminders are specified in minutes before the event and have a method that determines how the user will be alerted.                                                                          |

The Calendar Provider API is designed to be flexible and powerful. At the same time, it's important to provide a good end user experience and protect the integrity of the calendar and its data. To this end, here are some things to keep in mind when using the API:

- **Inserting, updating, and viewing calendar events.** To directly insert, modify, and read events from the Calendar Provider, you need the appropriate[permissions](https://developer.android.com/identity/providers/calendar-provider#manifest). However, if you're not building a full-fledged calendar application or sync adapter, requesting these permissions isn't necessary. You can instead use intents supported by Android's Calendar application to hand off read and write operations to that application. When you use the intents, your application sends users to the Calendar application to perform the desired operation in a pre-filled form. After they're done, they're returned to your application. By designing your application to perform common operations through the Calendar, you provide users with a consistent, robust user interface. This is the recommended approach. For more information, see[Calendar Intents](https://developer.android.com/identity/providers/calendar-provider#intents).
- **Sync adapters.** A sync adapter synchronizes the calendar data on a user's device with another server or data source. In the[CalendarContract.Calendars](https://developer.android.com/reference/android/provider/CalendarContract.Calendars)and[CalendarContract.Events](https://developer.android.com/reference/android/provider/CalendarContract.Events)tables, there are columns that are reserved for the sync adapters to use. The provider and applications should not modify them. In fact, they are not visible unless they are accessed as a sync adapter. For more information about sync adapters, see[Sync Adapters](https://developer.android.com/identity/providers/calendar-provider#sync-adapter).

## User permissions

To read calendar data, an application must include the[READ_CALENDAR](https://developer.android.com/reference/android/Manifest.permission#READ_CALENDAR)permission in its manifest file. It must include the[WRITE_CALENDAR](https://developer.android.com/reference/android/Manifest.permission#WRITE_CALENDAR)permission to delete, insert or update calendar data:  

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"...>
    <uses-sdk android:minSdkVersion="14" />
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.WRITE_CALENDAR" />
    ...
</manifest>
```

## Calendars table

The[CalendarContract.Calendars](https://developer.android.com/reference/android/provider/CalendarContract.Calendars)table contains details for individual calendars. The following Calendars columns are writable by both an application and a[sync adapter](https://developer.android.com/identity/providers/calendar-provider#sync-adapter). For a full list of supported fields, see the[CalendarContract.Calendars](https://developer.android.com/reference/android/provider/CalendarContract.Calendars)reference.

|                                                                 Constant                                                                 |                                                                                                                                                                                                      Description                                                                                                                                                                                                       |
|------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [NAME](https://developer.android.com/reference/android/provider/CalendarContract.Calendars#NAME)                                         | The name of the calendar.                                                                                                                                                                                                                                                                                                                                                                                              |
| [CALENDAR_DISPLAY_NAME](https://developer.android.com/reference/android/provider/CalendarContract.CalendarColumns#CALENDAR_DISPLAY_NAME) | The name of this calendar that is displayed to the user.                                                                                                                                                                                                                                                                                                                                                               |
| [VISIBLE](https://developer.android.com/reference/android/provider/CalendarContract.CalendarColumns#VISIBLE)                             | A boolean indicating whether the calendar is selected to be displayed. A value of 0 indicates that events associated with this calendar should not be shown. A value of 1 indicates that events associated with this calendar should be shown. This value affects the generation of rows in the[CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances)table. |
| [SYNC_EVENTS](https://developer.android.com/reference/android/provider/CalendarContract.CalendarColumns#SYNC_EVENTS)                     | A boolean indicating whether the calendar should be synced and have its events stored on the device. A value of 0 says do not sync this calendar or store its events on the device. A value of 1 says sync events for this calendar and store its events on the device.                                                                                                                                                |

### Include an account type for all operations

If you query on a[Calendars.ACCOUNT_NAME](https://developer.android.com/reference/android/provider/CalendarContract.SyncColumns#ACCOUNT_NAME), you must also include[Calendars.ACCOUNT_TYPE](https://developer.android.com/reference/android/provider/CalendarContract.SyncColumns#ACCOUNT_TYPE)in the selection. That is because a given account is only considered unique given both its`ACCOUNT_NAME`and its`ACCOUNT_TYPE`. The`ACCOUNT_TYPE`is the string corresponding to the account authenticator that was used when the account was registered with the[AccountManager](https://developer.android.com/reference/android/accounts/AccountManager). There is also a special type of account called[ACCOUNT_TYPE_LOCAL](https://developer.android.com/reference/android/provider/CalendarContract#ACCOUNT_TYPE_LOCAL)for calendars not associated with a device account.[ACCOUNT_TYPE_LOCAL](https://developer.android.com/reference/android/provider/CalendarContract#ACCOUNT_TYPE_LOCAL)accounts do not get synced.

### Query a calendar

Here is an example that shows how to get the calendars that are owned by a particular user. For simplicity's sake, in this example the query operation is shown in the user interface thread ("main thread"). In practice, this should be done in an asynchronous thread instead of on the main thread. For more discussion, see[Loaders](https://developer.android.com/guide/components/loaders). If you are not just reading data but modifying it, see[AsyncQueryHandler](https://developer.android.com/reference/android/content/AsyncQueryHandler).  

### Kotlin

```kotlin
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,                     // 0
        CalendarContract.Calendars.ACCOUNT_NAME,            // 1
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
        CalendarContract.Calendars.OWNER_ACCOUNT            // 3
)

// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
private const val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
private const val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3
```

### Java

```java
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
public static final String[] EVENT_PROJECTION = new String[] {
    Calendars._ID,                           // 0
    Calendars.ACCOUNT_NAME,                  // 1
    Calendars.CALENDAR_DISPLAY_NAME,         // 2
    Calendars.OWNER_ACCOUNT                  // 3
};

// The indices for the projection array above.
private static final int PROJECTION_ID_INDEX = 0;
private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
```

In the next part of the example, you construct your query. The selection specifies the criteria for the query. In this example the query is looking for calendars that have the`ACCOUNT_NAME`"hera@example.com", the`ACCOUNT_TYPE`"com.example", and the`OWNER_ACCOUNT`"hera@example.com". If you want to see all calendars that a user has viewed, not just calendars the user owns, omit the`OWNER_ACCOUNT`. The query returns a[Cursor](https://developer.android.com/reference/android/database/Cursor)object that you can use to traverse the result set returned by the database query. For more discussion of using queries in content providers, see[Content Providers](https://developer.android.com/guide/topics/providers/content-providers).  

### Kotlin

```kotlin
// Run query
val uri: Uri = CalendarContract.Calendars.CONTENT_URI
val selection: String = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (" +
        "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?) AND (" +
        "${CalendarContract.Calendars.OWNER_ACCOUNT} = ?))"
val selectionArgs: Array<String> = arrayOf("hera@example.com", "com.example", "hera@example.com")
val cur: Cursor = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)
```

### Java

```java
// Run query
Cursor cur = null;
ContentResolver cr = getContentResolver();
Uri uri = Calendars.CONTENT_URI;
String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND ("
                        + Calendars.ACCOUNT_TYPE + " = ?) AND ("
                        + Calendars.OWNER_ACCOUNT + " = ?))";
String[] selectionArgs = new String[] {"hera@example.com", "com.example",
        "hera@example.com"};
// Submit the query and get a Cursor object back.
cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
```

This next section uses the cursor to step through the result set. It uses the constants that were set up at the beginning of the example to return the values for each field.  

### Kotlin

```kotlin
// Use the cursor to step through the returned records
while (cur.moveToNext()) {
    // Get the field values
    val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
    val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
    val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
    val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
    // Do something with the values...
}
```

### Java

```java
// Use the cursor to step through the returned records
while (cur.moveToNext()) {
    long calID = 0;
    String displayName = null;
    String accountName = null;
    String ownerName = null;

    // Get the field values
    calID = cur.getLong(PROJECTION_ID_INDEX);
    displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
    accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
    ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

    // Do something with the values...

   ...
}
```

### Modify a calendar

To perform an update of a calendar, you can provide the[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)of the calendar either as an appended ID to the Uri ([withAppendedId()](https://developer.android.com/reference/android/content/ContentUris#withAppendedId(android.net.Uri, long))) or as the first selection item. The selection should start with`"_id=?"`, and the first`selectionArg`should be the[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)of the calendar. You can also do updates by encoding the ID in the URI. This example changes a calendar's display name using the ([withAppendedId()](https://developer.android.com/reference/android/content/ContentUris#withAppendedId(android.net.Uri, long))) approach:  

### Kotlin

```kotlin
const val DEBUG_TAG: String = "MyActivity"
...
val calID: Long = 2
val values = ContentValues().apply {
    // The new display name for the calendar
    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Trevor's Calendar")
}
val updateUri: Uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calID)
val rows: Int = contentResolver.update(updateUri, values, null, null)
Log.i(DEBUG_TAG, "Rows updated: $rows")
```

### Java

```java
private static final String DEBUG_TAG = "MyActivity";
...
long calID = 2;
ContentValues values = new ContentValues();
// The new display name for the calendar
values.put(Calendars.CALENDAR_DISPLAY_NAME, "Trevor's Calendar");
Uri updateUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calID);
int rows = getContentResolver().update(updateUri, values, null, null);
Log.i(DEBUG_TAG, "Rows updated: " + rows);
```

### Insert a calendar

Calendars are designed to be primarily managed by a sync adapter, so you should only insert new calendars as a sync adapter. For the most part, applications can only make superficial changes to calendars, such as changing the display name. If an application needs to create a local calendar, it can do this by performing the calendar insertion as a sync adapter, using an[ACCOUNT_TYPE](https://developer.android.com/reference/android/provider/CalendarContract.SyncColumns#ACCOUNT_TYPE)of[ACCOUNT_TYPE_LOCAL](https://developer.android.com/reference/android/provider/CalendarContract#ACCOUNT_TYPE_LOCAL).[ACCOUNT_TYPE_LOCAL](https://developer.android.com/reference/android/provider/CalendarContract#ACCOUNT_TYPE_LOCAL)is a special account type for calendars that are not associated with a device account. Calendars of this type are not synced to a server. For a discussion of sync adapters, see[Sync Adapters](https://developer.android.com/identity/providers/calendar-provider#sync-adapter).

## Events table

The[CalendarContract.Events](https://developer.android.com/reference/android/provider/CalendarContract.Events)table contains details for individual events. To add, update, or delete events, an application must include the[WRITE_CALENDAR](https://developer.android.com/reference/android/Manifest.permission#WRITE_CALENDAR)permission in its[manifest file](https://developer.android.com/identity/providers/calendar-provider#manifest).

The following Events columns are writable by both an application and a sync adapter. For a full list of supported fields, see the[CalendarContract.Events](https://developer.android.com/reference/android/provider/CalendarContract.Events)reference.

|                                                                   Constant                                                                   |                                                                                                                                                                                                               Description                                                                                                                                                                                                                |
|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CALENDAR_ID](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#CALENDAR_ID)                           | The[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)of the calendar the event belongs to.                                                                                                                                                                                                                                                                                                                  |
| [ORGANIZER](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#ORGANIZER)                               | Email of the organizer (owner) of the event.                                                                                                                                                                                                                                                                                                                                                                                             |
| [TITLE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#TITLE)                                       | The title of the event.                                                                                                                                                                                                                                                                                                                                                                                                                  |
| [EVENT_LOCATION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#EVENT_LOCATION)                     | Where the event takes place.                                                                                                                                                                                                                                                                                                                                                                                                             |
| [DESCRIPTION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DESCRIPTION)                           | The description of the event.                                                                                                                                                                                                                                                                                                                                                                                                            |
| [DTSTART](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTSTART)                                   | The time the event starts in UTC milliseconds since the epoch.                                                                                                                                                                                                                                                                                                                                                                           |
| [DTEND](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTEND)                                       | The time the event ends in UTC milliseconds since the epoch.                                                                                                                                                                                                                                                                                                                                                                             |
| [EVENT_TIMEZONE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#EVENT_TIMEZONE)                     | The time zone for the event.                                                                                                                                                                                                                                                                                                                                                                                                             |
| [EVENT_END_TIMEZONE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#EVENT_END_TIMEZONE)             | The time zone for the end time of the event.                                                                                                                                                                                                                                                                                                                                                                                             |
| [DURATION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DURATION)                                 | The duration of the event in[RFC5545](http://tools.ietf.org/html/rfc5545#section-3.8.2.5)format. For example, a value of`"PT1H"`states that the event should last one hour, and a value of`"P2W"`indicates a duration of 2 weeks.                                                                                                                                                                                                        |
| [ALL_DAY](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#ALL_DAY)                                   | A value of 1 indicates this event occupies the entire day, as defined by the local time zone. A value of 0 indicates it is a regular event that may start and end at any time during a day.                                                                                                                                                                                                                                              |
| [RRULE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RRULE)                                       | The recurrence rule for the event format. For example,`"FREQ=WEEKLY;COUNT=10;WKST=SU"`. You can find more examples[here](http://tools.ietf.org/html/rfc5545#section-3.8.5.3).                                                                                                                                                                                                                                                            |
| [RDATE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RDATE)                                       | The recurrence dates for the event. You typically use[RDATE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RDATE)in conjunction with[RRULE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RRULE)to define an aggregate set of repeating occurrences. For more discussion, see the[RFC5545 spec](http://tools.ietf.org/html/rfc5545#section-3.8.5.2). |
| [AVAILABILITY](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#AVAILABILITY)                         | If this event counts as busy time or is free time that can be scheduled over.                                                                                                                                                                                                                                                                                                                                                            |
| [GUESTS_CAN_MODIFY](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#GUESTS_CAN_MODIFY)               | Whether guests can modify the event.                                                                                                                                                                                                                                                                                                                                                                                                     |
| [GUESTS_CAN_INVITE_OTHERS](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#GUESTS_CAN_INVITE_OTHERS) | Whether guests can invite other guests.                                                                                                                                                                                                                                                                                                                                                                                                  |
| [GUESTS_CAN_SEE_GUESTS](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#GUESTS_CAN_SEE_GUESTS)       | Whether guests can see the list of attendees.                                                                                                                                                                                                                                                                                                                                                                                            |

### Add events

When your application inserts a new event, we recommend that you use an[INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT)Intent, as described in[Using an intent to insert an event](https://developer.android.com/identity/providers/calendar-provider#intent-insert). However, if you need to, you can insert events directly. This section describes how to do this.

Here are the rules for inserting a new event:

- You must include[CALENDAR_ID](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#CALENDAR_ID)and[DTSTART](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTSTART).
- You must include an[EVENT_TIMEZONE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#EVENT_TIMEZONE). To get a list of the system's installed time zone IDs, use[getAvailableIDs()](https://developer.android.com/reference/java/util/TimeZone#getAvailableIDs()). Note that this rule does not apply if you're inserting an event through the[INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT)Intent, described in[Using an intent to insert an event](https://developer.android.com/identity/providers/calendar-provider#intent-insert)---in that scenario, a default time zone is supplied.
- For non-recurring events, you must include[DTEND](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTEND).
- For recurring events, you must include a[DURATION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DURATION)in addition to[RRULE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RRULE)or[RDATE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RDATE). Note that this rule does not apply if you're inserting an event through the[INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT)Intent, described in[Using an intent to insert an event](https://developer.android.com/identity/providers/calendar-provider#intent-insert)---in that scenario, you can use an[RRULE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RRULE)in conjunction with[DTSTART](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTSTART)and[DTEND](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DTEND), and the Calendar application converts it to a duration automatically.

Here is an example of inserting an event. This is being performed in the UI thread for simplicity. In practice, inserts and updates should be done in an asynchronous thread to move the action into a background thread. For more information, see[AsyncQueryHandler](https://developer.android.com/reference/android/content/AsyncQueryHandler).  

### Kotlin

```kotlin
val calID: Long = 3
val startMillis: Long = Calendar.getInstance().run {
    set(2012, 9, 14, 7, 30)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2012, 9, 14, 8, 45)
    timeInMillis
}
...

val values = ContentValues().apply {
    put(CalendarContract.Events.DTSTART, startMillis)
    put(CalendarContract.Events.DTEND, endMillis)
    put(CalendarContract.Events.TITLE, "Jazzercise")
    put(CalendarContract.Events.DESCRIPTION, "Group workout")
    put(CalendarContract.Events.CALENDAR_ID, calID)
    put(CalendarContract.Events.EVENT_TIMEZONE, "America/Los_Angeles")
}
val uri: Uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

// get the event ID that is the last element in the Uri
val eventID: Long = uri.lastPathSegment.toLong()
//
// ... do something with event ID
//
//
```

### Java

```java
long calID = 3;
long startMillis = 0;
long endMillis = 0;
Calendar beginTime = Calendar.getInstance();
beginTime.set(2012, 9, 14, 7, 30);
startMillis = beginTime.getTimeInMillis();
Calendar endTime = Calendar.getInstance();
endTime.set(2012, 9, 14, 8, 45);
endMillis = endTime.getTimeInMillis();
...

ContentResolver cr = getContentResolver();
ContentValues values = new ContentValues();
values.put(Events.DTSTART, startMillis);
values.put(Events.DTEND, endMillis);
values.put(Events.TITLE, "Jazzercise");
values.put(Events.DESCRIPTION, "Group workout");
values.put(Events.CALENDAR_ID, calID);
values.put(Events.EVENT_TIMEZONE, "America/Los_Angeles");
Uri uri = cr.insert(Events.CONTENT_URI, values);

// get the event ID that is the last element in the Uri
long eventID = Long.parseLong(uri.getLastPathSegment());
//
// ... do something with event ID
//
//
```

**Note:**See how this example captures the event ID after the event is created. This is the easiest way to get an event ID. You often need the event ID to perform other calendar operations---for example, to add attendees or reminders to an event.

### Update events

When your application wants to allow the user to edit an event, we recommend that you use an[EDIT](https://developer.android.com/reference/android/content/Intent#ACTION_EDIT)Intent, as described in[Using an intent to edit an event](https://developer.android.com/identity/providers/calendar-provider#intent-edit). However, if you need to, you can edit events directly. To perform an update of an Event, you can provide the`_ID`of the event either as an appended ID to the Uri ([withAppendedId()](https://developer.android.com/reference/android/content/ContentUris#withAppendedId(android.net.Uri, long))) or as the first selection item. The selection should start with`"_id=?"`, and the first`selectionArg`should be the`_ID`of the event. You can also do updates using a selection with no ID. Here is an example of updating an event. It changes the title of the event using the[withAppendedId()](https://developer.android.com/reference/android/content/ContentUris#withAppendedId(android.net.Uri, long))approach:  

### Kotlin

```kotlin
val DEBUG_TAG = "MyActivity"
...
val eventID: Long = 188
...
val values = ContentValues().apply {
    // The new title for the event
    put(CalendarContract.Events.TITLE, "Kickboxing")
}
val updateUri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val rows: Int = contentResolver.update(updateUri, values, null, null)
Log.i(DEBUG_TAG, "Rows updated: $rows")
```

### Java

```java
private static final String DEBUG_TAG = "MyActivity";
...
long eventID = 188;
...
ContentResolver cr = getContentResolver();
ContentValues values = new ContentValues();
Uri updateUri = null;
// The new title for the event
values.put(Events.TITLE, "Kickboxing");
updateUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventID);
int rows = cr.update(updateUri, values, null, null);
Log.i(DEBUG_TAG, "Rows updated: " + rows);
```

### Delete events

You can delete an event either by its[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)as an appended ID on the URI, or by using standard selection. If you use an appended ID, you can't also do a selection. There are two versions of delete: as an application and as a sync adapter. An application delete sets the*deleted* column to 1. This flag that tells the sync adapter that the row was deleted and that this deletion should be propagated to the server. A sync adapter delete removes the event from the database along with all its associated data. Here is an example of application deleting an event through its[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID):  

### Kotlin

```kotlin
val DEBUG_TAG = "MyActivity"
...
val eventID: Long = 201
...
val deleteUri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val rows: Int = contentResolver.delete(deleteUri, null, null)
Log.i(DEBUG_TAG, "Rows deleted: $rows")
```

### Java

```java
private static final String DEBUG_TAG = "MyActivity";
...
long eventID = 201;
...
ContentResolver cr = getContentResolver();
Uri deleteUri = null;
deleteUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventID);
int rows = cr.delete(deleteUri, null, null);
Log.i(DEBUG_TAG, "Rows deleted: " + rows);
```

## Attendees table

Each row of the[CalendarContract.Attendees](https://developer.android.com/reference/android/provider/CalendarContract.Attendees)table represents a single attendee or guest of an event. Calling[query()](https://developer.android.com/reference/android/provider/CalendarContract.Reminders#query(android.content.ContentResolver, long, java.lang.String[]))returns a list of attendees for the event with the given[EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID). This[EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID)must match the[_ID](https://developer.android.com/reference/android/provider/BaseColumns#_ID)of a particular event.

The following table lists the writable fields. When inserting a new attendee, you must include all of them except`ATTENDEE_NAME`.

|                                                                 Constant                                                                  |                                                                                                                                                                                                                                                                                                                                                                                           Description                                                                                                                                                                                                                                                                                                                                                                                            |
|-------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID)                           | The ID of the event.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| [ATTENDEE_NAME](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_NAME)                 | The name of the attendee.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| [ATTENDEE_EMAIL](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_EMAIL)               | The email address of the attendee.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| [ATTENDEE_RELATIONSHIP](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_RELATIONSHIP) | The relationship of the attendee to the event. One of: - [RELATIONSHIP_ATTENDEE](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#RELATIONSHIP_ATTENDEE) - [RELATIONSHIP_NONE](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#RELATIONSHIP_NONE) - [RELATIONSHIP_ORGANIZER](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#RELATIONSHIP_ORGANIZER) - [RELATIONSHIP_PERFORMER](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#RELATIONSHIP_PERFORMER) - [RELATIONSHIP_SPEAKER](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#RELATIONSHIP_SPEAKER)                     |
| [ATTENDEE_TYPE](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_TYPE)                 | The type of attendee. One of: - [TYPE_REQUIRED](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#TYPE_REQUIRED) - [TYPE_OPTIONAL](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#TYPE_OPTIONAL)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| [ATTENDEE_STATUS](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS)             | The attendance status of the attendee. One of: - [ATTENDEE_STATUS_ACCEPTED](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS_ACCEPTED) - [ATTENDEE_STATUS_DECLINED](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS_DECLINED) - [ATTENDEE_STATUS_INVITED](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS_INVITED) - [ATTENDEE_STATUS_NONE](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS_NONE) - [ATTENDEE_STATUS_TENTATIVE](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#ATTENDEE_STATUS_TENTATIVE) |

### Add attendees

Here is an example that adds a single attendee to an event. Note that the[EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID)is required:  

### Kotlin

```kotlin
val eventID: Long = 202
...
val values = ContentValues().apply {
    put(CalendarContract.Attendees.ATTENDEE_NAME, "Trevor")
    put(CalendarContract.Attendees.ATTENDEE_EMAIL, "trevor@example.com")
    put(
        CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
        CalendarContract.Attendees.RELATIONSHIP_ATTENDEE
    )
    put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_OPTIONAL)
    put(
        CalendarContract.Attendees.ATTENDEE_STATUS,
        CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
    )
    put(CalendarContract.Attendees.EVENT_ID, eventID)
}
val uri: Uri = contentResolver.insert(CalendarContract.Attendees.CONTENT_URI, values)
```

### Java

```java
long eventID = 202;
...
ContentResolver cr = getContentResolver();
ContentValues values = new ContentValues();
values.put(Attendees.ATTENDEE_NAME, "Trevor");
values.put(Attendees.ATTENDEE_EMAIL, "trevor@example.com");
values.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ATTENDEE);
values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_OPTIONAL);
values.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);
values.put(Attendees.EVENT_ID, eventID);
Uri uri = cr.insert(Attendees.CONTENT_URI, values);
```

## Reminders table

Each row of the[CalendarContract.Reminders](https://developer.android.com/reference/android/provider/CalendarContract.Reminders)table represents a single reminder for an event. Calling[query()](https://developer.android.com/reference/android/provider/CalendarContract.Reminders#query(android.content.ContentResolver, long, java.lang.String[]))returns a list of reminders for the event with the given[EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.AttendeesColumns#EVENT_ID).

The following table lists the writable fields for reminders. All of them must be included when inserting a new reminder. Note that sync adapters specify the types of reminders they support in the[CalendarContract.Calendars](https://developer.android.com/reference/android/provider/CalendarContract.Calendars)table. See[ALLOWED_REMINDERS](https://developer.android.com/reference/android/provider/CalendarContract.CalendarColumns#ALLOWED_REMINDERS)for details.

|                                                    Constant                                                     |                                                                                                                                                                                                                                                                       Description                                                                                                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#EVENT_ID) | The ID of the event.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| [MINUTES](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#MINUTES)   | The minutes prior to the event that the reminder should fire.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| [METHOD](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#METHOD)     | The alarm method, as set on the server. One of: - [METHOD_ALERT](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#METHOD_ALERT) - [METHOD_DEFAULT](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#METHOD_DEFAULT) - [METHOD_EMAIL](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#METHOD_EMAIL) - [METHOD_SMS](https://developer.android.com/reference/android/provider/CalendarContract.RemindersColumns#METHOD_SMS) |

### Add reminders

This example adds a reminder to an event. The reminder fires 15 minutes before the event.  

### Kotlin

```kotlin
val eventID: Long = 221
...
val values = ContentValues().apply {
    put(CalendarContract.Reminders.MINUTES, 15)
    put(CalendarContract.Reminders.EVENT_ID, eventID)
    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
}
val uri: Uri = contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
```

### Java

```java
long eventID = 221;
...
ContentResolver cr = getContentResolver();
ContentValues values = new ContentValues();
values.put(Reminders.MINUTES, 15);
values.put(Reminders.EVENT_ID, eventID);
values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
Uri uri = cr.insert(Reminders.CONTENT_URI, values);
```

## Instances table

The[CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances)table holds the start and end time for occurrences of an event. Each row in this table represents a single event occurrence. The instances table is not writable and only provides a way to query event occurrences.

The following table lists some of the fields you can query on for an instance. Note that time zone is defined by[KEY_TIMEZONE_TYPE](https://developer.android.com/reference/android/provider/CalendarContract.CalendarCache#KEY_TIMEZONE_TYPE)and[KEY_TIMEZONE_INSTANCES](https://developer.android.com/reference/android/provider/CalendarContract.CalendarCache#KEY_TIMEZONE_INSTANCES).

|                                                     Constant                                                     |                                          Description                                           |
|------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| [BEGIN](https://developer.android.com/reference/android/provider/CalendarContract.Instances#BEGIN)               | The beginning time of the instance, in UTC milliseconds.                                       |
| [END](https://developer.android.com/reference/android/provider/CalendarContract.Instances#END)                   | The ending time of the instance, in UTC milliseconds.                                          |
| [END_DAY](https://developer.android.com/reference/android/provider/CalendarContract.Instances#END_DAY)           | The Julian end day of the instance, relative to the Calendar's time zone.                      |
| [END_MINUTE](https://developer.android.com/reference/android/provider/CalendarContract.Instances#END_MINUTE)     | The end minute of the instance measured from midnight in the Calendar's time zone.             |
| [EVENT_ID](https://developer.android.com/reference/android/provider/CalendarContract.Instances#EVENT_ID)         | The`_ID`of the event for this instance.                                                        |
| [START_DAY](https://developer.android.com/reference/android/provider/CalendarContract.Instances#START_DAY)       | The Julian start day of the instance, relative to the Calendar's time zone.                    |
| [START_MINUTE](https://developer.android.com/reference/android/provider/CalendarContract.Instances#START_MINUTE) | The start minute of the instance measured from midnight, relative to the Calendar's time zone. |

### Query the instances table

To query the Instances table, you need to specify a range time for the query in the URI. In this example,[CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances)gets access to the[TITLE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#TITLE)field through its implementation of the[CalendarContract.EventsColumns](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns)interface. In other words,[TITLE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#TITLE)is returned through a database view, not through querying the raw[CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances)table.  

### Kotlin

```kotlin
const val DEBUG_TAG: String = "MyActivity"
val INSTANCE_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Instances.EVENT_ID, // 0
        CalendarContract.Instances.BEGIN, // 1
        CalendarContract.Instances.TITLE // 2
)

// The indices for the projection array above.
const val PROJECTION_ID_INDEX: Int = 0
const val PROJECTION_BEGIN_INDEX: Int = 1
const val PROJECTION_TITLE_INDEX: Int = 2

// Specify the date range you want to search for recurring
// event instances
val startMillis: Long = Calendar.getInstance().run {
    set(2011, 9, 23, 8, 0)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2011, 10, 24, 8, 0)
    timeInMillis
}

// The ID of the recurring event whose instances you are searching
// for in the Instances table
val selection: String = "${CalendarContract.Instances.EVENT_ID} = ?"
val selectionArgs: Array<String> = arrayOf("207")

// Construct the query with the desired date range.
val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
ContentUris.appendId(builder, startMillis)
ContentUris.appendId(builder, endMillis)

// Submit the query
val cur: Cursor = contentResolver.query(
        builder.build(),
        INSTANCE_PROJECTION,
        selection,
        selectionArgs, null
)
while (cur.moveToNext()) {
    // Get the field values
    val eventID: Long = cur.getLong(PROJECTION_ID_INDEX)
    val beginVal: Long = cur.getLong(PROJECTION_BEGIN_INDEX)
    val title: String = cur.getString(PROJECTION_TITLE_INDEX)

    // Do something with the values.
    Log.i(DEBUG_TAG, "Event: $title")
    val calendar = Calendar.getInstance().apply {
        timeInMillis = beginVal
    }
    val formatter = SimpleDateFormat("MM/dd/yyyy")
    Log.i(DEBUG_TAG, "Date: ${formatter.format(calendar.time)}")
}
```

### Java

```java
private static final String DEBUG_TAG = "MyActivity";
public static final String[] INSTANCE_PROJECTION = new String[] {
    Instances.EVENT_ID,      // 0
    Instances.BEGIN,         // 1
    Instances.TITLE          // 2
  };

// The indices for the projection array above.
private static final int PROJECTION_ID_INDEX = 0;
private static final int PROJECTION_BEGIN_INDEX = 1;
private static final int PROJECTION_TITLE_INDEX = 2;
...

// Specify the date range you want to search for recurring
// event instances
Calendar beginTime = Calendar.getInstance();
beginTime.set(2011, 9, 23, 8, 0);
long startMillis = beginTime.getTimeInMillis();
Calendar endTime = Calendar.getInstance();
endTime.set(2011, 10, 24, 8, 0);
long endMillis = endTime.getTimeInMillis();

Cursor cur = null;
ContentResolver cr = getContentResolver();

// The ID of the recurring event whose instances you are searching
// for in the Instances table
String selection = Instances.EVENT_ID + " = ?";
String[] selectionArgs = new String[] {"207"};

// Construct the query with the desired date range.
Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
ContentUris.appendId(builder, startMillis);
ContentUris.appendId(builder, endMillis);

// Submit the query
cur =  cr.query(builder.build(),
    INSTANCE_PROJECTION,
    selection,
    selectionArgs,
    null);

while (cur.moveToNext()) {
    String title = null;
    long eventID = 0;
    long beginVal = 0;

    // Get the field values
    eventID = cur.getLong(PROJECTION_ID_INDEX);
    beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
    title = cur.getString(PROJECTION_TITLE_INDEX);

    // Do something with the values.
    Log.i(DEBUG_TAG, "Event:  " + title);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(beginVal);
    DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    Log.i(DEBUG_TAG, "Date: " + formatter.format(calendar.getTime()));
    }
 }
```

## Calendar intents

Your application doesn't need[permissions](https://developer.android.com/identity/providers/calendar-provider#manifest)to read and write calendar data. It can instead use intents supported by Android's Calendar application to hand off read and write operations to that application. The following table lists the intents supported by the Calendar Provider:

|                                                                                  Action                                                                                   |                                                                                                                                                                                        URI                                                                                                                                                                                         |                        Description                        |                                                                                                                                           Extras                                                                                                                                           |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [VIEW](https://developer.android.com/reference/android/content/Intent#ACTION_VIEW)                                                                                        | `content://com.android.calendar/time/<ms_since_epoch>` You can also refer to the URI with[CalendarContract.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract#CONTENT_URI). For an example of using this intent, see[Using intents to view calendar data](https://developer.android.com/guide/topics/providers/calendar-provider#intent-view). | Open calendar to the time specified by`<ms_since_epoch>`. | None.                                                                                                                                                                                                                                                                                      |
| [VIEW](https://developer.android.com/reference/android/content/Intent#ACTION_VIEW)                                                                                        | `content://com.android.calendar/events/<event_id>` You can also refer to the URI with[Events.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract.Events#CONTENT_URI). For an example of using this intent, see[Using intents to view calendar data](https://developer.android.com/guide/topics/providers/calendar-provider#intent-view).        | View the event specified by`<event_id>`.                  | [CalendarContract.EXTRA_EVENT_BEGIN_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_BEGIN_TIME) <br /> [CalendarContract.EXTRA_EVENT_END_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_END_TIME) |
| [EDIT](https://developer.android.com/reference/android/content/Intent#ACTION_EDIT)                                                                                        | `content://com.android.calendar/events/<event_id>` You can also refer to the URI with[Events.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract.Events#CONTENT_URI). For an example of using this intent, see[Using an intent to edit an event](https://developer.android.com/guide/topics/providers/calendar-provider#intent-edit).           | Edit the event specified by`<event_id>`.                  | [CalendarContract.EXTRA_EVENT_BEGIN_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_BEGIN_TIME) <br /> [CalendarContract.EXTRA_EVENT_END_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_END_TIME) |
| [EDIT](https://developer.android.com/reference/android/content/Intent#ACTION_EDIT) [INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT) | `content://com.android.calendar/events` You can also refer to the URI with[Events.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract.Events#CONTENT_URI). For an example of using this intent, see[Using an intent to insert an event](https://developer.android.com/guide/topics/providers/calendar-provider#intent-insert).                  | Create an event.                                          | Any of the extras listed in the table below.                                                                                                                                                                                                                                               |

The following table lists the intent extras supported by the Calendar Provider:

|                                                                Intent Extra                                                                 |                                   Description                                   |
|---------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| [Events.TITLE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#TITLE)                               | Name for the event.                                                             |
| [CalendarContract.EXTRA_EVENT_BEGIN_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_BEGIN_TIME) | Event begin time in milliseconds from the epoch.                                |
| [CalendarContract.EXTRA_EVENT_END_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_END_TIME)     | Event end time in milliseconds from the epoch.                                  |
| [CalendarContract.EXTRA_EVENT_ALL_DAY](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_ALL_DAY)       | A boolean that indicates that an event is all day. Value can be`true`or`false`. |
| [Events.EVENT_LOCATION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#EVENT_LOCATION)             | Location of the event.                                                          |
| [Events.DESCRIPTION](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#DESCRIPTION)                   | Event description.                                                              |
| [Intent.EXTRA_EMAIL](https://developer.android.com/reference/android/content/Intent#EXTRA_EMAIL)                                            | Email addresses of those to invite as a comma-separated list.                   |
| [Events.RRULE](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#RRULE)                               | The recurrence rule for the event.                                              |
| [Events.ACCESS_LEVEL](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#ACCESS_LEVEL)                 | Whether the event is private or public.                                         |
| [Events.AVAILABILITY](https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns#AVAILABILITY)                 | If this event counts as busy time or is free time that can be scheduled over.   |

The following sections describe how to use these intents.

### Use an intent to insert an event

Using the[INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT)Intent lets your application hand off the event insertion task to the Calendar itself. With this approach, your application doesn't even need to have the[WRITE_CALENDAR](https://developer.android.com/reference/android/Manifest.permission#WRITE_CALENDAR)permission included in its[manifest file](https://developer.android.com/identity/providers/calendar-provider#manifest).

When users run an application that uses this approach, the application sends them to the Calendar to finish adding the event. The[INSERT](https://developer.android.com/reference/android/content/Intent#ACTION_INSERT)Intent uses extra fields to pre-populate a form with the details of the event in the Calendar. Users can then cancel the event, edit the form as needed, or save the event to their calendars.

Here is a code snippet that schedules an event on January 19, 2012, that runs from 7:30 a.m. to 8:30 a.m. Note the following about this code snippet:

- It specifies[Events.CONTENT_URI](https://developer.android.com/reference/android/provider/CalendarContract.Events#CONTENT_URI)as the Uri.
- It uses the[CalendarContract.EXTRA_EVENT_BEGIN_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_BEGIN_TIME)and[CalendarContract.EXTRA_EVENT_END_TIME](https://developer.android.com/reference/android/provider/CalendarContract#EXTRA_EVENT_END_TIME)extra fields to pre-populate the form with the time of the event. The values for these times must be in UTC milliseconds from the epoch.
- It uses the[Intent.EXTRA_EMAIL](https://developer.android.com/reference/android/content/Intent#EXTRA_EMAIL)extra field to provide a comma-separated list of invitees, specified by email address.

### Kotlin

```kotlin
val startMillis: Long = Calendar.getInstance().run {
    set(2012, 0, 19, 7, 30)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2012, 0, 19, 8, 30)
    timeInMillis
}
val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, "Yoga")
        .putExtra(CalendarContract.Events.DESCRIPTION, "Group class")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, "The gym")
        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com")
startActivity(intent)
```

### Java

```java
Calendar beginTime = Calendar.getInstance();
beginTime.set(2012, 0, 19, 7, 30);
Calendar endTime = Calendar.getInstance();
endTime.set(2012, 0, 19, 8, 30);
Intent intent = new Intent(Intent.ACTION_INSERT)
        .setData(Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
        .putExtra(Events.TITLE, "Yoga")
        .putExtra(Events.DESCRIPTION, "Group class")
        .putExtra(Events.EVENT_LOCATION, "The gym")
        .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
        .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com");
startActivity(intent);
```

### Use an intent to edit an event

You can update an event directly, as described in[Updating events](https://developer.android.com/identity/providers/calendar-provider#update-event). But using the[EDIT](https://developer.android.com/reference/android/content/Intent#ACTION_EDIT)Intent allows an application that doesn't have permission to hand off event editing to the Calendar application. When users finish editing their event in Calendar, they're returned to the original application.

Here is an example of an intent that sets a new title for a specified event and lets users edit the event in the Calendar.  

### Kotlin

```kotlin
val eventID: Long = 208
val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val intent = Intent(Intent.ACTION_EDIT)
        .setData(uri)
        .putExtra(CalendarContract.Events.TITLE, "My New Title")
startActivity(intent)
```

### Java

```java
long eventID = 208;
Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventID);
Intent intent = new Intent(Intent.ACTION_EDIT)
    .setData(uri)
    .putExtra(Events.TITLE, "My New Title");
startActivity(intent);
```

### Use intents to view calendar data

Calendar Provider offers two different ways to use the[VIEW](https://developer.android.com/reference/android/content/Intent#ACTION_VIEW)Intent:

- To open the Calendar to a particular date.
- To view an event.

Here is an example that shows how to open the Calendar to a particular date:  

### Kotlin

```kotlin
val startMillis: Long
...
val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
ContentUris.appendId(builder, startMillis)
val intent = Intent(Intent.ACTION_VIEW)
        .setData(builder.build())
startActivity(intent)
```

### Java

```java
// A date-time specified in milliseconds since the epoch.
long startMillis;
...
Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
builder.appendPath("time");
ContentUris.appendId(builder, startMillis);
Intent intent = new Intent(Intent.ACTION_VIEW)
    .setData(builder.build());
startActivity(intent);
```

Here is an example that shows how to open an event for viewing:  

### Kotlin

```kotlin
val eventID: Long = 208
...
val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val intent = Intent(Intent.ACTION_VIEW).setData(uri)
startActivity(intent)
```

### Java

```java
long eventID = 208;
...
Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventID);
Intent intent = new Intent(Intent.ACTION_VIEW)
   .setData(uri);
startActivity(intent);
```

## Sync adapters

There are only minor differences in how an application and a sync adapter access the Calendar Provider:

- A sync adapter needs to specify that it's a sync adapter by setting[CALLER_IS_SYNCADAPTER](https://developer.android.com/reference/android/provider/CalendarContract#CALLER_IS_SYNCADAPTER)to`true`.
- A sync adapter needs to provide an[ACCOUNT_NAME](https://developer.android.com/reference/android/provider/CalendarContract.SyncColumns#ACCOUNT_NAME)and an[ACCOUNT_TYPE](https://developer.android.com/reference/android/provider/CalendarContract.SyncColumns#ACCOUNT_TYPE)as query parameters in the URI.
- A sync adapter has write access to more columns than an application or widget. For example, an application can only modify a few characteristics of a calendar, such as its name, display name, visibility setting, and whether the calendar is synced. By comparison, a sync adapter can access not only those columns, but many others, such as calendar color, time zone, access level, location, and so on. However, a sync adapter is restricted to the`ACCOUNT_NAME`and`ACCOUNT_TYPE`it specified.

Here is a helper method you can use to return a URI for use with a sync adapter:  

### Kotlin

```kotlin
fun asSyncAdapter(uri: Uri, account: String, accountType: String): Uri {
    return uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType).build()
}
```

### Java

```java
static Uri asSyncAdapter(Uri uri, String account, String accountType) {
    return uri.buildUpon()
        .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,"true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
 }
```