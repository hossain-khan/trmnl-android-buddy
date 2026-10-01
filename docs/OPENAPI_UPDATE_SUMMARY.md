# TRMNL OpenAPI Schema Update Summary (October 2026)

This document provides a comprehensive summary of the OpenAPI schema changes introduced in `api/resources/trmnl-open-api.yaml` comparing the updated snapshot with the previous version.

---

## 1. High-Level Metrics & Overview

| Metric | Previous Snapshot | New Snapshot | Delta |
| :--- | :--- | :--- | :--- |
| **Total Paths (Endpoints)** | 23 | 130 | **+107** |
| **Total Component Schemas** | 11 | 45 | **+34** |
| **OpenAPI Specification Version** | `3.0.1` | `3.0.1` | Unchanged |
| **Specification File Size / Lines** | ~730 lines | ~10,200 lines | **+9,466 lines** |

### Metadata & Security Updates
* **API Documentation & Scope Support**: The API description has been updated to document OAuth scopes (`read`, `content`, `devices`, `delete`, `profile`, `apps`), granular token restrictions per device/plugin, 403 error reporting for missing capabilities, TRMNL CLI commands (`brew install usetrmnl/tap/trmnl`), `trmnlp` plugin server, and Model Context Protocol (MCP) tool integration.
* **Authentication Scheme**: Bearer token description updated from `"Account API key"` to `"API key or OAuth access token"`.
* **Hardware & Panel Headers**: Device check-in endpoints (`/api/display` and `/api/setup`) now support an optional `Panel-Rev` header specifying the fitted ePaper panel revision.

---

## 2. Changes to Existing Endpoints

Several existing endpoints received new HTTP methods, extended request payloads, or additional response statuses:

| Endpoint | Method | Change Type | Description |
| :--- | :--- | :--- | :--- |
| `/api/devices` | `POST` | **New Method** | Claim an unclaimed device using `friendly_id` (the setup code shown on the screen). |
| `/api/devices/{id}` | `PATCH` | **Extended Body** | Expanded from just sleep mode settings to support device rename (`name`), `refresh_interval`, `orientation`, `sleep_until`, `ota_enabled`, `firmware_channel`, `low_battery_notification_enabled`, `palette_id`, `font_family`, `text_scale`, `theme`, `playlist_item_ttl`, and BYOD display dimensions (`custom_width`, `custom_height`, `scale_factor`, etc.). |
| `/api/me` | `PATCH` | **New Method** | Update current user profile (`first_name`, `last_name`, `time_zone`, `locale`, `title_bar_enabled`, `low_battery_notification_email`, `account_name`). |
| `/api/playlists/items/{id}` | `DELETE` | **New Method** | Permanently remove an item from a playlist. |
| `/api/playlists/items/{id}` | `PATCH` | **Extended Body** | Added typography and visual presentation fields (`palette_id`, `font_family`, `text_scale`, `theme`). |
| `/api/plugin_settings/{id}` | `PATCH` | **New Method** | Rename instance (`name`, `description`), customize `refresh_interval` (5 to 1440 mins), and toggle `health_notification_enabled`. |
| `/api/plugin_settings/{id}/image` | `POST` | **Extended Body** | Added explicit support for multi-content types (`application/json`, `image/png`, `image/jpeg`, `image/webp`) with `image_base64`. |
| `/api/log` | `POST` | **Schema Fix** | Formally typed `logs` property as an array in the request body. |
| `/api/display` | `GET` | **Added Header** | Added optional `Panel-Rev` header parameter. |
| `/api/setup` | `GET` | **Added Header** | Added optional `Panel-Rev` header parameter. |

---

## 3. New Endpoints by Functional Domain (107 Endpoints)

### 3.1 Devices, Playlist Management & Diagnostics (16 endpoints)
Allows deep management of physical TRMNL devices, mirroring between devices, playlist reordering, and timeline simulation.

* **Claim & Association**:
  * `POST /api/devices`: Claim an unclaimed device with a friendly code.
  * `DELETE /api/devices/{device_id}/association`: Unlink and disassociate an owned device from the account.
* **Playlists & Layouts**:
  * `GET /api/devices/{device_id}/playlist_items`: Retrieve the device's playlist.
  * `POST /api/devices/{device_id}/playlist_items`: Add a plugin instance to the playlist.
  * `PUT /api/devices/{device_id}/playlist_items/order`: Reorder items in a device playlist.
  * `POST /api/devices/{device_id}/playlist_items/bulk`: Show or hide multiple playlist items in bulk.
  * `POST /api/devices/{device_id}/playlist_copies`: Duplicate an entire playlist to another device.
  * `DELETE /api/devices/{device_id}/playlist`: Clear all items from a device playlist.
* **Mirroring**:
  * `POST /api/devices/{device_id}/mirror`: Configure a device to mirror another device's playlist.
  * `DELETE /api/devices/{device_id}/mirror`: Stop mirroring.
  * `POST /api/devices/{device_id}/mirror/resyncs`: Force resynchronization of a mirrored device with its master.
* **Diagnostics & Scheduling**:
  * `GET /api/devices/{device_id}/timeline`: Retrieve past and planned device check-in timeline.
  * `GET /api/devices/{device_id}/forecast`: Simulate upcoming check-in and render decisions.
  * `GET /api/devices/{device_id}/coverage`: Check times of the week when nothing is scheduled to display.
  * `POST /api/devices/{device_id}/identification`: Trigger visual identification ping on a physical device.
  * `GET /api/devices/{device_id}/logs`: Read raw logs reported by the device.
  * `POST /api/devices/{device_id}/firmware_update_retries`: Retry stalled OTA firmware update.
  * `POST /api/devices/{device_id}/mashups`: Add a mashup layout to a device.
  * `GET /api/devices/{device_id}/mashups/options`: Read supported preset mashup layouts and compatible plugins.

### 3.2 Playlist Items & Scheduling (3 endpoints)
* `POST /api/playlists/items/{id}/duplicates`: Duplicate an existing playlist item.
* `GET /api/playlists/items/{item_id}/preview`: Fetch the rendered preview image of a playlist item.
* `GET /api/playlists/items/{item_id}/schedule`: Read display time rules for an item.
* `PUT /api/playlists/items/{item_id}/schedule`: Set display schedule rules for an item.

### 3.3 Plugins & Dynamic Configuration Lifecycle (35 endpoints)
Major expansion introducing an enterprise plugin engine with dynamic form configuration, connection handshakes, and diagnostic tools.

* **Plugin Catalog**:
  * `GET /api/plugins`: List all available installable plugins in the marketplace.
  * `GET /api/plugins/{id}`: Get full plugin metadata and configuration form schema.
* **Installation Flow**:
  * `POST /api/plugin_installations`: Initiate a multi-step plugin installation.
  * `GET /api/plugin_installations/{id}`: View installation state.
  * `DELETE /api/plugin_installations/{id}`: Cancel a pending installation.
  * `POST /api/plugin_installations/{plugin_installation_id}/completion`: Finalize and commit an installation.
* **Dynamic Configuration Engine**:
  * `GET /api/plugin_installations/{plugin_installation_id}/configuration`: Read schema sections and fields.
  * `PATCH /api/plugin_installations/{plugin_installation_id}/configuration`: Save drafted configuration.
  * `POST /api/plugin_installations/{plugin_installation_id}/configuration/evaluation`: Test configuration values without persisting.
  * `POST /api/plugin_installations/{plugin_installation_id}/configuration/choices/{resolver_id}`: Fetch dynamic options for fields with remote resolvers.
  * `GET /api/plugin_settings/{plugin_setting_id}/configuration`: Read active typed configuration.
  * `PATCH /api/plugin_settings/{plugin_setting_id}/configuration`: Apply atomic typed changes.
  * `POST /api/plugin_settings/{plugin_setting_id}/configuration/evaluation`: Test changes on an active setting.
  * `POST /api/plugin_settings/{plugin_setting_id}/configuration/choices/{resolver_id}`: Resolve remote choices on an active setting.
  * `POST /api/plugin_settings/{plugin_setting_id}/configuration/photo_selection`: Initiate Google Photos picker session.
  * `PATCH /api/plugin_settings/{plugin_setting_id}/configuration/photo_selection`: Save selected Google Photos album/media.
* **OAuth Connections & Account Links**:
  * `POST /api/plugin_installations/{plugin_installation_id}/connections/{connection_id}/attempts`: Start OAuth grant attempt.
  * `DELETE /api/plugin_installations/{plugin_installation_id}/connections/{connection_id}`: Remove OAuth connection.
  * `GET /api/plugin_connection_attempts/{id}`: Check OAuth authorization status.
  * `POST /api/plugin_connection_attempts/{id}/confirmation`: Complete OAuth handshake with callback payload.
  * `DELETE /api/plugin_connection_attempts/{id}`: Cancel pending OAuth connection.
  * `POST /api/plugin_settings/{plugin_setting_id}/connections/{connection_id}/attempts`: Connect an account to an existing plugin.
  * `DELETE /api/plugin_settings/{plugin_setting_id}/connections/{connection_id}`: Disconnect an account.
* **Maintenance & Debugging**:
  * `POST /api/plugin_settings/{id}/copies`: Clone a plugin setting instance.
  * `DELETE /api/plugin_settings/{id}/credentials`: Unlink credentials and delete instance.
  * `POST /api/plugin_settings/{id}/debug_logs`: Enable 24-hour verbose debug logging.
  * `PUT /api/plugin_settings/{id}/featured_image`: Generate marketplace preview image.
  * `DELETE /api/plugin_settings/{id}/featured_image`: Remove preview image.
  * `POST /api/plugin_settings/{id}/health_resets`: Reset error status on an unhealthy plugin.
  * `POST /api/plugin_settings/{id}/state_clears`: Clear cached/accumulated state.
  * `DELETE /api/plugin_settings/{id}/transform`: Clear private plugin transform script.
  * `GET /api/plugin_settings/{plugin_setting_id}/files`: Read files of a private plugin.
  * `PUT /api/plugin_settings/{plugin_setting_id}/files`: Update files of a private plugin.
  * `GET /api/plugin_settings/{plugin_setting_id}/logs`: Read instance health logs.
  * `GET /api/plugin_settings/{plugin_setting_id}/merge_variables`: Read resolved Liquid merge variables.
  * `POST /api/plugin_settings/{plugin_setting_id}/refreshes`: Trigger an immediate refresh render.
  * `GET /api/plugin_settings/{plugin_setting_id}/refreshes/{id}`: Poll status of a refresh request.
  * `GET /api/plugin_settings/{plugin_setting_id}/removal_preview`: Preview impact of removing a plugin.
  * `POST /api/plugin_settings/{plugin_setting_id}/screenshots`: Trigger preview rendering.
  * `GET /api/plugin_settings/{plugin_setting_id}/screenshots/{id}`: Fetch screenshot result.
  * `GET /api/plugin_settings/{plugin_setting_id}/timeline`: View instance render timeline.
  * `POST /api/plugin_settings/custom_fields/verifications`: Validate custom fields YAML.

### 3.4 Room Booking Application (25 endpoints)
A turnkey first-party TRMNL app enabling room signage, calendar synchronization, and walk-up reservations.

* **Installation & Settings**:
  * `GET /api/apps/room_booking/{installation_id}`: Get room booking status and details.
  * `GET /api/apps/room_booking/{installation_id}/settings`: Read room booking preferences.
  * `PATCH /api/apps/room_booking/{installation_id}/settings`: Update settings (timezone, walk-up booking window, dark mode, rotation).
  * `PUT /api/apps/room_booking/{installation_id}/settings/company_logo`: Upload monochrome logo.
  * `DELETE /api/apps/room_booking/{installation_id}/settings/company_logo`: Delete monochrome logo.
  * `PUT /api/apps/room_booking/{installation_id}/settings/color_company_logo`: Upload color logo.
  * `DELETE /api/apps/room_booking/{installation_id}/settings/color_company_logo`: Delete color logo.
  * `GET /api/apps/room_booking/{installation_id}/billing`: Check billing/subscription status.
* **Integrations (Google Workspace, Microsoft 365, iCal)**:
  * `GET /api/apps/room_booking/{installation_id}/integrations`: List connected accounts.
  * `GET /api/apps/room_booking/{installation_id}/integrations/{integration_type}/connect_url`: Get OAuth URL to connect Google/Microsoft.
  * `PATCH /api/apps/room_booking/{installation_id}/integrations/{integration_type}/{id}`: Configure synced calendars.
  * `DELETE /api/apps/room_booking/{installation_id}/integrations/{integration_type}/{id}`: Remove integration.
  * `POST /api/apps/room_booking/{installation_id}/integrations/{integration_type}/{id}/syncs`: Force calendar sync.
* **Calendars & Bookings**:
  * `GET /api/apps/room_booking/{installation_id}/calendars`: List calendars.
  * `POST /api/apps/room_booking/{installation_id}/calendars`: Add an iCal calendar.
  * `GET /api/apps/room_booking/{installation_id}/calendars/{id}`: Get calendar details and schedule.
  * `PATCH /api/apps/room_booking/{installation_id}/calendars/{id}`: Edit calendar settings.
  * `DELETE /api/apps/room_booking/{installation_id}/calendars/{id}`: Remove calendar.
  * `GET /api/apps/room_booking/{installation_id}/calendars/{id}/bookings`: List bookings.
  * `POST /api/apps/room_booking/{installation_id}/calendars/{id}/bookings`: Create room booking.
  * `DELETE /api/apps/room_booking/{installation_id}/calendars/{id}/bookings/{booking_id}`: Cancel booking.
  * `PATCH /api/apps/room_booking/{installation_id}/calendars/{id}/bookings/{booking_id}/end`: End in-progress booking early.
  * `PUT /api/apps/room_booking/{installation_id}/calendars/{id}/devices/{device_id}`: Assign calendar to a device screen.
  * `DELETE /api/apps/room_booking/{installation_id}/calendars/{id}/devices/{device_id}`: Unassign device.
  * `POST /api/apps/room_booking/{installation_id}/calendars/{id}/refetches`: Force refresh from source.
  * `GET /api/apps/room_booking/{installation_id}/calendars/{id}/screens`: Read rendered screen status.
* **Collections (Multi-Room Displays)**:
  * `GET /api/apps/room_booking/{installation_id}/collections`: List collections.
  * `POST /api/apps/room_booking/{installation_id}/collections`: Create collection.
  * `PATCH /api/apps/room_booking/{installation_id}/collections/{id}`: Edit collection.
  * `DELETE /api/apps/room_booking/{installation_id}/collections/{id}`: Delete collection.
  * `PUT /api/apps/room_booking/{installation_id}/collections/{id}/devices/{device_id}`: Assign collection to device.
  * `DELETE /api/apps/room_booking/{installation_id}/collections/{id}/devices/{device_id}`: Unassign collection.
  * `GET /api/apps/room_booking/{installation_id}/collections/{id}/screens`: Read collection screen status.
* **Walk-Up Public Portal**:
  * `GET /api/book/{token}/events`: List events visible to walk-up visitors.
  * `POST /api/book/{token}/bookings`: Create booking directly from room QR code.
  * `DELETE /api/book/{token}/bookings/{id}`: Cancel walk-up booking.
  * `PATCH /api/book/{token}/bookings/{id}/end`: End walk-up booking.

### 3.5 Fleet Management Application (8 endpoints)
Manage groups of screens displaying uniform content with centralized synchronization.

* `GET /api/apps/fleet/{installation_id}`: Read fleet configuration and health overview.
* `PATCH /api/apps/fleet/{installation_id}/settings`: Select device settings mirrored from master.
* `PATCH /api/apps/fleet/{installation_id}/alerts`: Configure overdue check-in alerting thresholds.
* `PUT /api/apps/fleet/{installation_id}/master`: Assign master device.
* `DELETE /api/apps/fleet/{installation_id}/master`: Remove master device.
* `GET /api/apps/fleet/{installation_id}/devices`: List mirror devices in fleet.
* `POST /api/apps/fleet/{installation_id}/devices`: Add device to fleet.
* `DELETE /api/apps/fleet/{installation_id}/devices/{device_id}`: Remove device from fleet.
* `POST /api/apps/fleet/{installation_id}/pushes`: Trigger push to all fleet mirrors.
* `POST /api/apps/fleet/{installation_id}/devices/{device_id}/pushes`: Trigger push to a specific mirror.

### 3.6 Apps Management (3 endpoints)
* `GET /api/apps`: List available installable first-party applications.
* `GET /api/apps/installations`: List user's installed applications.
* `POST /api/apps/installations`: Install an application.
* `DELETE /api/apps/installations/{id}`: Uninstall an application.

### 3.7 Mashups (3 endpoints)
Multi-widget split-screen layouts:
* `GET /api/mashups/{id}`: Read layout configuration and active sections.
* `PATCH /api/mashups/{id}`: Update quadrant/section mappings.
* `POST /api/mashups/{id}/health_resets`: Reset error breaker state.
* `GET /api/mashups/{mashup_id}/timeline`: Check render timeline of mashup segments.

### 3.8 Recipes Catalog (4 endpoints)
* `GET /api/recipes`: Search published community and official recipes with categories/tags.
* `GET /api/recipes/{id}`: Read recipe details, author, configuration requirements.
* `POST /api/recipes/{id}/installs`: One-click install recipe into user account.
* `GET /api/recipes/{id}/markup`: View recipe HTML/Liquid source template.

### 3.9 User Themes (3 endpoints)
Custom display stylings:
* `GET /api/user_themes`: List custom user themes.
* `POST /api/user_themes`: Create custom theme.
* `POST /api/user_themes/imports`: Import theme from snippet JSON.
* `GET /api/user_themes/{id}`: Read theme details, settings, and compiled SCSS.
* `PATCH /api/user_themes/{id}`: Update theme settings (fonts, spacing, palette mapping).
* `DELETE /api/user_themes/{id}`: Delete custom theme.

### 3.10 Developer / Custom Plugins (2 endpoints)
* `GET /api/my_plugins`: List developer's custom registered plugins.
* `POST /api/my_plugins`: Register a new developer plugin (with webhook URLs and manifest).
* `PATCH /api/my_plugins/{id}`: Update developer plugin settings and callbacks.

### 3.11 Developer Analytics (4 endpoints)
* `GET /api/analytics`: View install counts, active device counts, and performance metrics.
* `GET /api/analytics/errors`: List rendering exceptions encountered by users.
* `POST /api/analytics/hidden_errors`: Suppress specific known error warnings.
* `DELETE /api/analytics/hidden_errors`: Unsuppress hidden errors.
* `GET /api/analytics/uninstall_feedback`: View aggregated reasons why users uninstalled your plugin.

### 3.12 System Capabilities (1 endpoint)
* `GET /api/capabilities`: Read supported TRMNL Companion contract version numbers for client compatibility negotiation.

---

## 4. Component Schemas Diff

### 4.1 Modifications to Existing Schemas

| Schema | Added Properties | Removed Properties | Other Adjustments |
| :--- | :--- | :--- | :--- |
| **`Device`** | `management`, `firmware_version`, `firmware_channel`, `ota_enabled`, `pinned_firmware_version`, `refresh_interval`, `orientation`, `sleep_screen_enabled`, `low_battery_notification_enabled`, `sleep_until`, `mashup_layouts` | *(none)* | `mac_address` description updated noting partial masking (`••:••:••:••:9A:BC`) unless using root account key. Date-time formats normalized. |
| **`PlaylistItem`** | `presentation`, `palette_id`, `font_family`, `text_scale`, `theme` | `playlist_group_id` | Full support for typography scaling and visual theming directly on individual playlist entries. |
| **`PluginSetting`** | `sync`, `refresh_interval`, `health_notification_enabled` | *(none)* | Detailed execution and refresh intervals. |
| **`User`** | `title_bar_enabled`, `account_name`, `low_battery_notification_email` | `api_key` | Removal of `api_key` from user model payload enhances API key security (tokens are no longer echoed back). |
| **`Palette`** | *(none)* | *(none)* | Property requirements relaxed for dynamic palettes. |

### 4.2 New Component Schemas (34 Schemas)

#### Applications & Fleet Management
* **`App`**: Basic application catalog entry (`app_key`, `name`, `tagline`, `installed`).
* **`AppInstallation`**: Active installation instance details (`id`, `app_key`, `name`, `summary`, `created_at`).
* **`Fleet`**: Centralized device fleet instance (`id`, `name`, `master`, `mirror_count`, `needs_push_count`, `overdue_count`, `last_pushed_at`, `inherited_settings`, `alerts`).
* **`FleetMember`**: State of an individual mirror device within a fleet (`device_id`, `name`, `role`, `check_in_state`, `expected_check_in_at`, `last_pushed_at`, `in_sync`).

#### Dynamic Configuration Engine
* **`CatalogEntry`**: Unified catalog item model (`kind`, `id`, `name`, `source_revision`, `requirements`, `compatibility`, `install_choices`).
* **`ConfigurationChange`**: RFC 6902-style atomic change patch (`op`, `path`, `value`).
* **`ConfigurationWrite`**: Atomic configuration commit payload (`revision`, `changes`).
* **`ConfigurationField`**: Form schema field descriptor (`path`, `type`, `label`, `help`, `required`, `read_only`, `nullable`, `constraints`, `choices`).
* **`ConfigurationError`**: Typed configuration validation error (`error`).
* **`PluginConfiguration`**: Complete dynamic plugin config model (`schema_version`, `revision`, `required_capabilities`, `sections`, `values`, `secrets`, `connections`, `actions`, `readiness`, `sync`).
* **`PluginConnectionAttempt`**: OAuth authentication flow tracker (`id`, `state`, `expires_at`, `connection_id`, `launch_url`, `target`).
* **`PluginInstallation`**: Staged installation instance (`id`, `state`, `source`, `plugin_setting_id`, `expires_at`).
* **`PluginSync`**: Sync state metadata (`source_kinds`, `upload_allowed`, `reason`, `action_id`).

#### Plugins, Recipes & Mashups
* **`Plugin`**: Full plugin specification including form fields and OAuth requirements.
* **`Recipe`**: Pre-built community template definition (`author`, `categories`, `strategy`, `custom_fields`, `stats`, `screenshot_url`).
* **`Mashup`**: Multi-tile screen definition (`layout`, `grid_config`, `positions`, `contents`, `health_notification_enabled`).
* **`MashupOptions`**: Layout presets and compatible plugin settings for assembling mashups.
* **`MyPlugin`**: Developer-published private/custom plugin definition with webhooks.
* **`MyPluginParams`**: Request payload to create/update developer plugins.

#### Room Booking System
* **`RoomBooking`**: Reservation entry (`id`, `title`, `starts_at`, `ends_at`, `all_day`, `source`, `status`, `booked_with_trmnl`).
* **`RoomBookingBilling`**: Subscription and trial state for the room booking add-on.
* **`RoomBookingCalendar`**: Calendar entity and integration metadata (`feed_backed`, `ics_url`, `booking_mode`, `bound_device_ids`, `public_booking_url`).
* **`RoomBookingCalendarDetails`**: Calendar entity extended with `current_booking` and `upcoming_bookings`.
* **`RoomBookingCollection`**: Group of room calendars for directory screens.
* **`RoomBookingDeviceScreen`**: Binding of a physical device with a rendered calendar screen image.
* **`RoomBookingIntegration`**: Third-party calendar provider connection (`Google`, `Microsoft 365`).
* **`RoomBookingSettings`**: Room booking workspace settings (`timezone`, `public_bookings`, `dark_mode`, logos).
* **`RoomBookingSummary`**: High-level room booking installation status.
* **`PublicBookingEvent`**: Walk-up booking reservation slot for QR code guests.

#### Theming, Diagnostics & Displays
* **`UserTheme`**: User-defined design system theme with compiled SCSS.
* **`UserThemeSettings`**: Typography, scale factor, surfaces, chips, and color remap rules.
* **`Screen`**: Rendered screen image container (`image_url`, `rendered_at`, `playlist_item_id`, `plugin_setting_id`, `mashup_id`).
* **`Timeline`**: Complete historical and future check-in log.
* **`TimelineStep`**: Simulated check-in event (`at`, `refresh_seconds`, `render_reason`, etc.).

---

## 5. Potential Opportunities for TRMNL Android Buddy

The updated OpenAPI snapshot exposes numerous capabilities that could directly improve the Android application:

1. **Device Claiming Flow**:
   * Implement QR code scanning / Bluetooth or manual input of `friendly_id` directly in the Android app to claim new devices using `POST /api/devices`.
2. **Enhanced Device Settings**:
   * Support device renaming, refresh intervals (down to 5 minutes), sleep until datetime, touchbar mode, temperature profiles, and orientation directly in device details UI via `PATCH /api/devices/{id}`.
3. **Playlist Item Management**:
   * Allow users to delete playlist items via `DELETE /api/playlists/items/{id}`.
   * Allow duplicating items and customizing typography scale, font, and palette per item.
   * View live rendered image previews of playlist items (`/api/playlists/items/{item_id}/preview`).
   * Manage display schedule rules per item (`/api/playlists/items/{item_id}/schedule`).
4. **Device Mirroring & Reordering**:
   * Configure devices to mirror others, trigger mirror resyncs, and drag-to-reorder playlist items.
5. **Timeline & Diagnostics**:
   * Visualize device check-in timeline and simulate future render intervals via the `/timeline` and `/forecast` endpoints.
   * Trigger the "Identify Device" physical screen flash directly from the app.
