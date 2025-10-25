package ink.trmnl.android.buddy.di

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.api.TrmnlApiClient
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.ContentDatabase
import ink.trmnl.android.buddy.data.database.BatteryHistoryDao
import ink.trmnl.android.buddy.data.database.TrmnlDatabase

@ContributesTo(AppScope::class)
interface AppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideTrmnlApiService(
        @ApplicationContext context: Context,
    ): TrmnlApiService {
        // Create API service with debug logging in debug builds
        val isDebug = BuildConfig.DEBUG
        val appVersion = BuildConfig.VERSION_NAME
        return TrmnlApiClient.create(isDebug = isDebug, appVersion = appVersion)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideTrmnlDatabase(
        @ApplicationContext context: Context,
    ): TrmnlDatabase = TrmnlDatabase.getInstance(context)

    @Provides
    fun provideBatteryHistoryDao(database: TrmnlDatabase): BatteryHistoryDao = database.batteryHistoryDao()

    // Content module bindings
    @Provides
    @SingleIn(AppScope::class)
    fun provideContentDatabase(
        @ApplicationContext context: Context,
    ): ContentDatabase =
        Room
            .databaseBuilder(
                context,
                ContentDatabase::class.java,
                "trmnl_content.db",
            ).addMigrations(
                ContentDatabase.MIGRATION_1_2,
                ContentDatabase.MIGRATION_2_3,
            ).build()

    @Provides
    fun provideAnnouncementDao(database: ContentDatabase): AnnouncementDao = database.announcementDao()

    @Provides
    fun provideBlogPostDao(database: ContentDatabase): BlogPostDao = database.blogPostDao()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnnouncementRepository(announcementDao: AnnouncementDao): ink.trmnl.android.buddy.content.repository.AnnouncementRepository =
        ink.trmnl.android.buddy.content.repository
            .AnnouncementRepository(announcementDao)

    @Provides
    @SingleIn(AppScope::class)
    fun provideBlogPostRepository(blogPostDao: BlogPostDao): ink.trmnl.android.buddy.content.repository.BlogPostRepository =
        ink.trmnl.android.buddy.content.repository
            .BlogPostRepository(blogPostDao)

    @Provides
    @SingleIn(AppScope::class)
    fun provideContentFeedRepository(
        announcementDao: AnnouncementDao,
        blogPostDao: BlogPostDao,
    ): ink.trmnl.android.buddy.content.repository.ContentFeedRepository =
        ink.trmnl.android.buddy.content.repository
            .ContentFeedRepository(announcementDao, blogPostDao)
}
