package com.akhilasdeveloper.asciicamera.di

import android.content.Context
import androidx.room.Room
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctions
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctionsImpl
import com.akhilasdeveloper.asciicamera.repository.room.ASCIIDatabase
import com.akhilasdeveloper.asciicamera.util.ColorSorter
import com.akhilasdeveloper.asciicamera.util.Constants.ASCII_DB_NAME
import com.akhilasdeveloper.asciicamera.util.TextGraphicsSorter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTextGraphicsSorter(): TextGraphicsSorter {
        return TextGraphicsSorter()
    }

    @Singleton
    @Provides
    fun provideColorSorter(): ColorSorter {
        return ColorSorter()
    }

    @Singleton
    @Provides
    fun provideDataStoreFunctions( @ApplicationContext app: Context): DataStoreFunctions {
        return DataStoreFunctionsImpl(app)
    }

    @Singleton
    @Provides
    fun provideASCIIDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        ASCIIDatabase::class.java,
        ASCII_DB_NAME
    ).fallbackToDestructiveMigration().build()


    @Singleton
    @Provides
    fun provideFilterSpecsDao(db: ASCIIDatabase) = db.getFilterSpecsDao()

}
