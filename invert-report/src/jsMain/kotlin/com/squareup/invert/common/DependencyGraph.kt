package com.squareup.invert.common

import com.squareup.invert.common.navigation.NavRouteRepo

/**
 * REALLY HACKY Static DI Graph which is populated by [InvertReport]
 */
object DependencyGraph {
    fun initialize(
        collectedDataRepo: CollectedDataRepo,
        navRouteRepo: NavRouteRepo,
        reportDataRepo: ReportDataRepo,
    ) {
        _navRouteRepo = navRouteRepo
        _reportDataRepo = reportDataRepo
        _collectedDataRepo = collectedDataRepo
    }

    private lateinit var _navRouteRepo: NavRouteRepo
    private lateinit var _collectedDataRepo: CollectedDataRepo
    private lateinit var _reportDataRepo: ReportDataRepo

    val navRouteRepo: NavRouteRepo get() = _navRouteRepo
    val collectedDataRepo: CollectedDataRepo get() = _collectedDataRepo
    val reportDataRepo: ReportDataRepo get() = _reportDataRepo
}
