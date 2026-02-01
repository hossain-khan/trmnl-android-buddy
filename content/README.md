# Content Module

The `:content` Gradle module manages TRMNL's public content feeds (announcements and blog posts) with offline-first architecture, providing users with on-the-go access to platform updates, articles, and tutorials.

## Overview

This Android library module fetches, caches, and manages RSS/Atom feeds from TRMNL's content platform:

- **Announcements**: Platform updates, feature releases, and important notices
- **Blog Posts**: Articles, tutorials, and deep-dive content about TRMNL ecosystem

### Key Features

- 📡 **RSS/Atom Feed Parsing**: Fetches content from TRMNL feeds using [RSS-Parser](https://github.com/prof18/RSS-Parser)
- 💾 **Offline-First Storage**: Room database caching ensures content availability without network
- 🔄 **Reactive Data Flow**: Kotlin Flow APIs for real-time UI updates
- 📖 **Read Tracking**: Monitor user's reading progress and mark items as read
- ⭐ **Favorites/Bookmarks**: Users can save favorite posts for quick access
- 🔍 **Search & Filter**: Full-text search and category-based filtering
- 🎨 **Multi-Image Support**: Extracts and displays all images from RSS content with auto-rotation

## Architecture

```
content/
├── db/                         # Room database layer
│   ├── AnnouncementEntity.kt   # Announcement data model
│   ├── AnnouncementDao.kt      # Announcement database queries
│   ├── BlogPostEntity.kt       # Blog post data model
│   ├── BlogPostDao.kt          # Blog post database queries
│   ├── ContentDatabase.kt      # Room database configuration
│   └── Converters.kt           # Type converters (Instant, List<String>)
├── repository/                 # Data access layer
│   ├── AnnouncementRepository.kt  # Announcement data management
│   └── BlogPostRepository.kt      # Blog post data management
├── di/                         # Dependency injection
│   └── ContentBindings.kt      # Module-level DI bindings
├── models/                     # Data models
│   └── AnnouncementFilter.kt   # Filter enums for announcements
└── RssParserFactory.kt         # RSS parser factory
```

### Design Patterns

- **Offline-First**: Room database is the single source of truth; network fetches update cache
- **Repository Pattern**: Abstracts data sources from UI layer
- **Flow-Based Reactivity**: All data queries return Kotlin Flow for reactive UI updates
- **Dependency Injection**: Uses Metro DI for constructor injection and `@Singleton` scoping

## Database Schema

### Announcements Table

```kotlin
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,        // Unique identifier (URL)
    val title: String,                  // Announcement title
    val summary: String,                // Brief description
    val link: String,                   // URL to full announcement
    val publishedDate: Instant,         // Publication timestamp
    val isRead: Boolean = false,        // Read tracking
    val fetchedAt: Instant,             // Cache timestamp
)
```

### Blog Posts Table

```kotlin
@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey val id: String,           // Unique identifier (URL)
    val title: String,                    // Post title
    val summary: String,                  // Short description
    val link: String,                     // URL to full post
    val authorName: String,               // Author display name
    val category: String?,                // Post category (nullable)
    val publishedDate: Instant,           // Publication timestamp
    val featuredImageUrl: String?,        // Main image URL (nullable)
    val imageUrls: List<String>?,         // All image URLs from content
    val isRead: Boolean = false,          // Read tracking
    val lastReadAt: Instant? = null,      // Last read timestamp
    val fetchedAt: Instant,               // Cache timestamp
    val isFavorite: Boolean = false,      // User bookmark
)
```

**Database Version**: 3

**Migrations**:
- **v1 → v2**: Added `blog_posts` table
- **v2 → v3**: Added `imageUrls` column to `blog_posts`

## Data Sources

### RSS/Atom Feeds

| Content Type  | Feed URL                                      | Format |
|---------------|-----------------------------------------------|--------|
| Announcements | `https://trmnl.com/feeds/announcements.xml` | Atom   |
| Blog Posts    | `https://trmnl.com/feeds/posts.xml`        | Atom   |

### Content Processing

**Announcements**:
- Parses title, summary, link, and published date from Atom feed
- Automatically sanitizes HTML to plain text for summaries
- Caches all entries for offline access

**Blog Posts**:
- Extracts all metadata from Atom feed
- Parses featured image from `<content>` HTML
- Extracts **all images** from post content using regex pattern
- Sanitizes HTML for readable summaries
- Supports categories and author attribution

## API Reference

### AnnouncementRepository

```kotlin
@Singleton
class AnnouncementRepository @Inject constructor(
    private val announcementDao: AnnouncementDao
)
```

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllAnnouncements()` | `Flow<List<AnnouncementEntity>>` | Stream all announcements (newest first) |
| `getUnreadAnnouncements()` | `Flow<List<AnnouncementEntity>>` | Stream unread announcements only |
| `getUnreadCount()` | `Flow<Int>` | Stream count of unread announcements |
| `refreshAnnouncements()` | `suspend Result<Unit>` | Fetch latest from RSS feed and update cache |
| `markAsRead(id: String)` | `suspend Unit` | Mark specific announcement as read |
| `markAllAsRead()` | `suspend Unit` | Mark all announcements as read |

### BlogPostRepository

```kotlin
@Singleton
class BlogPostRepository @Inject constructor(
    private val blogPostDao: BlogPostDao
)
```

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllPosts()` | `Flow<List<BlogPostEntity>>` | Stream all blog posts (newest first) |
| `getPostsByCategory(category)` | `Flow<List<BlogPostEntity>>` | Stream posts filtered by category |
| `getFavorites()` | `Flow<List<BlogPostEntity>>` | Stream user's bookmarked posts |
| `getUnread()` | `Flow<List<BlogPostEntity>>` | Stream unread posts only |
| `getUnreadCount()` | `Flow<Int>` | Stream count of unread posts |
| `searchPosts(query)` | `Flow<List<BlogPostEntity>>` | Full-text search across title and summary |
| `refreshBlogPosts()` | `suspend Result<Unit>` | Fetch latest from RSS feed and update cache |
| `markAsRead(id)` | `suspend Unit` | Mark specific post as read |
| `markAllAsRead()` | `suspend Unit` | Mark all posts as read |
| `toggleFavorite(id)` | `suspend Unit` | Bookmark/unbookmark a post |

## Usage Examples

### Fetching Announcements

```kotlin
@CircuitInject(AnnouncementsScreen::class, AppScope::class)
@Composable
fun AnnouncementsPresenter(
    repository: AnnouncementRepository
): AnnouncementsScreen.State {
    val announcements by repository.getAllAnnouncements().collectAsState(initial = emptyList())
    
    return AnnouncementsScreen.State(
        announcements = announcements,
        isLoading = false,
        eventSink = { event ->
            when (event) {
                is Event.AnnouncementClicked -> {
                    // Open in browser
                }
                is Event.MarkAsRead -> {
                    scope.launch { repository.markAsRead(event.id) }
                }
            }
        }
    )
}
```

### Refreshing Blog Posts

```kotlin
// In presenter or repository
suspend fun refreshContent() {
    blogPostRepository.refreshBlogPosts()
        .onSuccess { 
            // Show success message
        }
        .onFailure { error ->
            // Handle error
            Timber.e(error, "Failed to refresh blog posts")
        }
}
```

### Searching Posts

```kotlin
val searchResults by repository.searchPosts("tutorial").collectAsState(initial = emptyList())
```

## Dependencies

This module uses the following key dependencies:

| Dependency | Version | Purpose |
|------------|---------|---------|
| [RSS-Parser](https://github.com/prof18/RSS-Parser) | 6.0.8 | Parse Atom/RSS feeds |
| [Room](https://developer.android.com/training/data-storage/room) | 2.8.3 | Local database for offline caching |
| [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) | 1.10.2 | Asynchronous operations |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | 1.9.0 | JSON serialization for type converters |
| [Timber](https://github.com/JakeWharton/timber) | 5.0.1 | Logging |

## Testing

The module includes comprehensive unit tests:

- **`BlogPostRepositoryTest`**: Tests RSS parsing, HTML sanitization, and image extraction
- **`ConvertersTest`**: Tests Room type converters (JSON serialization for URLs with special characters)

### Running Tests

```bash
# Run all content module tests
./gradlew :content:test

# Run with coverage
./gradlew :content:testDebugUnitTestCoverage
```

### Test Coverage

- RSS feed parsing with real sample XML
- HTML sanitization (strip tags, entities)
- Image URL extraction from HTML content
- URLs with special characters (commas, spaces, fragments)
- Database operations (insert, query, update, delete)
- Error handling and edge cases

## Type Converters

The module provides custom Room type converters:

### Instant ↔ Long (Epoch Seconds)

```kotlin
@TypeConverter
fun fromInstant(instant: Instant?): Long? = instant?.epochSecond

@TypeConverter
fun toInstant(epochSecond: Long?): Instant? = epochSecond?.let { Instant.ofEpochSecond(it) }
```

### List<String> ↔ JSON String

Uses JSON serialization (not comma-separated strings) to handle URLs with special characters:

```kotlin
@TypeConverter
fun fromStringList(list: List<String>?): String? = list?.let { Json.encodeToString(it) }

@TypeConverter
fun toStringList(value: String?): List<String>? = 
    value?.let { Json.decodeFromString<List<String>>(it) }
```

**Why JSON?** Prevents data corruption when image URLs contain commas, ampersands, or other special characters.

## Image Processing

### Multi-Image Carousel Support

The `BlogPostRepository` extracts **all images** from RSS feed content:

```kotlin
private fun extractAllImages(content: String?): List<String>? {
    val imgRegex = """<img[^>]+src\s*=\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
    val imageUrls = imgRegex.findAll(content).mapNotNull { it.groupValues.getOrNull(1) }.toList()
    return imageUrls.takeIf { it.isNotEmpty() }
}
```

**Features**:
- Extracts all `<img src="...">` tags from HTML content
- Case-insensitive regex matching
- Returns `null` if no images found (graceful fallback)
- Used by UI to display image carousels with auto-rotation

## Integration with App Module

The `:content` module is consumed by the `:app` module via Metro DI:

```kotlin
// app/src/main/java/ink/trmnl/android/buddy/di/AppBindings.kt
@Provides
@SingleIn(AppScope::class)
fun provideContentDatabase(@ApplicationContext context: Context): ContentDatabase {
    return Room.databaseBuilder(context, ContentDatabase::class.java, "content-database")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}

@Provides
fun provideAnnouncementDao(database: ContentDatabase): AnnouncementDao = database.announcementDao()

@Provides
fun provideBlogPostDao(database: ContentDatabase): BlogPostDao = database.blogPostDao()
```

Repositories are auto-injected via constructor injection with `@Singleton` scope.

## Error Handling

All repository methods that perform network operations return `Result<T>`:

```kotlin
suspend fun refreshBlogPosts(): Result<Unit> {
    return try {
        val channel = rssParser.getRssChannel(POSTS_FEED_URL)
        val posts = channel.items.mapNotNull { /* transform */ }
        blogPostDao.insertAll(posts)
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to refresh blog posts")
        Result.failure(e)
    }
}
```

UI layer handles success/failure appropriately.

## Performance Considerations

- **Lazy Loading**: Uses `LazyColumn` in UI for efficient scrolling
- **Image Caching**: Coil library handles image caching automatically
- **Database Indexing**: Primary keys and foreign keys properly indexed
- **Flow Emissions**: Room Flow queries only emit when data actually changes
- **Background Threading**: All database operations run on IO dispatcher

## Future Enhancements

Potential improvements tracked in issues:

- [ ] Pagination for large feeds (50+ posts)
- [ ] In-app reading view with WebView
- [ ] Reading progress tracking (scroll position)
- [ ] Background sync worker (WorkManager)
- [ ] Push notifications for new content
- [ ] Share functionality
- [ ] Deep linking to specific posts
- [ ] Cache expiration policy

## Related Issues

- [#141: Feature - Incorporate TRMNL announcements](https://github.com/hossain-khan/trmnl-android-buddy/issues/141) ✅ Closed
- [#142: Feature - Incorporate blog posts](https://github.com/hossain-khan/trmnl-android-buddy/issues/142) ✅ Closed
- [#162: Improve blog post experience](https://github.com/hossain-khan/trmnl-android-buddy/issues/162) ✅ Closed

## Contributing

When adding new features to this module:

1. Follow existing architecture patterns (offline-first, Flow-based)
2. Write unit tests with AssertK assertions
3. Update database schema version and provide migrations
4. Use Material 3 design system in consuming UI
5. Format code with `./gradlew formatKotlin`
6. Update this README with API changes

## Resources

- [Circuit Documentation](https://slackhq.github.io/circuit/)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [RSS-Parser GitHub](https://github.com/prof18/RSS-Parser)
- [Kotlin Flow Documentation](https://kotlinlang.org/docs/flow.html)
- [TRMNL Content Feeds](https://trmnl.com/feeds)

---

**Module Version**: 1.0.0  
**Last Updated**: October 25, 2025  
**Maintained By**: TRMNL Android Buddy Team
