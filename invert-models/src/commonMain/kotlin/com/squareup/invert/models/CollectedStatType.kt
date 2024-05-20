package com.squareup.invert.models

enum class CollectedStatType {
  BOOLEAN,
  NUMERIC,
  STRING, // Become Explore
  CODE_REFERENCES, // EXPLORE (CUSTOM)
  DI_PROVIDES_AND_INJECTS, // EXPLORE (CUSTOM)
}
