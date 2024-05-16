package com

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import scopes.AppScope


@Module
@ContributesTo(AppScope::class)
object ProcessNameModule {

    @Provides
    @ProcessName
    fun provideProcessName(): String {
        return "main"
    }

    @Provides
    @IsMainProcess
    fun providerIsMainProcess(@ProcessName processName: String): Boolean {
        return processName == "main"
    }
}