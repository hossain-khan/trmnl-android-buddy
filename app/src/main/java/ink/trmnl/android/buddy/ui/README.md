# UI Screens Documentation

This document describes the UI screens in the TRMNL Android Buddy app.

## Architecture

The app follows these best practices:

- **Circuit Architecture**: Compose-driven UDF (Unidirectional Data Flow)
- **Metro Dependency Injection**: Constructor injection with KSP code generation
- **Repository Pattern**: Abstracts data sources (DataStore, API)
- **Kotlin Flows**: Reactive data streams with coroutines
- **DataStore**: Secure preference storage

## Screen Structure

Each screen package contains:
- `<Name>Screen.kt` - Screen definition, Presenter, and UI Content
- Screen sealed class with State and Events
- Presenter handles business logic
- UI Composable renders the state

## Screens

### 1. Welcome Screen (`ui.welcome.WelcomeScreen`)

**Purpose**: Landing screen that welcomes users and routes them based on setup status.

**Location**: `app/src/main/java/ink/trmnl/android/buddy/ui/welcome/`

**Features**:
- Shows app branding (title and subtitle)
- Checks if API token is configured
- Routes to token input if not configured
- Routes to main app if configured

**State**:
- `isLoading: Boolean` - Loading state while checking preferences

**Events**:
- `GetStartedClicked` - User taps "Get Started" button

**Dependencies**:
- `UserPreferencesRepository` - Checks for existing API token

### 2. Access Token Screen (`ui.accesstoken.AccessTokenScreen`)

**Purpose**: Allows users to input and save their TRMNL API token.

**Location**: `app/src/main/java/ink/trmnl/android/buddy/ui/accesstoken/`

**Features**:
- Secure token input with show/hide toggle
- Real-time validation
- Error messaging
- Saves token to DataStore
- Back navigation

**State**:
- `token: String` - Current token input
- `isLoading: Boolean` - Saving state
- `errorMessage: String?` - Validation/error message

**Events**:
- `TokenChanged(token: String)` - User types in token field
- `SaveClicked` - User taps "Save Token" button
- `BackClicked` - User taps back button

**Validation**:
- Token cannot be empty
- Token must be at least 10 characters

**Dependencies**:
- `UserPreferencesRepository` - Saves token and onboarding status

### 3. TRMNL Devices Screen (`ui.devices.TrmnlDevicesScreen`)

**Purpose**: Main screen displaying all TRMNL devices with their status.

**Location**: `app/src/main/java/ink/trmnl/android/buddy/ui/devices/`

**Features**:
- Fetches devices from TRMNL API
- Displays devices in Material 3 Cards with ListItems
- Shows device properties:
  - Name and Friendly ID
  - MAC Address
  - Battery level with color-coded progress bar
  - Battery voltage (if available)
  - WiFi signal strength with color-coded progress bar
  - WiFi RSSI in dBm (if available)
- Loading, error, and empty states
- Pull-to-refresh capability (TODO)
- Device click navigation (TODO)

**State**:
- `devices: List<Device>` - List of fetched devices
- `isLoading: Boolean` - Loading state
- `errorMessage: String?` - Error message if fetch fails

**Events**:
- `Refresh` - Reload devices from API
- `DeviceClicked(device: Device)` - User taps on a device

**API Integration**:
- Uses `TrmnlApiService.getDevices()` from `:api` module
- Authenticates with Bearer token from DataStore
- Handles all EitherNet ApiResult types:
  - Success: Displays devices
  - HttpFailure: Shows specific error (401, 404, etc.)
  - NetworkFailure: Network connection error
  - ApiFailure: API-specific error
  - UnknownFailure: Unexpected error

**UI Components**:
- Material 3 Cards with elevation
- ListItem for structured device info
- LinearProgressIndicator for battery and WiFi
- Color-coded indicators:
  - Green (≥60%/70%): Good status
  - Orange (30-60%/40-70%): Warning
  - Red (<30%/40%): Critical

**Dependencies**:
- `TrmnlApiService` - API calls
- `UserPreferencesRepository` - Get API token

## Data Layer

### UserPreferences (`data.preferences.UserPreferences`)

Data class representing user preferences:

```kotlin
data class UserPreferences(
    val apiToken: String? = null,
    val isOnboardingCompleted: Boolean = false,
)
```

### UserPreferencesRepository

**Interface**: `data.preferences.UserPreferencesRepository`

**Implementation**: `data.preferences.UserPreferencesRepositoryImpl`

**Purpose**: Repository for managing user preferences using DataStore.

**API**:

```kotlin
interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun saveApiToken(token: String)
    suspend fun clearApiToken()
    suspend fun setOnboardingCompleted()
    suspend fun clearAll()
}
```

**Storage**: Uses AndroidX DataStore Preferences API

**File**: `user_preferences.xml` (in app's DataStore directory)

## Navigation Flow

```
WelcomeScreen
    └─> Check if API token exists
        ├─> No token: Navigate to AccessTokenScreen
        │   └─> Save token -> Navigate to TrmnlDevicesScreen
        └─> Has token: Navigate to TrmnlDevicesScreen
            └─> Click device -> Navigate to DeviceDetailScreen (TODO)
```

## Dependencies Added

### DataStore
- `androidx.datastore:datastore-preferences:1.1.2`

Added to `gradle/libs.versions.toml`:
```toml
androidxDataStore = "1.1.2"
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "androidxDataStore" }
```

## Example Screens

The `circuit` package contains example screens from Circuit tutorial:
- `ExampleInboxScreen.kt` - Email inbox example
- `ExampleEmailDetailsScreen.kt` - Email detail example

**These are kept as examples** and can be referenced when building new screens.

## Next Steps

1. **Add Pull-to-Refresh** - Swipe to refresh devices list
2. **Create DeviceDetailScreen** - Show detailed device information and controls
3. **Add Settings Screen** - Allow token management and app preferences
4. **Implement Error Retry** - Add retry button for failed API calls
5. **Add Device Filtering** - Filter devices by battery level or WiFi strength
6. **Add Device Sorting** - Sort devices by name, battery, or WiFi strength

## Testing

To test the screens:

1. Run the app on emulator/device
2. First launch shows WelcomeScreen
3. Tap "Get Started"
4. Enter API token in AccessTokenScreen
5. Token is saved to DataStore
6. On next launch, app checks for token and routes accordingly

## Best Practices Followed

✅ Repository pattern for data access
✅ Kotlin Flows for reactive data
✅ DataStore for secure preferences
✅ Circuit architecture (UDF)
✅ Metro dependency injection
✅ Separation of concerns (Screen/Presenter/UI)
✅ Material Design 3 components
✅ Proper error handling
✅ Input validation
✅ Loading states
✅ Back navigation support
