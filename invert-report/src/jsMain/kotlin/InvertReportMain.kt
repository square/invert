import com.squareup.invert.common.InvertReport
import com.squareup.invert.common.navigation.NavRoute

fun main() {
    InvertReport<NavRoute>(customReportPages = reportPages)
}