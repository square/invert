package com.squareup.invert.examples

import com.squareup.anvil.annotations.MergeComponent
import com.squareup.invert.examples.di.NetworkGraph
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Singleton

@Singleton
@MergeComponent(AppScope::class)
interface AppComponent {
    fun networking(): NetworkGraph
}
