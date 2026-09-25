package com.naampath.colorpath3d

import com.naampath.colorpath3d.achievements.AchievementEvaluator
import com.naampath.colorpath3d.achievements.AchievementSnapshot
import com.naampath.colorpath3d.achievements.AchievementId
import com.naampath.colorpath3d.gameplay.GameController
import com.naampath.colorpath3d.gameplay.HintTier
import com.naampath.colorpath3d.gameplay.ScoreCalculator
import com.naampath.colorpath3d.level.DailyChallengeFactory
import com.naampath.colorpath3d.level.LevelGenerator
import com.naampath.colorpath3d.level.LevelSolver
import com.naampath.colorpath3d.level.LevelValidator
import com.naampath.colorpath3d.level.WorldCatalog
import com.naampath.colorpath3d.model.ColorPair
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.Obstacle
import com.naampath.colorpath3d.model.ObstacleType
import com.naampath.colorpath3d.model.PathColor
import com.naampath.colorpath3d.model.RunStatus
import com.naampath.colorpath3d.path.CollisionManager
import com.naampath.colorpath3d.path.PathValidator
import com.naampath.colorpath3d.path.RejectReason
import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.progress.ProgressBookkeeper
import com.naampath.colorpath3d.security.ProgressToken
import com.naampath.colorpath3d.gameplay.LevelResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameplayRulesTest {
    private val generator = LevelGenerator()
    private val validator = LevelValidator()

    @Test fun pathMustStartOnItsOwnSource() {
        val level = TutorialLevels()
        val controller = GameController(level, GameMode.CLASSIC)
        assertNull(controller.pointerDown(GridPos(2, 2)))
        assertFalse(controller.snapshot().draftActive)
        controller.pointerDown(GridPos(0, 0))
        assertEquals(PathColor.RED, controller.snapshot().draftColor)
    }

    @Test fun pathMustEndOnMatchingDestination() {
        val level = TutorialLevels()
        val controller = GameController(level, GameMode.CLASSIC)
        play(controller, listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0), GridPos(3, 0), GridPos(4, 0)))
        assertEquals(1, controller.snapshot().locked.size)
        assertEquals(0, controller.snapshot().mistakes)
    }

    @Test fun wrongColorFailsGracefully() {
        val level = TutorialLevels()
        val controller = GameController(level, GameMode.CLASSIC)
        controller.pointerDown(GridPos(0, 0))
        controller.pointerMove(GridPos(0, 1))
        val reason = controller.pointerUp(GridPos(0, 4))
        assertEquals(RejectReason.WRONG_END, reason)
        assertEquals(0, controller.snapshot().locked.size)
        assertEquals(1, controller.snapshot().mistakes)
        assertEquals(RunStatus.PLAYING, controller.snapshot().status)
    }

    @Test fun pathCannotLeaveTheBoard() {
        val check = rules().isWellFormed(
            TutorialLevels(),
            PathColor.RED,
            listOf(GridPos(0, 0), GridPos(-1, 0)),
            emptySet(),
            0
        )
        assertEquals(RejectReason.OUT_OF_BOARD, check.reason)
    }

    @Test fun diagonalStepsAreRejected() {
        val check = rules().isWellFormed(
            TutorialLevels(),
            PathColor.RED,
            listOf(GridPos(0, 0), GridPos(1, 1), GridPos(4, 0)),
            emptySet(),
            0
        )
        assertEquals(RejectReason.DIAGONAL, check.reason)
    }

    @Test fun nonAdjacentStepsAreRejected() {
        val check = rules().isWellFormed(
            TutorialLevels(),
            PathColor.RED,
            listOf(GridPos(0, 0), GridPos(2, 0), GridPos(4, 0)),
            emptySet(),
            0
        )
        assertEquals(RejectReason.NOT_ADJACENT, check.reason)
    }

    @Test fun aPathCannotReuseACell() {
        val check = rules().isWellFormed(
            TutorialLevels(),
            PathColor.RED,
            listOf(GridPos(0, 0), GridPos(1, 0), GridPos(1, 1), GridPos(1, 0), GridPos(4, 0)),
            emptySet(),
            0
        )
        assertEquals(RejectReason.REVISIT, check.reason)
        assertTrue(CollisionManager().selfOverlap(listOf(GridPos(0, 0), GridPos(1, 0), GridPos(0, 0))))
    }

    @Test fun pathsCannotCross() {
        val level = TutorialLevels()
        val controller = GameController(level, GameMode.CLASSIC)
        play(controller, (0..4).map { GridPos(it, 0) })
        controller.pointerDown(GridPos(0, 4))
        val reason = controller.pointerMove(GridPos(0, 0))
        assertEquals(RejectReason.CROSSES_PATH, reason ?: controller.pointerMove(GridPos(1, 0)))
    }

    @Test fun blockedCellsRejectEntry() {
        val level = blockedLevel()
        val controller = GameController(level, GameMode.CLASSIC)
        controller.pointerDown(GridPos(0, 0))
        val reason = controller.pointerMove(GridPos(1, 0))
        assertEquals(RejectReason.BLOCKED, reason)
        assertEquals(listOf(GridPos(0, 0)), controller.snapshot().draft)
    }

    @Test fun oneWayAndBarrierRules() {
        val level = specialLevel()
        val forward = rulesFor(level)
        val ok = forward.isWellFormed(level, PathColor.RED, listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0)), emptySet(), 1)
        assertTrue(ok.ok)
        val wrongWay = forward.isWellFormed(level, PathColor.RED, listOf(GridPos(2, 0), GridPos(1, 0), GridPos(0, 0)), emptySet(), 1)
        assertEquals(RejectReason.ONE_WAY, wrongWay.reason)
        val closed = forward.isWellFormed(level, PathColor.BLUE, listOf(GridPos(0, 2), GridPos(1, 2), GridPos(2, 2)), emptySet(), 0)
        assertEquals(RejectReason.BARRIER, closed.reason)
    }

    @Test fun undoAndRestart() {
        val controller = GameController(TutorialLevels(), GameMode.CLASSIC)
        play(controller, (0..4).map { GridPos(it, 0) })
        assertTrue(controller.undo())
        assertEquals(0, controller.snapshot().locked.size)
        play(controller, (0..4).map { GridPos(it, 0) })
        controller.restart()
        assertEquals(0, controller.snapshot().moves)
        assertEquals(RunStatus.PLAYING, controller.snapshot().status)
    }

    @Test fun hintsAndCompletion() {
        val controller = GameController(TutorialLevels(), GameMode.CLASSIC)
        assertTrue(controller.hint(HintTier.NEXT_CELL))
        assertTrue(controller.snapshot().hintFlash != null)
        assertTrue(controller.hint(HintTier.COMPLETE_PAIR))
        assertEquals(1, controller.snapshot().locked.size)
        assertTrue(controller.hint(HintTier.COMPLETE_PAIR))
        assertEquals(RunStatus.COMPLETE, controller.snapshot().status)
        assertTrue(controller.snapshot().score > 0)
    }

    @Test fun perfectModeFailsOnAMistake() {
        val controller = GameController(TutorialLevels(), GameMode.PERFECT)
        controller.pointerDown(GridPos(0, 0))
        controller.pointerMove(GridPos(0, 1))
        controller.pointerUp(GridPos(1, 1))
        assertEquals(RunStatus.FAILED, controller.snapshot().status)
    }

    @Test fun timedModeExpires() {
        val controller = GameController(TutorialLevels(), GameMode.TIMED)
        controller.advance(controller.snapshot().timeLimitMs!! + 5)
        assertEquals(RunStatus.TIME_UP, controller.snapshot().status)
        controller.extendTime(10_000)
        assertEquals(RunStatus.PLAYING, controller.snapshot().status)
    }

    @Test fun scorePenaltiesAndBonuses() {
        val clean = ScoreCalculator.score(5_000, 2, 0, 0, 2, 40, 8, 8)
        val messy = ScoreCalculator.score(90_000, 8, 3, 2, 2, 40, 20, 8)
        assertTrue(clean > messy)
        assertEquals(3, ScoreCalculator.stars(2, 0, 0, 5_000, 2, 40))
        assertEquals(1, ScoreCalculator.stars(6, 2, 1, 90_000, 2, 40))
    }

    @Test fun everyCampaignLevelIsSolvable() {
        val sizes = mutableSetOf<Int>()
        for (id in 1..WorldCatalog.CAMPAIGN_SIZE) {
            val level = generator.generate(id)
            val report = validator.validate(level)
            assertTrue("level $id ${report.errors}", report.ok)
            assertTrue(level.pairs.size >= 2)
            assertEquals(level.pairs.size, level.solution.size)
            sizes += level.size
        }
        assertEquals(setOf(5, 6, 7, 8, 9, 10, 12), sizes)
    }

    @Test fun dailySeedIsStable() {
        val day = LocalDate.of(2026, 9, 25)
        val first = DailyChallengeFactory.level(day)
        val second = DailyChallengeFactory.level(day)
        assertEquals(first.solution, second.solution)
        assertEquals(DailyChallengeFactory.seed(day.toEpochDay()), DailyChallengeFactory.seed(day.toEpochDay()))
        val other = DailyChallengeFactory.level(day.plusDays(1))
        assertNotEquals(first.solution, other.solution)
    }

    @Test fun solverFindsACorridor() {
        val red = listOf(GridPos(0, 0), GridPos(1, 0))
        val blue = listOf(GridPos(0, 1), GridPos(1, 1))
        val open = (red + blue).toSet()
        val blocks = (0 until 4).flatMap { y -> (0 until 4).map { x -> GridPos(x, y) } }
            .filter { it !in open }
            .map { Obstacle(it, ObstacleType.BLOCK) }
        val level = Level(
            id = 4,
            worldId = 1,
            size = 4,
            pairs = listOf(
                ColorPair(PathColor.RED, PathColor.RED.symbol, red.first(), red.last()),
                ColorPair(PathColor.BLUE, PathColor.BLUE.symbol, blue.first(), blue.last())
            ),
            obstacles = blocks,
            solution = listOf(red, blue),
            parMoves = 2,
            parTimeSec = 30
        )
        assertNotNull(LevelSolver().solve(level))
    }

    @Test fun progressSignatureRejectsTampering() {
        val secret = "colorpath-test-key".toByteArray()
        val book = ProgressBookkeeper(secret)
        val state = book.apply(
            book.fresh(),
            LevelResult(1, GameMode.CLASSIC, 900, 3, 2, 4000, 0, 0, true),
            20
        )
        assertEquals(20, book.trusted(state).wallet.coins)
        val tampered = state.copy(wallet = state.wallet.copy(coins = 99999))
        assertEquals(0, book.trusted(tampered).wallet.coins)
        assertNull(book.spendCoins(state, 100000))
        val payload = ProgressToken.levelPayload(1, "CLASSIC", 3, 900, 4000, 2)
        assertTrue(ProgressToken.verify(secret, payload, state.levels.values.first().signature))
    }

    @Test fun achievementsUnlockOnMilestones() {
        val earned = AchievementEvaluator.unlocked(
            AchievementSnapshot(1000, 1, 1, 1, 10, 1, 100, 1)
        )
        assertTrue(AchievementId.LEVELS_1000 in earned)
        assertTrue(AchievementId.STREAK_100 in earned)
        assertTrue(AchievementId.FIRST_CONNECTION in earned)
    }

    private fun play(controller: GameController, path: List<GridPos>) {
        controller.pointerDown(path.first())
        path.drop(1).forEach { controller.pointerMove(it) }
        assertNull(controller.pointerUp(path.last()))
    }

    private fun rules(): PathValidator = PathValidator(Board3D.from(TutorialLevels()), CollisionManager())

    private fun rulesFor(level: Level) = PathValidator(Board3D.from(level), CollisionManager())

    private fun TutorialLevels() = generator.generate(1)

    private fun blockedLevel(): Level {
        val path = listOf(GridPos(0, 0), GridPos(0, 1), GridPos(0, 2), GridPos(1, 2), GridPos(2, 2))
        val other = listOf(GridPos(4, 0), GridPos(4, 1), GridPos(4, 2), GridPos(4, 3), GridPos(4, 4))
        return Level(
            id = 9001,
            worldId = 1,
            size = 5,
            pairs = listOf(
                ColorPair(PathColor.RED, PathColor.RED.symbol, path.first(), path.last()),
                ColorPair(PathColor.BLUE, PathColor.BLUE.symbol, other.first(), other.last())
            ),
            obstacles = listOf(Obstacle(GridPos(1, 0), ObstacleType.BLOCK), Obstacle(GridPos(1, 1), ObstacleType.METAL)),
            solution = listOf(path, other),
            parMoves = 2,
            parTimeSec = 40
        )
    }

    private fun specialLevel(): Level {
        val red = listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0))
        val blue = listOf(GridPos(0, 2), GridPos(1, 2), GridPos(2, 2))
        return Level(
            id = 9002,
            worldId = 4,
            size = 5,
            pairs = listOf(
                ColorPair(PathColor.RED, PathColor.RED.symbol, red.first(), red.last()),
                ColorPair(PathColor.BLUE, PathColor.BLUE.symbol, blue.first(), blue.last())
            ),
            obstacles = listOf(
                Obstacle(GridPos(1, 0), ObstacleType.ONE_WAY, oneWay = Direction.RIGHT),
                Obstacle(GridPos(1, 2), ObstacleType.BARRIER, opensAfter = 1)
            ),
            solution = listOf(red, blue),
            parMoves = 2,
            parTimeSec = 40
        )
    }
}
