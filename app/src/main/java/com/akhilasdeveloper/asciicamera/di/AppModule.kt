package com.akhilasdeveloper.asciicamera.di

import android.content.Context
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.room.Room
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctions
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctionsImpl
import com.akhilasdeveloper.asciicamera.repository.room.ASCIIDatabase
import com.akhilasdeveloper.asciicamera.util.ColorSorter
import com.akhilasdeveloper.asciicamera.util.Constants.ASCII_DB_NAME
import com.akhilasdeveloper.asciicamera.util.TextGraphicsSorter
import com.akhilasdeveloper.asciicamera.util.Utilities
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import com.google.common.util.concurrent.ListenableFuture
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
    fun provideAsciiGenerator(): AsciiGenerator {
        return AsciiGenerator()
    }

    @Singleton
    @Provides
    fun provideCameraProcessProvider(@ApplicationContext app: Context): ListenableFuture<ProcessCameraProvider> {
        return ProcessCameraProvider.getInstance(app)
    }

    @Singleton
    @Provides
    fun provideProcessCameraProvider(cameraProcessProvider: ListenableFuture<ProcessCameraProvider>): ProcessCameraProvider {
        return cameraProcessProvider.get()
    }

    @Singleton
    @Provides
    fun provideImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(Surface.ROTATION_90)
            .build()
    }

    @Singleton
    @Provides
    fun provideDataStoreFunctions( @ApplicationContext app: Context): DataStoreFunctions {
        return DataStoreFunctionsImpl(app)
    }

    @Singleton
    @Provides
    fun provideUtilities( @ApplicationContext app: Context, textGraphicsSorter: TextGraphicsSorter): Utilities {
        return Utilities(app, textGraphicsSorter)
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
