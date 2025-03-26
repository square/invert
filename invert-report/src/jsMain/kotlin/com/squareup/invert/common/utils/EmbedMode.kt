package com.squareup.invert.common.utils

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList

object EmbedMode {

  private fun getCssPropertyByClass(className: String, property: String): String? {
    document.querySelectorAll(".$className").asList().forEach { element ->
      return (element as? HTMLElement)?.style?.getPropertyValue(property)
    }
    return null
  }

  private fun updateCssByClass(className: String, property: String, value: String) {
    document.querySelectorAll(".$className").asList().forEach { element ->
      (element as? HTMLElement)?.style?.setProperty(property, value)
    }
  }

  private fun removeCssByClass(className: String, property: String) {
    document.querySelectorAll(".$className").asList().forEach { element ->
      (element as? HTMLElement)?.style?.removeProperty(property)
    }
  }

  data class OriginalCssValue(
    val className: String,
    val propertyName: String,
  ) {
    val value = getCssPropertyByClass(className, propertyName)
  }

  val originalValues: List<OriginalCssValue> by lazy {
    mapOf<String, String>(
      "dashboard-sidebar" to "display",
      "navbar" to "display",
      "main-content" to "margin-left"
    ).map { (cssClass, cssProperty) ->
      OriginalCssValue(cssClass, cssProperty)
    }
  }

  val embedModeValues: Map<OriginalCssValue, String> = originalValues.associate {
    it to when (it.className) {
      "dashboard-sidebar" -> "none"
      "navbar" -> "none"
      "main-content" -> "0px"
      else -> error("Unknown css class name: ${it.className}")
    }
  }

  fun enableEmbedMode() {
    embedModeValues.forEach { entry ->
      updateCssByClass(entry.key.className, entry.key.propertyName, entry.value)
    }
  }

  fun disableEmbedMode() {
    originalValues.forEach { entry ->
      if (entry.value.isNullOrEmpty()) {
        removeCssByClass(entry.className, entry.propertyName)
      } else {
        updateCssByClass(entry.className, entry.propertyName, entry.value)
      }
    }
  }
}
