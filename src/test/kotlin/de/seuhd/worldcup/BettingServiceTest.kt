package de.seuhd.worldcup

import kotlin.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows

class BettingServiceTest {

    private fun match(id: Int, home: String, away: String, hs: Int?, aws: Int?) =
        Match(
            matchId = id,
            round = "Matchday 1",
            date = "2026-06-01",
            homeTeam = home,
            awayTeam = away,
            homeScore = hs,
            awayScore = aws,
            ground = "Test Stadium"
        )

    @BeforeTest
    fun resetBets() {
        BettingService.clear()
    }

    // ── evaluateBonus ──────────────────────────────────────────────────────────

    @Test
    fun `evaluateBonus awards 3 points for an exact score prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 2, 1))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN, 2, 1))
        assertEquals(3, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
        // Branch 1: Outcome correct, but score mismatch (e.g., 2:0 actual vs 2:1 predicted)
        val matches = listOf(match(1, "AAA", "BBB", 2, 0))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN, 2, 1))
        assertEquals(1, BettingService.evaluateBonus(matches))

        // Branch 2: Outcome correct, but NO score prediction provided at all
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))
        assertEquals(1, BettingService.evaluateBonus(matches))

    }

    @Test
    fun `evaluateBonus awards 0 points for a wrong prediction`() {
        val matches = listOf(match(1, "AAA", "BBB", 0, 2))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN, 1, 0))
        assertEquals(0, BettingService.evaluateBonus(matches))
    }

    @Test
    fun `evaluateBonus ignores unplayed matches`() {
        val matches = listOf(match(1, "AAA", "BBB", null, null))
        BettingService.placeBet(Bet(1, Prediction.DRAW, 0, 0))
        assertEquals(0, BettingService.evaluateBonus(matches))

        val playedMatches = listOf(match(99, "CCC", "DDD", 1, 1))
        assertEquals(0, BettingService.evaluateBonus(playedMatches))
    }

    // ── removeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `removeBet removes an existing bet so it no longer affects evaluation`() {
        val matches = listOf(match(1, "AAA", "BBB", 1, 0))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))
        BettingService.removeBet(1)

        val result = BettingService.evaluate(matches)
        assertEquals(0, result.evaluated)
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        // Branch: If branch (bets.containsKey) evaluates to false
        BettingService.removeBet(999)
        assertEquals(0, BettingService.evaluate(emptyList()).evaluated)
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        val matches = listOf(match(1, "AAA", "BBB", 0, 0))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))
        BettingService.changeBet(Bet(1, Prediction.DRAW))

        // Branch Coverage for Cache: Call evaluate twice
        BettingService.evaluate(matches) // First call calculates and sets cachedResult
        val secondResult = BettingService.evaluate(matches) // Hits cachedResult?.let { return it }

        assertEquals(1, secondResult.correct)
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        // Branch: !bets.containsKey(bet.matchId) is true -> throws exception
        val orphanBet = Bet(999, Prediction.HOME_WIN)
        assertThrows<IllegalArgumentException> {
            BettingService.changeBet(orphanBet)
        }
    }
}