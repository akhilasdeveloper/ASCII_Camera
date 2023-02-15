package com.akhilasdeveloper.asciicamera.di

import com.akhilasdeveloper.asciicamera.util.TextGraphicsSorter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

}
