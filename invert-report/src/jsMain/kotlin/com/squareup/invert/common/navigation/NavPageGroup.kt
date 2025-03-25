package com.squareup.invert.common.navigation

import com.squareup.invert.models.js.BuildSystem

data class NavPageGroup(
  val groupTitle: String,
  val navItems: Set<NavPage.NavItem>,
  val buildSystem: BuildSystem? = null
)