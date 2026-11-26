package nl.ecannemeijer.casacheck.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.SharedPayload
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.ui.add.AddPropertyScreen
import nl.ecannemeijer.casacheck.ui.compare.CompareScreen
import nl.ecannemeijer.casacheck.ui.dashboard.DashboardScreen
import nl.ecannemeijer.casacheck.ui.detail.AgentQuestionsScreen
import nl.ecannemeijer.casacheck.ui.detail.CrackEditScreen
import nl.ecannemeijer.casacheck.ui.detail.DefectEditScreen
import nl.ecannemeijer.casacheck.ui.detail.EditPropertyScreen
import nl.ecannemeijer.casacheck.ui.detail.InspectionQuestionsScreen
import nl.ecannemeijer.casacheck.ui.detail.MoistureEditScreen
import nl.ecannemeijer.casacheck.ui.detail.PropertyDetailScreen
import nl.ecannemeijer.casacheck.ui.detail.RoomEditScreen
import nl.ecannemeijer.casacheck.ui.inspection.ItemScreen
import nl.ecannemeijer.casacheck.ui.inspection.WizardScreen
import nl.ecannemeijer.casacheck.ui.photos.AnnotateScreen
import nl.ecannemeijer.casacheck.ui.photos.CameraScreen
import nl.ecannemeijer.casacheck.ui.photos.PhotoCompareScreen
import nl.ecannemeijer.casacheck.ui.photos.PhotoViewerScreen
import nl.ecannemeijer.casacheck.ui.properties.PropertyListScreen
import nl.ecannemeijer.casacheck.ui.reports.ReportsScreen
import nl.ecannemeijer.casacheck.ui.settings.PreferencesScreen
import nl.ecannemeijer.casacheck.ui.settings.SettingsScreen

/** Camera target: where a new photo belongs. */
data class CameraTarget(val propertyId: Long, val itemId: Long? = null, val roomId: Long? = null, val defectId: Long? = null,
                        val moistureId: Long? = null, val crackId: Long? = null, val kind: PhotoKind = PhotoKind.INSPECTION)

class Nav(private val c: NavHostController) {
    fun back() { c.popBackStack() }
    /** After deleting a property: pop every screen that belongs to it (detail, edit, wizard, photos, ...). */
    fun leaveProperty() {
        val inner = setOf("property", "edit", "wizard", "item", "camera", "photo", "annotate", "photocompare", "room", "defect", "moisture", "crack", "agent", "cpq", "recording")
        while (c.previousBackStackEntry != null && c.currentDestination?.route?.substringBefore('/')?.substringBefore('?') in inner) {
            if (!c.popBackStack()) break
        }
    }
    fun tab(route: String) = c.navigate(route) { popUpTo(c.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
    fun add() = c.navigate("add")
    fun property(id: Long, tab: Int = 0) = c.navigate("property/$id?tab=$tab") { launchSingleTop = true }
    fun propertyReplacingAdd(id: Long) = c.navigate("property/$id?tab=0") { popUpTo("add") { inclusive = true } }
    fun edit(id: Long) = c.navigate("edit/$id")
    fun wizard(pid: Long, mode: InspectionMode, room: Long? = null, cat: String? = null, start: Long? = null) =
        c.navigate("wizard/$pid?mode=${mode.name}&room=${room ?: -1}&cat=${cat ?: ""}&start=${start ?: -1}")
    fun item(itemId: Long) = c.navigate("item/$itemId")
    fun camera(t: CameraTarget) = c.navigate("camera/${t.propertyId}?item=${t.itemId ?: -1}&room=${t.roomId ?: -1}&defect=${t.defectId ?: -1}&moist=${t.moistureId ?: -1}&crack=${t.crackId ?: -1}&kind=${t.kind.name}")
    fun photo(pid: Long, photoId: Long, filter: String = "ALL") = c.navigate("photo/$pid/$photoId?filter=$filter")
    fun annotate(photoId: Long) = c.navigate("annotate/$photoId")
    fun photoCompare(pid: Long, a: Long = -1, b: Long = -1) = c.navigate("photocompare/$pid?a=$a&b=$b")
    fun room(pid: Long, roomId: Long = -1) = c.navigate("room/$pid?id=$roomId")
    fun defect(pid: Long, id: Long = -1) = c.navigate("defect/$pid?id=$id")
    fun moisture(pid: Long, id: Long = -1) = c.navigate("moisture/$pid?id=$id")
    fun crack(pid: Long, id: Long = -1, prev: Long = -1) = c.navigate("crack/$pid?id=$id&prev=$prev")
    fun agent(pid: Long) = c.navigate("agent/$pid")
    fun checkpointQuestions(pid: Long) = c.navigate("cpq/$pid")
    fun recording(id: Long) = c.navigate("recording/$id") { launchSingleTop = true }
    fun preferences() = c.navigate("preferences")
    fun compare(ids: List<Long>) = c.navigate("compare?ids=${ids.joinToString(",")}") { launchSingleTop = true }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)
private val tabs = listOf(
    Tab("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard),
    Tab("properties", R.string.nav_properties, Icons.Filled.HomeWork),
    Tab("compare", R.string.nav_compare, Icons.Filled.CompareArrows),
    Tab("reports", R.string.nav_reports, Icons.Filled.Description),
    Tab("settings", R.string.nav_settings, Icons.Filled.Settings),
)

private fun Long.orNull() = takeIf { it >= 0 }

@Composable
fun CasaNavHost(container: AppContainer, shared: SharedPayload?, onSharedConsumed: () -> Unit) {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val entry by controller.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore('?')
    val showBar = route in tabs.map { it.route } || route == null

    LaunchedEffect(shared) { if (shared != null) controller.navigate("add") { launchSingleTop = true } }

    val openRecordings by nl.ecannemeijer.casacheck.data.recording.RecordingNav.openProperty.collectAsState()
    LaunchedEffect(openRecordings) {
        openRecordings?.let { nav.property(it, nl.ecannemeijer.casacheck.ui.detail.TAB_RECORDINGS); nl.ecannemeijer.casacheck.data.recording.RecordingNav.openProperty.value = null }
    }

    Scaffold(bottomBar = {
        androidx.compose.foundation.layout.Column {
        nl.ecannemeijer.casacheck.ui.recording.RecordingMiniBar(insetBottom = !showBar) { pid -> nav.property(pid, nl.ecannemeijer.casacheck.ui.detail.TAB_RECORDINGS) }
        if (showBar) NavigationBar {
            tabs.forEach { t ->
                NavigationBarItem(selected = route == t.route, onClick = { nav.tab(t.route) },
                    icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.label), maxLines = 1) })
            }
        }
        }
    }) { pad ->
        NavHost(controller, startDestination = "dashboard", modifier = Modifier.padding(bottom = pad.calculateBottomPadding())) {
            composable("dashboard") { DashboardScreen(container, nav) }
            composable("properties") { PropertyListScreen(container, nav) }
            composable("compare?ids={ids}", arguments = listOf(navArgument("ids") { defaultValue = "" })) {
                CompareScreen(container, nav, it.arguments?.getString("ids").orEmpty().split(',').mapNotNull { s -> s.toLongOrNull() })
            }
            composable("reports") { ReportsScreen(container, nav, null) }
            composable("settings") { SettingsScreen(container, nav) }
            composable("preferences") { PreferencesScreen(container, nav) }
            composable("add") { AddPropertyScreen(container, nav, shared, onSharedConsumed) }
            composable("property/{id}?tab={tab}", arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
                PropertyDetailScreen(container, nav, it.arguments!!.getLong("id"), it.arguments!!.getInt("tab"))
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { EditPropertyScreen(container, nav, it.arguments!!.getLong("id")) }
            composable("wizard/{pid}?mode={mode}&room={room}&cat={cat}&start={start}", arguments = listOf(
                navArgument("pid") { type = NavType.LongType }, navArgument("mode") { defaultValue = "FULL" },
                navArgument("room") { type = NavType.LongType; defaultValue = -1L }, navArgument("cat") { defaultValue = "" },
                navArgument("start") { type = NavType.LongType; defaultValue = -1L })) {
                val a = it.arguments!!
                WizardScreen(container, nav, a.getLong("pid"), InspectionMode.valueOf(a.getString("mode") ?: "FULL"), a.getLong("room").orNull(),
                    a.getString("cat")?.ifBlank { null }, a.getLong("start").orNull())
            }
            composable("item/{itemId}", arguments = listOf(navArgument("itemId") { type = NavType.LongType })) { ItemScreen(container, nav, it.arguments!!.getLong("itemId")) }
            composable("camera/{pid}?item={item}&room={room}&defect={defect}&moist={moist}&crack={crack}&kind={kind}", arguments = listOf(
                navArgument("pid") { type = NavType.LongType }, navArgument("item") { type = NavType.LongType; defaultValue = -1L },
                navArgument("room") { type = NavType.LongType; defaultValue = -1L }, navArgument("defect") { type = NavType.LongType; defaultValue = -1L },
                navArgument("moist") { type = NavType.LongType; defaultValue = -1L }, navArgument("crack") { type = NavType.LongType; defaultValue = -1L },
                navArgument("kind") { defaultValue = "INSPECTION" })) {
                val a = it.arguments!!
                CameraScreen(container, nav, CameraTarget(a.getLong("pid"), a.getLong("item").orNull(), a.getLong("room").orNull(), a.getLong("defect").orNull(),
                    a.getLong("moist").orNull(), a.getLong("crack").orNull(), PhotoKind.valueOf(a.getString("kind") ?: "INSPECTION")))
            }
            composable("photo/{pid}/{photoId}?filter={filter}", arguments = listOf(navArgument("pid") { type = NavType.LongType },
                navArgument("photoId") { type = NavType.LongType }, navArgument("filter") { defaultValue = "ALL" })) {
                PhotoViewerScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("photoId"), it.arguments!!.getString("filter") ?: "ALL")
            }
            composable("annotate/{photoId}", arguments = listOf(navArgument("photoId") { type = NavType.LongType })) { AnnotateScreen(container, nav, it.arguments!!.getLong("photoId")) }
            composable("photocompare/{pid}?a={a}&b={b}", arguments = listOf(navArgument("pid") { type = NavType.LongType },
                navArgument("a") { type = NavType.LongType; defaultValue = -1L }, navArgument("b") { type = NavType.LongType; defaultValue = -1L })) {
                PhotoCompareScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("a").orNull(), it.arguments!!.getLong("b").orNull())
            }
            composable("room/{pid}?id={id}", arguments = listOf(navArgument("pid") { type = NavType.LongType }, navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
                RoomEditScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("id").orNull())
            }
            composable("defect/{pid}?id={id}", arguments = listOf(navArgument("pid") { type = NavType.LongType }, navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
                DefectEditScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("id").orNull())
            }
            composable("moisture/{pid}?id={id}", arguments = listOf(navArgument("pid") { type = NavType.LongType }, navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
                MoistureEditScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("id").orNull())
            }
            composable("crack/{pid}?id={id}&prev={prev}", arguments = listOf(navArgument("pid") { type = NavType.LongType },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }, navArgument("prev") { type = NavType.LongType; defaultValue = -1L })) {
                CrackEditScreen(container, nav, it.arguments!!.getLong("pid"), it.arguments!!.getLong("id").orNull(), it.arguments!!.getLong("prev").orNull())
            }
            composable("agent/{pid}", arguments = listOf(navArgument("pid") { type = NavType.LongType })) { AgentQuestionsScreen(container, nav, it.arguments!!.getLong("pid")) }
            composable("recording/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { nl.ecannemeijer.casacheck.ui.recording.RecordingScreen(container, nav, it.arguments!!.getLong("id")) }
        composable("cpq/{pid}", arguments = listOf(navArgument("pid") { type = NavType.LongType })) { InspectionQuestionsScreen(container, nav, it.arguments!!.getLong("pid")) }
        }
    }
}
