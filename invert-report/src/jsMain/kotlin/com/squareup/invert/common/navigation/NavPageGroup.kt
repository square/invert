package com.squareup.invert.common.navigation

data class NavPageGroup(
  val groupTitle: String,
  val navItems: Set<NavPage.NavItem>,
)