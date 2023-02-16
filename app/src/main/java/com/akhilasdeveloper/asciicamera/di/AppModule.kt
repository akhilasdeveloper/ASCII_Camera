package com.akhilasdeveloper.asciicamera.di

import android.content.Context
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctions
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctionsImpl
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
    fun provideDataStoreFunctions( @ApplicationContext app: Context): DataStoreFunctions {
        return DataStoreFunctionsImpl(app)
    }

}
