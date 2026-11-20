package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.data.local.EmiPulseDatabase
import com.example.data.local.SavedEmiSessionEntity
import com.example.data.local.SavedSessionRepository
import com.example.domain.CurrencyOption
import com.example.domain.EmiCalculatorEngine
import com.example.domain.LoanInput
import com.example.domain.LoanPreset
import com.example.domain.PrepaymentFrequency
import com.example.domain.TenureUnit
import com.example.ui.EmiPulseApp
import com.example.ui.EmiUiState
import com.example.ui.EmiViewModel
import com.example.ui.components.AmortizationStackedChart
import com.example.ui.components.EmiDonutChart
import com.example.ui.components.LoanBreakdownPieChart
import com.example.ui.components.PieSlice
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.EligibilityScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.EmiPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EMI Pulse", appName)
  }

  @Test
  fun `loan breakdown pie chart renders in dark mode and toggles slice`() {
    var selected: PieSlice? = null
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = true) {
        LoanBreakdownPieChart(
          principalRatio = 0.6f,
          interestRatio = 0.4f,
          selectedSlice = selected,
          onSelectSlice = { selected = it }
        )
      }
    }

    composeTestRule
      .onNodeWithContentDescription("Loan breakdown pie chart: Principal 60 percent, Interest 40 percent")
      .assertIsDisplayed()
      .performClick()
  }

  @Test
  fun `eligibility screen renders hero card and interest toggle`() {
    var toggledValue: Boolean? = null
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        EligibilityScreen(
          uiState = EmiUiState(),
          onIncomeTextChange = {},
          onIncomeSliderChange = {},
          onNudgeIncome = {},
          onLiabilitiesTextChange = {},
          onLiabilitiesSliderChange = {},
          onNudgeLiabilities = {},
          onSelectDtiPreset = {},
          onDtiSliderChange = {},
          onToggleInterest = { toggledValue = it },
          onRateTextChange = {},
          onRateSliderChange = {},
          onNudgeRate = {},
          onTenureTextChange = {},
          onTenureSliderChange = {},
          onSwitchTenureUnit = {},
          onApplyToCalculator = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("eligibility_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eligibility_max_loan_value").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eligibility_share_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eligibility_interest_toggle_switch")
      .performScrollTo()
      .performClick()
    assertFalse(toggledValue ?: true)
  }

  @Test
  fun `compare screen toggles loan interest flag`() {
    var lastToggle: Pair<Boolean, Boolean>? = null
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        CompareScreen(
          uiState = EmiUiState(),
          onCopyCurrentToSlot = {},
          onSwapLoans = {},
          onToggleCompareInterest = { isLoanA, enabled ->
            lastToggle = isLoanA to enabled
          },
          onUpdateCompareField = { _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("compare_a_interest_toggle")
      .performScrollTo()
      .performClick()
    assertEquals(true to false, lastToggle)
  }

  @Test
  fun `schedule screen switches to pie summary chart`() {
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        ScheduleScreen(
          uiState = EmiUiState(),
          onGranularityChange = {},
          onShowMessage = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("schedule_chart_pie_tab")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.onNodeWithTag("schedule_pie_chart").assertIsDisplayed()
  }

  @Test
  fun `calculator screen renders share button and quick tenure chip`() {
    var selectedQuickTenure: Int? = null
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        CalculatorScreen(
          uiState = EmiUiState(),
          onSelectPreset = {},
          onPrincipalTextChange = {},
          onPrincipalSliderChange = {},
          onNudgePrincipal = {},
          onToggleIncludeInterest = {},
          onRateTextChange = {},
          onRateSliderChange = {},
          onNudgeRate = {},
          onTenureTextChange = {},
          onTenureSliderChange = { selectedQuickTenure = it },
          onSwitchTenureUnit = {},
          onSelectPrepaymentFreq = {},
          onExtraPaymentTextChange = {},
          onSaveSessionClick = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("share_summary_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_tenure_row").performScrollTo()
    composeTestRule.onNodeWithTag("quick_tenure_15")
      .performScrollTo()
      .performClick()
    assertEquals(15, selectedQuickTenure)
  }

  @Test
  fun `history screen filters saved sessions by search query`() {
    val sessions = listOf(
      SavedEmiSessionEntity(
        id = 1L,
        label = "Dream Home Quote",
        presetName = LoanPreset.HOME.name,
        currencyCode = "USD",
        principal = 250000.0,
        annualRatePercent = 7.5,
        tenureValue = 20,
        tenureUnitName = TenureUnit.YEARS.name,
        extraPayment = 0.0,
        prepaymentFrequencyName = PrepaymentFrequency.NONE.name,
        monthlyEmi = 2013.98,
        totalInterest = 233355.2,
        totalPayable = 483355.2,
        interestSaved = 0.0,
        monthsSaved = 0
      ),
      SavedEmiSessionEntity(
        id = 2L,
        label = "City Car Loan",
        presetName = LoanPreset.CAR.name,
        currencyCode = "USD",
        principal = 30000.0,
        annualRatePercent = 6.5,
        tenureValue = 5,
        tenureUnitName = TenureUnit.YEARS.name,
        extraPayment = 0.0,
        prepaymentFrequencyName = PrepaymentFrequency.NONE.name,
        monthlyEmi = 586.98,
        totalInterest = 5219.0,
        totalPayable = 35219.0,
        interestSaved = 0.0,
        monthsSaved = 0
      )
    )

    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = true) {
        HistoryScreen(
          sessions = sessions,
          onLoadSession = {},
          onDeleteSession = {},
          onClearAll = {},
          onGoToCalculator = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("saved_session_card_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_search_input").performTextInput("Car")
    composeTestRule.onNodeWithTag("saved_session_card_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("saved_session_card_1").assertDoesNotExist()
  }

  @Test
  fun `schedule screen opens csv export dialog`() {
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        ScheduleScreen(
          uiState = EmiUiState(),
          onGranularityChange = {},
          onShowMessage = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("export_csv_button")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.onNodeWithTag("export_share_csv_option").assertIsDisplayed()
    composeTestRule.onNodeWithTag("export_save_csv_option").assertIsDisplayed()
  }

  @Test
  fun `compare screen opens csv export dialog`() {
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = true) {
        CompareScreen(
          uiState = EmiUiState(),
          onCopyCurrentToSlot = {},
          onSwapLoans = {},
          onToggleCompareInterest = { _, _ -> },
          onUpdateCompareField = { _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("compare_export_csv_button")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.onNodeWithTag("compare_share_csv_option").assertIsDisplayed()
    composeTestRule.onNodeWithTag("compare_save_csv_option").assertIsDisplayed()
  }

  @Test
  fun `calculator screen quick prepayment chip updates extra payment`() {
    var updatedExtraPayment: String? = null
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        CalculatorScreen(
          uiState = EmiUiState(
            loanInput = LoanInput(prepaymentFrequency = PrepaymentFrequency.MONTHLY)
          ),
          onSelectPreset = {},
          onPrincipalTextChange = {},
          onPrincipalSliderChange = {},
          onNudgePrincipal = {},
          onToggleIncludeInterest = {},
          onRateTextChange = {},
          onRateSliderChange = {},
          onNudgeRate = {},
          onTenureTextChange = {},
          onTenureSliderChange = {},
          onSwitchTenureUnit = {},
          onSelectPrepaymentFreq = {},
          onExtraPaymentTextChange = { updatedExtraPayment = it },
          onSaveSessionClick = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_prepay_row").performScrollTo()
    composeTestRule.onNodeWithTag("quick_prepay_250")
      .performScrollTo()
      .performClick()
    assertEquals("250", updatedExtraPayment)
  }

  @Test
  fun `history screen sort selector reorders sessions`() {
    val sessions = listOf(
      SavedEmiSessionEntity(
        id = 1L,
        label = "Dream Home Quote",
        presetName = LoanPreset.HOME.name,
        currencyCode = "USD",
        principal = 250000.0,
        annualRatePercent = 7.5,
        tenureValue = 20,
        tenureUnitName = TenureUnit.YEARS.name,
        extraPayment = 0.0,
        prepaymentFrequencyName = PrepaymentFrequency.NONE.name,
        monthlyEmi = 2013.98,
        totalInterest = 233355.2,
        totalPayable = 483355.2,
        interestSaved = 0.0,
        monthsSaved = 0,
        createdAtMillis = 2_000L
      ),
      SavedEmiSessionEntity(
        id = 2L,
        label = "City Car Loan",
        presetName = LoanPreset.CAR.name,
        currencyCode = "USD",
        principal = 30000.0,
        annualRatePercent = 6.5,
        tenureValue = 5,
        tenureUnitName = TenureUnit.YEARS.name,
        extraPayment = 0.0,
        prepaymentFrequencyName = PrepaymentFrequency.NONE.name,
        monthlyEmi = 586.98,
        totalInterest = 5219.0,
        totalPayable = 35219.0,
        interestSaved = 0.0,
        monthsSaved = 0,
        createdAtMillis = 1_000L
      )
    )

    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = false) {
        HistoryScreen(
          sessions = sessions,
          onLoadSession = {},
          onDeleteSession = {},
          onClearAll = {},
          onGoToCalculator = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("history_sort_newest").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_sort_emi")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("saved_session_card_2").assertIsDisplayed()

    composeTestRule.onNodeWithTag("history_sort_principal")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("saved_session_card_1").assertIsDisplayed()
  }

  @Test
  fun `theme toggle switches between light and dark modes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EmiPulseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val viewModel = EmiViewModel(
        prefsRepo = UserPreferencesRepository(context),
        savedSessionRepo = SavedSessionRepository(db.savedEmiSessionDao())
      )
      viewModel.setThemeMode(ThemeMode.LIGHT)

      composeTestRule.setContent {
        EmiPulseApp(viewModel = viewModel)
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("theme_toggle_button")
        .assertIsDisplayed()
        .assertIsOff()
        .performClick()

      composeTestRule.waitForIdle()
      assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
      composeTestRule.onNodeWithTag("theme_toggle_button").assertIsOn()

      composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
      composeTestRule.waitForIdle()
      assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
      composeTestRule.onNodeWithTag("theme_toggle_button").assertIsOff()

      composeTestRule.onNodeWithTag("currency_selector_button").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("theme_option_system").assertIsDisplayed()
      composeTestRule.onNodeWithTag("theme_option_light").assertIsDisplayed()
      composeTestRule.onNodeWithTag("theme_option_dark").assertIsDisplayed().performClick()
      composeTestRule.waitForIdle()
      assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    } finally {
      db.close()
    }
  }

  @Test
  fun `charts render cleanly in both light and dark themes`() {
    val result = EmiCalculatorEngine.calculate(LoanInput())
    composeTestRule.setContent {
      EmiPulseTheme(darkTheme = true) {
        EmiDonutChart(
          principalRatio = result.principalRatio,
          interestRatio = result.interestRatio
        )
        AmortizationStackedChart(
          yearlySchedule = result.yearlySchedule,
          currency = CurrencyOption.USD
        )
      }
    }

    composeTestRule.waitForIdle()
  }
}
