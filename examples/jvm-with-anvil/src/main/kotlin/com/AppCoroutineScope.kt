package com
import javax.inject.Qualifier

/** Identifies a coroutine scope type that is scope to the app lifecycle */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class AppCoroutineScope
