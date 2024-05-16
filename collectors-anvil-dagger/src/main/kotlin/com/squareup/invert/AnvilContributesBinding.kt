//package com.squareup.invert
//
//import kotlinx.serialization.Serializable
//
///**
//* Represents the data in an Anvil ContributesBinding Annotation Usage
//*/
//@Serializable
//data class AnvilContributesBinding(
//    val annotation: String,
//    val scope: String,
//    val boundImplementation: String,
//    val boundType: String,
//    val replaces: List<String>,
//)
//
///**
// * Represents the data in an Anvil ContributesBinding Annotation Usage
// */
//@Serializable
//data class AnvilInjection(
//    val type: String,
//    val qualifierAnnotations: List<String>
//)
//
//@Serializable
//data class AnvilContributionAndConsumption(
//    val classFqName: String,
//    val contributions: List<AnvilContributesBinding>,
//    val consumptions: List<AnvilInjection>,
//    val fileName: String,
//    val lineNumber: Int,
//)