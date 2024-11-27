package com.squareup.invert.examples

import com.squareup.anvil.annotations.MergeComponent
import com.squareup.invert.examples.di.NetworkGraph
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Singleton

@Singleton
@MergeComponent(AppScope::class)
interface AppComponent {
    // TODO make this app better
    fun networking(): NetworkGraph
}
