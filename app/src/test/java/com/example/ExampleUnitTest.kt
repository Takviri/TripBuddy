package com.example

import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `epochDay round trip and Indonesian date formatting are accurate`() {
    val epochDay = DateUtils.epochDayFromYmd(2026, 10, 25) // 25 November 2026
    val (year, month, day) = DateUtils.ymdFromEpochDay(epochDay)
    assertEquals(2026, year)
    assertEquals(10, month)
    assertEquals(25, day)
    assertEquals("November 2026", DateUtils.formatMonthYear(2026, 10))
    assertEquals("25 Nov 2026", DateUtils.formatIndonesianShortDate(epochDay))
    assertEquals("Rabu, 25 November 2026", DateUtils.formatIndonesianFullDate(epochDay))
  }

  @Test
  fun `countdown calculation returns expected days hours minutes seconds`() {
    val todayDay = DateUtils.epochDayFromYmd(2026, 10, 25)
    val nowMillis = DateUtils.targetMillisForVisit(todayDay, 10, 0)
    val targetDay = todayDay + 2L

    val countdown = DateUtils.computeCountdownParts(
      visitEpochDay = targetDay,
      visitHour = 13,
      visitMinute = 30,
      nowMillis = nowMillis
    )

    assertEquals(2L, countdown.days)
    assertEquals(3L, countdown.hours)
    assertEquals(30L, countdown.minutes)
    assertEquals(0L, countdown.seconds)
    assertFalse(countdown.isToday)
    assertFalse(countdown.isTomorrow)
  }

  @Test
  fun `leap year February has 29 days`() {
    assertEquals(29, DateUtils.daysInMonth(2024, 1))
    assertEquals(28, DateUtils.daysInMonth(2026, 1))
    assertTrue(DateUtils.firstDayOfWeekInMonth(2026, 10) in 0..6)
  }
}
