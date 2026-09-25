package com.naampath.colorpath3d.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naampath.colorpath3d.R
import com.naampath.colorpath3d.game.GameCommand
import com.naampath.colorpath3d.gameplay.HintTier
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.progress.ProgressBookkeeper
import com.naampath.colorpath3d.ui.AchievementsViewModel
import com.naampath.colorpath3d.ui.DailyViewModel
import com.naampath.colorpath3d.ui.GameplayViewModel
import com.naampath.colorpath3d.ui.LeaderboardViewModel
import com.naampath.colorpath3d.ui.LevelSelectViewModel
import com.naampath.colorpath3d.ui.MenuViewModel
import com.naampath.colorpath3d.ui.SessionFlags
import com.naampath.colorpath3d.ui.SettingsViewModel
import com.naampath.colorpath3d.ui.ShopViewModel
import com.naampath.colorpath3d.ui.ShellViewModel
import com.naampath.colorpath3d.ui.StatsViewModel
import com.naampath.colorpath3d.ui.screens.AchievementsScreen
import com.naampath.colorpath3d.ui.screens.DailyScreen
import com.naampath.colorpath3d.ui.screens.GameplayScreen
import com.naampath.colorpath3d.ui.screens.LeaderboardScreen
import com.naampath.colorpath3d.ui.screens.LevelCompleteScreen
import com.naampath.colorpath3d.ui.screens.LevelGridScreen
import com.naampath.colorpath3d.ui.screens.MainMenuScreen
import com.naampath.colorpath3d.ui.screens.PauseScreen
import com.naampath.colorpath3d.ui.screens.PrivacyScreen
import com.naampath.colorpath3d.ui.screens.SettingsScreen
import com.naampath.colorpath3d.ui.screens.ShopScreen
import com.naampath.colorpath3d.ui.screens.SplashScreen
import com.naampath.colorpath3d.ui.screens.StatisticsScreen
import com.naampath.colorpath3d.ui.screens.TutorialScreen
import com.naampath.colorpath3d.ui.screens.WorldSelectScreen
import com.naampath.colorpath3d.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val playModes = listOf(GameMode.CLASSIC, GameMode.TIMED, GameMode.MOVES, GameMode.PERFECT, GameMode.ZEN, GameMode.HARD)

@Composable
fun AppNav(onGdxVisible: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val shell: ShellViewModel = hiltViewModel()
    val splashDone by shell.host.splash.collectAsState()
    val settings by shell.settings.collectAsState()
    var gdx by remember { mutableStateOf(true) }
    LaunchedEffect(splashDone) {
        if (splashDone && nav.currentDestination?.route == "splash") {
            delay(250)
            nav.navigate("menu") { popUpTo("splash") { inclusive = true } }
        }
    }
    DisposableEffect(gdx) {
        onGdxVisible(gdx)
        onDispose { }
    }
    NavHost(nav, startDestination = "splash") {
        composable("splash") {
            gdx = true
            SessionFlags.gameplayVisible = false
            Box(Modifier.fillMaxSize().background(Color.Transparent)) {
                SplashScreen(true)
            }
        }
        composable("menu") {
            gdx = false
            SessionFlags.gameplayVisible = false
            SessionFlags.allowAppOpen = true
            val menu: MenuViewModel = hiltViewModel()
            val progress by menu.progress.collectAsState()
            val tuning = menu.tuning()
            Box(Modifier.fillMaxSize().background(Ink)) {
                MainMenuScreen(
                    coins = progress.wallet.coins,
                    endlessEnabled = tuning.endlessEnabled,
                    dailyEnabled = tuning.dailyEnabled,
                    showBanner = !progress.wallet.adsRemoved && !progress.wallet.premium,
                    onPlay = {
                        shell.click()
                        val done = progress.levels.values.filter { it.mode == GameMode.CLASSIC.name && it.completed }.map { it.levelId }.toSet()
                        val next = (1..1000).firstOrNull { it !in done } ?: 1000
                        if (!progress.tutorialDone && next == 1) nav.navigate("tutorial")
                        else nav.navigate("game/${GameMode.CLASSIC.name}/$next")
                    },
                    onDaily = { shell.click(); nav.navigate("daily") },
                    onLevels = { shell.click(); nav.navigate("levels") },
                    onEndless = {
                        shell.click()
                        nav.navigate("game/${GameMode.ENDLESS.name}/${progress.endlessIndex}")
                    },
                    onAchievements = { shell.click(); nav.navigate("achievements") },
                    onLeaderboard = { shell.click(); nav.navigate("leaderboard") },
                    onSettings = { shell.click(); nav.navigate("settings") },
                    onShop = { shell.click(); nav.navigate("shop") },
                    onStats = { shell.click(); nav.navigate("stats") }
                )
            }
        }
        composable("tutorial") {
            gdx = false
            var step by remember { mutableIntStateOf(0) }
            Box(Modifier.fillMaxSize().background(Ink)) {
                TutorialScreen(step) {
                    shell.click()
                    if (step >= 4) nav.navigate("game/${GameMode.CLASSIC.name}/1")
                    else step += 1
                }
            }
        }
        composable("levels") {
            gdx = false
            val vm: LevelSelectViewModel = hiltViewModel()
            val progress by vm.progress.collectAsState()
            Box(Modifier.fillMaxSize().background(Ink)) {
                WorldSelectScreen(
                    worlds = vm.worlds(),
                    cleared = { world ->
                        progress.levels.values.count {
                            it.completed && it.mode == GameMode.CLASSIC.name && it.levelId in world.firstLevel..world.lastLevel
                        }
                    },
                    onWorld = { nav.navigate("world/${it.id}") },
                    onBack = { nav.popBackStack() }
                )
            }
        }
        composable("world/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
            gdx = false
            val vm: LevelSelectViewModel = hiltViewModel()
            val id = entry.arguments?.getInt("id") ?: 1
            val world = vm.worlds().first { it.id == id }
            var modeIndex by remember { mutableIntStateOf(0) }
            val mode = playModes[modeIndex]
            Box(Modifier.fillMaxSize().background(Ink)) {
                LevelGridScreen(
                    world = world,
                    modeLabel = mode.name,
                    onCycleMode = { modeIndex = (modeIndex + 1) % playModes.size },
                    unlocked = { level -> mode != GameMode.CLASSIC || vm.unlocked(level) },
                    record = { level -> vm.progress.value.levels[ProgressBookkeeper.key(level, mode.name)] },
                    onLevel = { level -> nav.navigate("game/${mode.name}/$level") },
                    onBack = { nav.popBackStack() }
                )
            }
        }
        composable(
            "game/{mode}/{levelId}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("levelId") { type = NavType.IntType }
            )
        ) { entry ->
            gdx = true
            SessionFlags.gameplayVisible = true
            val mode = GameMode.valueOf(entry.arguments?.getString("mode") ?: GameMode.CLASSIC.name)
            val levelId = entry.arguments?.getInt("levelId") ?: 1
            val vm: GameplayViewModel = hiltViewModel()
            val hud by vm.hud.collectAsState()
            val result by vm.result.collectAsState()
            val activity = LocalContext.current.findActivity()
            var paused by remember { mutableStateOf(false) }
            var hintOpen by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(mode, levelId) { vm.start(mode, levelId) }
            Box(Modifier.fillMaxSize()) {
                GameplayScreen(
                    title = if (mode == GameMode.DAILY) "DAILY" else if (mode == GameMode.ENDLESS) "ENDLESS ${levelId + 1}" else "LEVEL $levelId",
                    state = hud,
                    cameraRotation = settings.cameraRotation,
                    onCommand = {
                        shell.click()
                        vm.command(it)
                    },
                    onBack = {
                        vm.command(GameCommand.Pause)
                        scope.launch {
                            activity?.let { vm.onLeave(it, hud?.draftActive == true) }
                            nav.popBackStack()
                        }
                    },
                    onPause = {
                        paused = true
                        vm.command(GameCommand.Pause)
                    },
                    onHint = { hintOpen = true }
                )
                if (paused) {
                    PauseScreen(
                        onResume = { paused = false; vm.command(GameCommand.Resume) },
                        onRestart = { paused = false; vm.command(GameCommand.Restart) },
                        onSettings = { nav.navigate("settings") },
                        onQuit = { nav.popBackStack(); nav.popBackStack() }
                    )
                }
                if (hintOpen && activity != null) {
                    HintDialog(
                        onTier = { tier ->
                            hintOpen = false
                            vm.hint(tier, activity)
                        },
                        onAd = { tier ->
                            hintOpen = false
                            vm.rewardedHint(tier, activity)
                        },
                        onTime = { vm.rewardedTime(activity); hintOpen = false },
                        onClose = { hintOpen = false }
                    )
                }
                val finished = result
                if (finished != null) {
                    val best = vm.campaign.value.levels[ProgressBookkeeper.key(finished.levelId, finished.mode.name)]?.bestScore ?: finished.score
                    LevelCompleteScreen(
                        result = finished,
                        bestScore = best,
                        onNext = {
                            vm.finishTutorial()
                            scope.launch {
                                activity?.let { vm.onLeave(it, false) }
                                val next = if (mode == GameMode.ENDLESS) levelId + 1 else (levelId + 1).coerceAtMost(1000)
                                nav.navigate("game/${mode.name}/$next") {
                                    popUpTo("game/{mode}/{levelId}") { inclusive = true }
                                }
                            }
                        },
                        onRetry = { vm.start(mode, levelId) },
                        onSelect = { nav.popBackStack() }
                    )
                }
            }
        }
        composable("settings") {
            gdx = false
            val vm: SettingsViewModel = hiltViewModel()
            val state by vm.settings.collectAsState()
            Box(Modifier.fillMaxSize().background(Ink)) {
                SettingsScreen(state, vm::update, { nav.navigate("privacy") }, { nav.popBackStack() })
            }
        }
        composable("privacy") {
            gdx = false
            Box(Modifier.fillMaxSize().background(Ink)) { PrivacyScreen { nav.popBackStack() } }
        }
        composable("shop") {
            gdx = false
            val vm: ShopViewModel = hiltViewModel()
            val state by vm.wallet.collectAsState()
            val ready by vm.billingReady.collectAsState()
            val message by vm.billingMessage.collectAsState()
            val activity = LocalContext.current.findActivity()
            Box(Modifier.fillMaxSize().background(Ink)) {
                ShopScreen(
                    state = state,
                    billingReady = ready,
                    message = message,
                    onBuy = { product -> activity?.let { vm.buy(it, product) } },
                    onRestore = { vm.restore() },
                    onTheme = { id, price, owned ->
                        if (owned) vm.selectTheme(id) else vm.buyTheme(id, price)
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }
        composable("achievements") {
            gdx = false
            val vm: AchievementsViewModel = hiltViewModel()
            val unlocked by vm.unlocked.collectAsState()
            Box(Modifier.fillMaxSize().background(Ink)) {
                AchievementsScreen(unlocked, vm.catalog) { nav.popBackStack() }
            }
        }
        composable("stats") {
            gdx = false
            val vm: StatsViewModel = hiltViewModel()
            val state by vm.progress.collectAsState()
            Box(Modifier.fillMaxSize().background(Ink)) { StatisticsScreen(state) { nav.popBackStack() } }
        }
        composable("daily") {
            gdx = false
            val vm: DailyViewModel = hiltViewModel()
            val state by vm.progress.collectAsState()
            Box(Modifier.fillMaxSize().background(Ink)) {
                DailyScreen(state, vm.reward(), { nav.navigate("game/${GameMode.DAILY.name}/0") }, { nav.popBackStack() })
            }
        }
        composable("leaderboard") {
            gdx = false
            val vm: LeaderboardViewModel = hiltViewModel()
            val rows by vm.rows.collectAsState()
            val note by vm.note.collectAsState()
            val activity = LocalContext.current.findActivity()
            LaunchedEffect(Unit) { vm.load("CLASSIC") }
            Box(Modifier.fillMaxSize().background(Ink)) {
                LeaderboardScreen(
                    rows = rows,
                    note = note,
                    onBoard = { vm.load(it) },
                    onSync = { activity?.let { vm.sync(it, R.string.leaderboard_high_score) } },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun HintDialog(onTier: (HintTier) -> Unit, onAd: (HintTier) -> Unit, onTime: () -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        com.naampath.colorpath3d.ui.components.NeonCard {
            androidx.compose.foundation.layout.Column {
                com.naampath.colorpath3d.ui.components.NeonButton("HIGHLIGHT NEXT CELL") { onTier(HintTier.NEXT_CELL) }
                com.naampath.colorpath3d.ui.components.NeonButton("SHOW PART OF A PATH") { onTier(HintTier.PARTIAL_PATH) }
                com.naampath.colorpath3d.ui.components.NeonButton("COMPLETE ONE PAIR") { onTier(HintTier.COMPLETE_PAIR) }
                com.naampath.colorpath3d.ui.components.NeonButton("WATCH AD FOR HINT") { onAd(HintTier.NEXT_CELL) }
                com.naampath.colorpath3d.ui.components.NeonButton("WATCH AD FOR TIME") { onTime() }
                com.naampath.colorpath3d.ui.components.NeonButton("CLOSE", onClick = onClose)
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
