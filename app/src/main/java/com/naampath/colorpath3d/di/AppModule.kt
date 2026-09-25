package com.naampath.colorpath3d.di

import android.content.Context
import androidx.room.Room
import com.naampath.colorpath3d.achievements.AchievementManager
import com.naampath.colorpath3d.ads.AdManager
import com.naampath.colorpath3d.audio.MusicManager
import com.naampath.colorpath3d.audio.SoundManager
import com.naampath.colorpath3d.billing.BillingRepository
import com.naampath.colorpath3d.data.ProgressRepository
import com.naampath.colorpath3d.data.SettingsRepository
import com.naampath.colorpath3d.data.db.AppDatabase
import com.naampath.colorpath3d.firebase.AnalyticsManager
import com.naampath.colorpath3d.firebase.CrashlyticsManager
import com.naampath.colorpath3d.firebase.RemoteConfigManager
import com.naampath.colorpath3d.haptics.HapticPlayer
import com.naampath.colorpath3d.leaderboard.LeaderboardManager
import com.naampath.colorpath3d.level.LevelRepository
import com.naampath.colorpath3d.level.ProceduralLevelRepository
import com.naampath.colorpath3d.security.SecurityKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "colorpath.db").build()

    @Provides @Singleton
    fun settings(@ApplicationContext context: Context) = SettingsRepository(context)

    @Provides @Singleton
    fun keys(@ApplicationContext context: Context) = SecurityKeyProvider(context)

    @Provides @Singleton
    fun progress(database: AppDatabase, settings: SettingsRepository, keys: SecurityKeyProvider) =
        ProgressRepository(database, settings, keys)

    @Provides @Singleton
    fun levels(): LevelRepository = ProceduralLevelRepository()

    @Provides @Singleton
    fun crashlytics() = CrashlyticsManager()

    @Provides @Singleton
    fun analytics(@ApplicationContext context: Context, crashlytics: CrashlyticsManager) =
        AnalyticsManager(context, crashlytics)

    @Provides @Singleton
    fun remote(crashlytics: CrashlyticsManager) = RemoteConfigManager(crashlytics)

    @Provides @Singleton
    fun sounds(@ApplicationContext context: Context) = SoundManager(context)

    @Provides @Singleton
    fun music(@ApplicationContext context: Context) = MusicManager(context)

    @Provides @Singleton
    fun haptics(@ApplicationContext context: Context) = HapticPlayer(context)

    @Provides @Singleton
    fun ads(
        @ApplicationContext context: Context,
        progress: ProgressRepository,
        remote: RemoteConfigManager,
        analytics: AnalyticsManager
    ) = AdManager(context, progress, remote, analytics)

    @Provides @Singleton
    fun billing(
        @ApplicationContext context: Context,
        progress: ProgressRepository,
        analytics: AnalyticsManager
    ) = BillingRepository(context, progress, analytics)

    @Provides @Singleton
    fun leaderboards(@ApplicationContext context: Context) = LeaderboardManager(context)

    @Provides @Singleton
    fun achievements(progress: ProgressRepository) = AchievementManager(progress)
}
