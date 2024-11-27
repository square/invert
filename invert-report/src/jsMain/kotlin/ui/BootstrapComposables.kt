package ui

import PagingConstants
import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.squareup.invert.common.utils.CsvFileDownloadUtil
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.list
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.dom.Ul
import org.w3c.dom.HTMLAnchorElement
import kotlin.math.abs
import kotlin.random.Random
import kotlin.reflect.KClass

@Composable
fun AppLink(
  attrs: AttrBuilderContext<HTMLAnchorElement>? = null,
  content: ContentBuilder<HTMLAnchorElement>? = null
) {
  A("javascript:;", attrs, content)
}

@Composable
fun BootstrapTableHeaders(
  headerText: List<String>,
  sortedIdx: Int,
  ascending: Boolean,
  rows: List<List<String>>,
  enableSorting: Boolean,
  onHeaderSortClicked: (Int) -> Unit,
) {
  Thead {
    Tr({
      classes("table-row")
    }) {
      headerText.forEachIndexed { idx, text ->
        Th {
          Text(text)
          Text(" ")
          if (enableSorting) {
            val iconSlug = if (sortedIdx == idx) {
              if (ascending) {
                "caret-down-fill"
              } else {
                "caret-up-fill"
              }
            } else {
              if (ascending) {
                "caret-down"
              } else {
                "caret-up"
              }
            }
            BootstrapIcon(iconSlug, 16) {
              onHeaderSortClicked(idx)
            }
          }
          Text(" ")
          Button({
            classes("btn")
            title("Copy All ${rows.size} Column Values")
          }) {
            BootstrapIcon("copy", 16) {
              val columnValues = rows.map { it[idx] }
              window.navigator.clipboard.writeText(
                columnValues.joinToString(
                  separator = "\n"
                )
              )
            }
          }
        }
      }
    }
  }
}

class BootstrapTabData(
  val tabHeader: @Composable () -> Unit,
  val content: @Composable () -> Unit
) {
  constructor(tabName: String, content: @Composable () -> Unit) : this(@Composable {
    Text(tabName)
  }, content)
}

@Composable
fun BootstrapTabPane(
  tabs: List<BootstrapTabData>,
) {
  val activeIdx = 0
  val tabIds = tabs.map { "tab-pane-" + abs(Random.nextInt()) }
  Ul({
    classes("nav", "nav-tabs")
    attr("role", "tablist")
  }) {
    tabs.forEachIndexed { idx, tab ->
      Li({
        classes("nav-item")
        attr("role", "presentation")
      }) {
        Button({
          classes(mutableListOf("nav-link").apply {
            if (activeIdx == idx) {
              add("active")
            }
          })
          id("home-tab")
          attr("data-bs-target", "#${tabIds[idx]}")
          attr("data-bs-toggle", "tab")
          attr("aria-selected", "true")
          attr("role", "tab")
          attr("aria-controls", tabIds[idx])
        }) {
          tab.tabHeader()
        }
      }
    }
  }

  tabs.forEachIndexed { idx, tab ->
    Div({
      classes("tab-content")
    }) {
      Div({
        classes(mutableListOf("tab-pane", "fade", "show").apply {
          if (activeIdx == idx) {
            add("active")
          }
        })
        id(tabIds[idx])
      }) { tab.content() }

    }
  }
}

@Composable
fun BootstrapTableRow(
  cellValues: List<String>,
  onClicked: ((cellIdx: Int) -> Unit)?,
) {
  Tr({
  }) {
    cellValues.forEachIndexed { idx, cellValue ->
      Td({
        if (idx == 0) {
          onClicked?.let {
            onClick { onClicked(idx) }
            style {
              cursor("pointer")
            }
          }
        }
      }) {
        MarkdownText(cellValue)
      }
    }
  }
}

@Composable
fun BootstrapClickableList(
  header: String? = null,
  items: Collection<String>,
  maxResults: Int = MAX_RESULTS,
  onItemClick: (String) -> Unit,
) {
  BootstrapTable(
    headers = mutableListOf<String>().apply {
      if (header != null) {
        add(header)
      }
    },
    rows = items.map { listOf(it) },
    types = listOf(String::class),
    maxResultsLimitConstant = maxResults,
    onItemClickCallback = { cells ->
      onItemClick(cells[0])
    },
  )
}

interface MarkdownCellContent {}


@Composable
fun BootstrapTable(
  headers: List<String>,
  rows: List<List<String>>,
  /** If null, no sorting. */
  types: List<KClass<*>>? = headers.map { String::class },
  maxResultsLimitConstant: Int = PagingConstants.MAX_RESULTS,
  sortByColumn: Int = 0,
  sortAscending: Boolean = true,
  onItemClickCallback: ((cells: List<String>) -> Unit)? = null,
) {
  val enableSorting = types != null
  if (rows.isEmpty()) {
    return
  }
  var maxNumberOfRows by remember { mutableStateOf(maxResultsLimitConstant) }
  Table({
    classes("table table-bordered table-striped table-hover table-sm".split(" "))
  }) {
    var sortIdx by remember { mutableStateOf(sortByColumn) }
    var ascending by remember { mutableStateOf(sortAscending) }

    val sortedRows = if (enableSorting && types != null) {
      val typeToSortBy = types[sortIdx]
      val sorted1 = when (typeToSortBy) {
        Int::class -> {
          rows.sortedBy { columns ->
            columns[sortIdx].toInt()
          }
        }

        Boolean::class -> {
          rows.sortedBy { columns ->
            columns[sortIdx].toBoolean()
          }
        }

        else -> {
          rows.sortedBy { columns ->
            columns[sortIdx]
          }
        }
      }
      if (ascending) {
        sorted1
      } else {
        sorted1.reversed()
      }
    } else {
      rows
    }
    BootstrapTableHeaders(headers, sortIdx, ascending, sortedRows, enableSorting) { idx ->
      ascending = !ascending
      sortIdx = idx
    }
    Tbody {
      val sortedTrimmedRows = sortedRows.subList(0, minOf(sortedRows.size, maxNumberOfRows))
      sortedTrimmedRows.forEachIndexed { idx, cellValues ->
        BootstrapTableRow(cellValues, onClicked = { idx ->
          onItemClickCallback?.invoke(cellValues)
        })
      }
    }
  }
  BootstrapRow {
    BootstrapColumn(10) {
      if (rows.size > maxNumberOfRows) {
        P { Text("NOTE: ${rows.size} Total Results, but only $maxNumberOfRows shown for performance reasons.") }
        BootstrapButton("Show More") {
          maxNumberOfRows += maxResultsLimitConstant
        }
        Br { }
      }
    }
    BootstrapColumn(2) {
      Div({
        classes("text-end")
      }) {
        BootstrapColumn {
          Button({
            classes("btn")
            title("Download Table as CSV")
          }) {
            BootstrapIcon("download", 16) {
              val csvString = CsvFileDownloadUtil.createCsvString(rows)
              CsvFileDownloadUtil.downloadCsvFile(csvString)
            }
          }
        }
      }
    }
  }
}

@Composable
fun GenericList(
  title: String, items: List<String>, onItemClick: (String) -> Unit = { idx ->
    window.alert("Clicked: $idx")
  }
) {
  BootstrapClickableList(
    header = title,
    items = items,
    maxResults = MAX_RESULTS,
    onItemClick = onItemClick
  )
}


@Composable
fun BootstrapNavSectionHeader(title: String, iconSlug: String? = null) {
  H6({
    classes(
      "sidebar-heading d-flex justify-content-between px-3 mt-4 mb-1 text-muted"
        .split(" ")
    )
  }) {
    Text(title.uppercase())
    iconSlug?.let {
      A("#", {
        classes("d-flex text-muted".split(" "))
      }) {
        BootstrapIcon(iconSlug) {}
      }
    }
  }
}


@Composable
fun BootstrapJumbotron(
  centered: Boolean = false,
  paddingNum: Int = 4,
  headerContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  Div({
    classes("p-${paddingNum} mb-4 bg-body-tertiary rounded-3".split(" ").toMutableList().apply {
      if (centered) {
        add("text-center")
      }
    })
  }) {
    Div({
      classes("container-fluid py-3".split(" "))
    }) {
      H1({
        classes("display-5 fw-bold text-center".split(" "))
      }) { headerContent() }
      P({
        classes(listOf("fs-5"))
      }) { content() }
    }
  }
}

enum class BootstrapButtonType(val styleClass: String) {
  PRIMARY("btn-primary"),
  LINK("btn-link"),
  SECONDARY("btn-secondary");
}

@Composable
fun BootstrapButton(text: String, buttonType: BootstrapButtonType = BootstrapButtonType.PRIMARY, onClick: () -> Unit) {
  Button({
    classes("btn", buttonType.styleClass)
    onClick {
      onClick()
    }
  }) {
    Text(text)
  }
}

@Composable
fun BootstrapSettingsCheckbox(
  labelText: String,
  initialIsChecked: Boolean,
  onChecked: (Boolean) -> Unit
) {
  val id = "pluginCheck" + Random.nextInt()
  Div({ classes("form-check") }) {
    CheckboxInput {
      classes("form-check-input")
      id(id)
      onChange {
        onChecked(it.value)
      }
      checked(initialIsChecked)
    }
    Label(forId = id, { classes("form-check-label") }) {
      Text(labelText)
    }
  }
}

@Composable
fun BootstrapIcon(iconSlug: String, size: Int = 16, onClicked: (() -> Unit)? = null) {
  Img("https://icons.getbootstrap.com/assets/icons/${iconSlug}.svg", iconSlug) {
    style {
      property("width", "${size}px")
      property("height", "${size}px")
      onClicked?.let {
        cursor("pointer")
      }
    }
    classes("bootstrap-icon")
    onClicked?.let {
      onClick { onClicked() }
    }
  }

}

@Composable
fun BootstrapNavItem(text: String, iconSlug: String, onClick: () -> Unit, activeTab: Boolean = false) {
  Li({
    classes("nav-item")
  }) {
    Button({
      classes(mutableListOf<String>().apply {
        add("nav-link")
        add("gap-3")
        if (activeTab) {
          add("active")
          attr("aria-current", "page")
        }
      })
      onClick { onClick() }
    }) {
      BootstrapIcon(iconSlug) {}
      Span({
        classes("ps-2")
      }) {
        Text(text)
      }
    }
  }
}

@Composable
fun BootstrapAccordion(
  headerContent: @Composable () -> Unit,
  expanded: Boolean = false,
  bodyContent: @Composable () -> Unit,
) {
  val randomInt = Random.nextInt()
  val accordionId = "accordion${randomInt}"
  Div({
    classes("accordion".split(" "))
  }) {
    Div({
      classes("accordion-item".split(" "))
    }) {
      H2({
        classes("accordion-header")
      }) {
        Button({
          val classes = mutableListOf<String>("accordion-button").apply { if (!expanded) add("collapsed") }
          classes(classes)
          attr("data-bs-toggle", "collapse")
          attr("data-bs-target", "#$accordionId")
          attr("aria-expanded", expanded.toString())
          attr("aria-controls", accordionId)
        }) {
          headerContent()
        }
      }
      Div({
        val accordionCollapseClasses = mutableListOf<String>("accordion-collapse", "collapse").apply {
          if (expanded) {
            add("show")
          }
        }
        classes(accordionCollapseClasses)
        id(accordionId)
      }) {
        Div({
          classes("accordion-body".split(" "))
        }) {
          bodyContent()
        }
      }
    }
  }
}

@Composable
fun BootstrapSearchBox(
  query: String?,
  placeholderText: String,
  dataListId: String? = null,
  textUpdated: (String) -> Unit
) {
  TextInput(query ?: "") {
    dataListId?.let {
      list(dataListId)
    }
    placeholder(placeholderText)
    classes("form-control", "form-control-lg")
    this.onInput {
      textUpdated(it.value)
    }
  }
}

data class BootstrapSelectOption(
  val value: String,
  val displayText: String
)

@Composable
fun BootstrapSelectDropdown(
  placeholderText: String,
  options: List<BootstrapSelectOption>,
  currentValue: String?,
  onValueChange: (BootstrapSelectOption?) -> Unit
) {
  Select({
    classes(listOf("form-select"))
    attr("aria-label", placeholderText)
    currentValue?.let {
      attr("value", currentValue)
    }
    onChange { event -> onValueChange(options.firstOrNull { event.value == it.value }) }
  }) {
    Option(value = "") {
      Text(placeholderText)
    }
    options.forEach { option ->
      Option(value = option.value, {
        if (option.value == currentValue) {
          selected()
        }
      }) {
        Text(option.displayText)
      }
    }
  }
}

@Composable
fun BootstrapProgressBar(progressPercent: Int) {
  Div({
    classes("progress", "col-md-4")
    attr("role", "progressbar")
    attr("aria-valuenow", progressPercent.toString())
    attr("aria-valuemin", "0")
    attr("aria-valuemax", "100")
  }) {
    Div({
      classes("progress-bar", "bg-info", "w-$progressPercent")
    }) {
      Text("$progressPercent%")
    }
  }
}

@Composable
fun BootstrapLoadingMessageWithSpinner(text: String = "Loading...") {
  H3 {
    Text(text)
    Text(" ")
    BootstrapLoadingSpinner()
  }

}

@Composable
fun BootstrapLoadingSpinner() {
  Div({
    classes("spinner-border")
    attr("role", "status")
  }) {
    Span({
      classes("visually-hidden")
    }) {
      Text("Loading...")
    }
  }
}

@Composable
fun BootstrapCardTitle(
  classes: List<String> = emptyList(),
  content: @Composable () -> Unit
) {
  Div({
    classes(classes + "card-title")
  }) {
    content()
  }
}

@Composable
fun BootstrapCardBody(
  classes: List<String> = emptyList(),
  content: @Composable () -> Unit
) {
  Div({
    classes(classes + "card-body")
  }) {
    content()
  }
}

@Composable
fun BootstrapCard(
  classes: List<String> = emptyList(),
  content: @Composable () -> Unit
) {
  Div({
    classes(classes + "card")
  }) {
    content()
  }
}

@Composable
fun BootstrapRow(
  classes: List<String> = emptyList(),
  content: @Composable () -> Unit
) {
  Div({
    classes(classes + "row")
  }) {
    content()
  }
}

@Composable
fun BootstrapColumn(
  columnCount: Int = 12,
  type: String = "md",
  classes: List<String> = emptyList(),
  content: @Composable () -> Unit
) {
  Div({
    classes(classes + "col-$type-$columnCount")
  }) {
    content()
  }
}

@Composable
fun TitleRow(title: String) {
  BootstrapRow {
    BootstrapColumn {
      H2 { Text(title) }
    }
  }
}

@Composable
fun BoostrapExpandingCard(
  header: @Composable () -> Unit,
  headerRight: @Composable () -> Unit = {},
  expanded: Boolean,
  content: @Composable () -> Unit
) {
  val randomInt = Random.nextInt()
  val collapseId = "collapse${randomInt}"
  Div({ classes("card") }) {
    Div({ classes("card-header") }) {
      H6({ classes("mb-0") }) {
        Button({
          classes(
            mutableListOf(
              "btn", "btn-link"
            ).apply {
              if (!expanded) {
                add("collapsed")
              }
            })
          attr("data-bs-toggle", "collapse")
          attr("href", "#$collapseId")
          attr("aria-controls", collapseId)
          attr("aria-expanded", expanded.toString())
        }) {
          header()
        }
        headerRight()
      }
    }
    Div({
      id(collapseId)
      classes(
        mutableListOf(
          "collapsed"
        ).apply {
          if (expanded) {
            add("show")
          } else {
            add("collapse")
          }
        })
    }) {
      Div({ classes("card-body") }) {
        content()
      }
    }
  }
}

@Composable
fun BoostrapExpandingSection(
  headerText: String,
  expanded: Boolean,
  content: @Composable () -> Unit
) {
  val randomInt = Random.nextInt()
  val collapseId = "collapse${randomInt}"
  H6({ classes("mb-0") }) {
    Button({
      classes(
        mutableListOf(
          "btn"
        ).apply {
          if (!expanded) {
            add("collapsed")
          }
        })
      attr("data-bs-toggle", "collapse")
      attr("aria-controls", collapseId)
      attr("aria-expanded", expanded.toString())
    }) {
      Text(headerText)
    }
  }
  Div({
    id(collapseId)
    classes(
      mutableListOf(
        "collapsed"
      ).apply {
        if (expanded) {
          add("show")
        } else {
          add("collapse")
        }
      })
  }) {
    content()
  }
}