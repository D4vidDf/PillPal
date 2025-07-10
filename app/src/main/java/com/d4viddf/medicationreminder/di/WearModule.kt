package com.d4viddf.medicationreminder.di

import android.content.Context
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearModule {

    @Provides
    @Singleton
    fun provideWearConnectivityHelper(@ApplicationContext context: Context): WearConnectivityHelper {
        return WearConnectivityHelper(context)
    }
}
