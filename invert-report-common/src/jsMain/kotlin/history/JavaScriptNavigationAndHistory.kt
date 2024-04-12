package history

import com.squareup.invert.common.Log
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.models.InvertSerialization.InvertJson
import keysForObject
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.url.URL
import org.w3c.dom.url.URLSearchParams

object JavaScriptNavigationAndHistory {
  fun URLSearchParams.toMap(): Map<String, String?> {
    val map = mutableMapOf<String, String?>()
    keysForObject(this).forEach { map[it] = this.get(it) }
    return map
  }

  fun registerForPopstate(routeManager: NavRouteManager, navRouteRepo: NavRouteRepo) {
    window.addEventListener("popstate", {
      Log.d("History Event Count: ${window.history.length}")
      if (it is PopStateEvent) {
        Log.d("Popstate ${it.state} ")
        if (it.state != null) {
          try {
            val navRouteParams =
              InvertJson.decodeFromString(HistoryState.serializer(), it.state.toString()).params
            val newRoute = routeManager.parseParamsToRoute(navRouteParams)
            MainScope().launch {
              if (navRouteRepo.navRoute.first() != newRoute) {
                navRouteRepo.updateNavRoute(newRoute)
              }
            }
          } catch (e: Throwable) {
            println("Caught e ${e.message}")
            println("${e.printStackTrace()}")
          }
        }
      }
    }, false)
  }

  fun setUrlFromNavRoute(navRoute: NavRoute) {
    val newUrl = URL(window.location.toString())

    val currUrlParamsMap = mutableMapOf<String, String>().also { urlParamsMap ->
      keysForObject(newUrl.searchParams).forEach { key ->
        newUrl.searchParams.get(key)?.let { value ->
          if (value.isNotBlank()) {
            urlParamsMap[key] = value
          }
        }
      }
    }

    val newNavRouteParamsMap = navRoute.toSearchParams()
    if (currUrlParamsMap != newNavRouteParamsMap) {
      // Clear current params from Current URL Search Params
      newUrl.search = ""

      // Populate Search Params with key/values
      newNavRouteParamsMap.forEach { (key, value) ->
        if (value.isNotBlank()) {
          newUrl.searchParams.set(key, value)
        }
      }

      // Convert params to JSON
      val jsonState = InvertJson.encodeToString(HistoryState.serializer(), HistoryState(navRoute.toSearchParams()))
      val isNewPage =
        currUrlParamsMap[BaseNavRoute.PAGE_ID_PARAM] != newNavRouteParamsMap[BaseNavRoute.PAGE_ID_PARAM]
      if (isNewPage) {
        println("window.history.pushState: ${navRoute::class.simpleName} $newNavRouteParamsMap")
        window.history.pushState(jsonState, "", newUrl.toString())
      } else {
        println("window.history.replaceState: ${navRoute::class.simpleName} $newNavRouteParamsMap")
        window.history.replaceState(jsonState, "", newUrl.toString())
      }
    }
  }


}