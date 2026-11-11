# Rate Details Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tapping a currency in the rates list opens a screen with its current rate against the base, the
last 30 days of rates and the change over the period, with loading / error / no-data states and back.

**Architecture:** A new screen `feature/rates/details/` built exactly like the reference screen
`feature/rates/list/` (MVIKotlin Store → UiStateConverter → UiComponent → Compose). Data comes from a new
method on the existing `RatesRepository`, backed by the existing `FrankfurterApi.getTimeSeries`. The root
Decompose stack gets a new `Config`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Decompose 3.5, MVIKotlin, kotlin-inject, Ktor
(+ `ktor-client-mock` in tests), kotlinx.serialization, kotlinx-datetime, bignum, kotest.

**Spec:** `docs/superpowers/specs/2026-11-11-rate-details-screen-design.md` — read it first.

## Global Constraints

- **Before writing any code read `AGENTS.md`, `docs/architecture.md`, `docs/screen.md`,
  `docs/conventions.md`, `docs/commands.md`.** The work is judged on following them. Copy the shape of the
  matching file in `feature/rates/list/` (and its tests) for every new screen file.
- All code in `shared/src/commonMain`, tests in `shared/src/commonTest`. No new dependencies.
- Rates and everything derived from them are `BigDecimal`; numbers become strings only through
  `core/format/NumberFormatter`; the change only through `core/money/RateChangeCalculator`.
- No `it` in lambdas; `sealed class`, not `sealed interface`; `String.Empty` instead of `""` in State,
  UiState and converters; empty line after `{` and before `}` of a class body; lines ≤ 120.
- User-facing strings only in `ui/Strings.kt`; spacing/colours only from `AppTheme`.
- kotest `FunSpec`, fakes not mocks, test names describe behaviour.
- Each task ends with `./gradlew :shared:testAndroidHostTest detekt` green (from the repo root
  `/Users/lindevhard/Develop/testwork/fx-rates-test/.claude/worktrees/currency-rate-screen-752b80`), then
  **one commit** with an English message: a short subject (what) and a body (why). No attribution lines.
  Commit only the files of the task.

## Review Focus

1. A 404 from Frankfurter for an empty window must become an empty list ("No data"), but a 500 or a
   network failure must still be the error state with Retry — Task 2 tests both.
2. A history with one point: current rate and one history row shown, change says "not enough data" — Task 4.
3. An earliest rate of zero (`RateChangeCalculator` returns `null`) must not crash and shows "not enough
   data" — Task 4.
4. A change that rounds to zero shows `0.0000` / `0.00%` without a sign and is neutral (`Trend.Flat`) — Task 4.
5. Rotation must not reload the history (Store retained via `instanceKeeper.getStore`), and Retry after an
   error reloads with the same base and code — Tasks 3 and 5.

---

### Task 1: Share `Trend` between rates screens

**Files:**
- Create: `shared/src/commonMain/kotlin/com/fxrates/feature/rates/model/Trend.kt`
- Create: `shared/src/commonMain/kotlin/com/fxrates/feature/rates/model/TrendColor.kt`
- Modify: `shared/src/commonMain/kotlin/com/fxrates/feature/rates/list/model/RateItem.kt` (remove `Trend`)
- Modify: `RatesListUiStateConverter.kt`, `RatesListUiContent.kt`, `RatesListUiStatePreviewProvider.kt`,
  `RatesListUiStateConverterTest.kt` (imports; converter uses `toTrend()`; content drops its private `color()`)
- Test: `shared/src/commonTest/kotlin/com/fxrates/feature/rates/model/TrendTest.kt`

**Interfaces:**
- Produces: `enum class Trend { Up, Down, Flat }` in package `com.fxrates.feature.rates.model`;
  `fun RateChange?.toTrend(): Trend` (null or zero absolute → `Flat`, positive → `Up`, else `Down`);
  `@Composable internal fun Trend.color(): Color` mapping to `AppTheme.colors.positive/negative/neutral`.

- [ ] **Step 1: Write `TrendTest`** with tests `"no change is flat"` (`null.toTrend() shouldBe Trend.Flat` and
  `RateChange(0.toBigDecimal(), 0.toBigDecimal()).toTrend() shouldBe Trend.Flat`), `"a rise is up"`,
  `"a fall is down"` (use `RateChangeCalculator.calculate("0.88".toBigDecimal(), "0.89".toBigDecimal())` and the reverse).
- [ ] **Step 2:** Run `./gradlew :shared:testAndroidHostTest` — expect compile failure (`toTrend` missing).
- [ ] **Step 3:** Move `Trend`, add `toTrend()` and `color()`, switch the list converter's inline `when` to
  `change.toTrend()` and the list content to the shared `color()`.
- [ ] **Step 4:** Run `./gradlew :shared:testAndroidHostTest detekt` — all green, list tests unchanged in behaviour.
- [ ] **Step 5: Commit** — e.g. "Share the rate trend between rates screens" (body: the details screen needs
  the same trend rule and colours; one definition instead of two).

### Task 2: Rate history in the data layer

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fxrates/data/remote/FrankfurterApi.kt`
- Create: `shared/src/commonMain/kotlin/com/fxrates/domain/model/RatePoint.kt`
- Modify: `domain/repository/RatesRepository.kt`, `data/repository/RatesConverter.kt`,
  `data/repository/RatesRepositoryImpl.kt`
- Modify: `shared/src/commonTest/kotlin/com/fxrates/testing/FakeRatesRepository.kt`, `testing/TestData.kt`
- Test: `commonTest/.../data/repository/RatesConverterTest.kt` (extend),
  `commonTest/.../data/repository/RatesRepositoryImplTest.kt` (new)

**Interfaces:**
- Produces:
  - `FrankfurterApi.getTimeSeries(base: String, start: LocalDate, end: LocalDate? = null, symbols: String? = null)`
    — adds `parameter("symbols", symbols)` only when not null.
  - `data class RatePoint(val date: LocalDate, val rate: BigDecimal)` in `com.fxrates.domain.model`
    (KDoc: `rate` = units of the currency one unit of the base buys).
  - `RatesRepository.getRateHistory(base: String, code: String): List<RatePoint>` — last 30 calendar days,
    ascending by date, empty when the API has none.
  - `RatesConverter.toRateHistory(timeSeries: TimeSeriesDto, code: String): List<RatePoint>`.
  - `RatesRepositoryImpl`: `HISTORY_DAYS = 30`; start = `clock.todayIn(TimeZone.UTC).minus(DatePeriod(days = HISTORY_DAYS))`;
    `ClientRequestException` with `response.status == HttpStatusCode.NotFound` → `emptyList()`, anything else rethrown.
  - Test helpers: `FakeRatesRepository.rateHistory: Result<List<RatePoint>> = Result.success(emptyList())`,
    `FakeRatesRepository.requestedHistories: MutableList<Pair<String, String>>` (base to code);
    `fun ratePoint(date: LocalDate = LocalDate(2026, 11, 10), rate: String = "0.89"): RatePoint` in `TestData.kt`.

- [ ] **Step 1: Write tests.**
  - `RatesConverterTest`: `"the history keeps the dates that have the currency, oldest first"` — rates for
    `2026-11-10 → EUR 0.89`, `2026-10-13 → EUR 0.88`, `2026-10-20 → USD-only map without EUR` →
    `listOf(RatePoint(2026-10-13, "0.88"), RatePoint(2026-11-10, "0.89"))`; `"an empty time series gives an empty history"`.
  - `RatesRepositoryImplTest` (new): build `FrankfurterApi(HttpClient(MockEngine { … }) { expectSuccess = true;
    defaultRequest { url(FrankfurterBaseUrl) }; install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } })`,
    a fixed `Clock` returning `2026-11-11T12:00:00Z`, real `RatesConverter`. Tests:
    `"the history is requested for one currency over the last 30 days"` (request URL path ends with
    `2026-10-12..`, parameters `base=USD`, `symbols=EUR`; response with two dates → two points ascending);
    `"a window without data gives an empty history"` (respond `404` with `{"message":"not found"}` → `emptyList()`);
    `"a server failure is not hidden"` (respond `500` → `shouldThrow<ServerResponseException>`).
- [ ] **Step 2:** Run tests — expect compile failure.
- [ ] **Step 3:** Implement the interfaces above; add the fake/test-data helpers.
- [ ] **Step 4:** Run `./gradlew :shared:testAndroidHostTest detekt` — green.
- [ ] **Step 5: Commit** in two commits, in this order:
  1. `FrankfurterApi.kt` only — "Let the time series request ask for chosen currencies" (why: the history
     needs one currency, not all of them). It must compile on its own.
  2. Everything else — "Load the rate history of one currency" (why: the details screen shows 30 days of
     rates; a 404 for an empty window means no data, not a failure).

### Task 3: Rate details Store

**Files (create, in `shared/src/commonMain/kotlin/com/fxrates/feature/rates/details/mvi/`):**
`RateDetailsStore.kt`, `RateDetailsReducer.kt`, `RateDetailsExecutor.kt`, `RateDetailsStoreFactory.kt`
- Test: `shared/src/commonTest/kotlin/com/fxrates/feature/rates/details/mvi/RateDetailsStoreTest.kt`

**Interfaces:**
- Consumes: `RatesRepository.getRateHistory`, `RatePoint`, `FakeRatesRepository.rateHistory/requestedHistories`, `ratePoint()`.
- Produces (`internal`, mirrors `RatesListStore`):
  - `Intent`: `data object OnRetry`, `data object OnBackClick`
  - `Action`: `data class LoadHistory(val base: String, val code: String)`
  - `Message`: `data class UpdateHistory(val history: Resource<List<RatePoint>>)`
  - `Label`: `data object Back`
  - `State(val base: String, val code: String, val history: Resource<List<RatePoint>> = Resource.Loading)`
  - `RateDetailsStoreFactory(storeFactory: StoreFactory, repository: RatesRepository)` with
    `fun createStore(base: String, code: String): RateDetailsStore`, name `"RateDetailsStore"`,
    `SimpleBootstrapper(Action.LoadHistory(base, code))`.
  - Executor: `OnRetry` → `forward(Action.LoadHistory(base = state().base, code = state().code))`;
    `OnBackClick` → `publish(Label.Back)`; `loadHistory(action)` via `resourceFlow { … }.onEach { dispatch(…) }.launchIn(scope)`.

- [ ] **Step 1: Write `RateDetailsStoreTest`** (copy the setup of `RatesListStoreTest`):
  `"the history is loaded for the currency and base on start"` (`requestedHistories shouldBe listOf("USD" to "EUR")`,
  `state.history shouldBe Resource.Data(listOf(ratePoint()))`);
  `"a failed load ends in an error and a retry loads again"`;
  `"a back click publishes the back label"`.
- [ ] **Step 2:** Run — expect compile failure.
- [ ] **Step 3:** Implement the four files.
- [ ] **Step 4:** `./gradlew :shared:testAndroidHostTest detekt` — green.
- [ ] **Step 5: Commit** — "Add the rate details Store".

### Task 4: Rate details UiState and converter

**Files:**
- Create in `feature/rates/details/`: `model/HistoryItem.kt`, `RateDetailsUiState.kt`, `RateDetailsUiStateConverter.kt`
- Modify: `shared/src/commonMain/kotlin/com/fxrates/ui/Strings.kt`
- Test: `commonTest/.../feature/rates/details/RateDetailsUiStateConverterTest.kt`

**Interfaces:**
- Produces:
  - `@Immutable data class HistoryItem(val date: String, val rate: String)`
  - ```kotlin
    @Immutable
    internal data class RateDetailsUiState(
        val title: String = String.Empty,
        val content: Content = Content.Loading,
    ) {
        sealed class Content { Loading; Error; NoData; data class Data(val rate: String, val date: String, val change: Change, val history: List<HistoryItem>) }
        sealed class Change { data object NotEnoughData; data class Value(val period: String, val absolute: String, val percent: String, val trend: Trend) }
    }
    ```
    (each nested type `@Immutable` where the list's are; `data object` for the parameterless ones.)
  - `@Inject internal class RateDetailsUiStateConverter { fun createUiState(state: RateDetailsStore.State): RateDetailsUiState }`
  - `Strings`: `RATE_HISTORY_TITLE = "Last 30 days"`, `NO_DATA = "No data for the period"`,
    `NOT_ENOUGH_DATA = "Not enough data to calculate the change"`, `BACK = "Back"`,
    `fun rateDetailsTitle(code: String, base: String) = "$code / $base"`,
    `fun currentRate(base: String, rate: String, code: String) = "1 $base = $rate $code"`,
    `fun changePeriod(start: String, end: String) = "Change $start – $end"`.
- Rules: title always `"EUR / USD"`; current rate = last point; change = `RateChangeCalculator.calculate(first.rate, last.rate)`
  only when there are ≥ 2 points and the result is not null, else `NotEnoughData`; history newest first;
  dates as `LocalDate.toString()`.

- [ ] **Step 1: Write `RateDetailsUiStateConverterTest`** (State base `USD`, code `EUR`):
  - `"loading and error map to their states"`; `"the title names the currency and the base"` → `"EUR / USD"`.
  - `"no points give the no-data state"` → `Content.NoData`.
  - `"one point shows the rate and says there is not enough data"` → `rate == "1 USD = 0.8900 EUR"`,
    `date == "2026-11-10"`, `change == Change.NotEnoughData`, one history item.
  - `"the change over the period is signed and relative to the first point"` — points 2026-10-13 0.88,
    2026-10-20 0.885, 2026-11-10 0.89 → `Change.Value(period = "Change 2026-10-13 – 2026-11-10",
    absolute = "+0.0100", percent = "+1.14%", trend = Trend.Up)`.
  - `"a fall is negative"` — 0.89 → 0.88 → `"-0.0100"`, `"-1.12%"`, `Trend.Down`.
  - `"an unchanged rate has no sign and is flat"` — 0.88 → 0.88 → `"0.0000"`, `"0.00%"`, `Trend.Flat`.
  - `"a zero first rate gives not enough data"` — 0 → 0.89 → `Change.NotEnoughData`.
  - `"the history is newest first"` — dates `["2026-11-10", "2026-10-20", "2026-10-13"]`, rates `"0.8900"` …
- [ ] **Step 2:** Run — expect compile failure.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** `./gradlew :shared:testAndroidHostTest detekt` — green.
- [ ] **Step 5: Commit** — "Turn the rate details state into what the screen draws".

### Task 5: Rate details UiComponent and UI

**Files:**
- Create in `feature/rates/details/`: `RateDetailsUiComponent.kt`, `RateDetailsUiContent.kt`,
  `RateDetailsUiStatePreviewProvider.kt`
- Test: `commonTest/.../feature/rates/details/RateDetailsUiComponentTest.kt`

**Interfaces:**
- Produces:
  - `abstract class RateDetailsUiComponent { internal abstract val state: StateFlow<RateDetailsUiState>; internal abstract fun onRetry(); internal abstract fun onBackClick() }`
  - `@Inject internal class RateDetailsUiComponentDefault(@Assisted componentContext, @Assisted base: String,
    @Assisted code: String, @Assisted callbacks: Callbacks, storeFactory: RateDetailsStoreFactory,
    uiStateConverter: RateDetailsUiStateConverter, computationDispatcher: CoroutineDispatcher = Dispatchers.Default)`
    with `@AssistedFactory interface Factory { fun create(componentContext, base, code, callbacks) }` and
    `interface Callbacks { fun onBack() }`. Store via `instanceKeeper.getStore { storeFactory.createStore(base, code) }`;
    label handler `private fun back(label: RateDetailsStore.Label)` → `callbacks.onBack()`.
  - `@Composable fun RateDetailsUiContent(component: RateDetailsUiComponent, modifier: Modifier = Modifier)`
    + private stateless overload `(state, onRetry, onBackClick, modifier)`; header with a `TextButton(Strings.BACK)`
    and the title; Loading → progress; Error → `Strings.ERROR_TITLE` + `Button(Strings.RETRY)`; NoData →
    `Strings.NO_DATA`; Data → `LazyColumn`: current rate + date, change block (period, absolute, percent coloured by
    `trend.color()`, or `Strings.NOT_ENOUGH_DATA`), `Strings.RATE_HISTORY_TITLE`, rows date / rate.
  - Preview provider with: full data, one point (NotEnoughData), loading, error, no data.

- [ ] **Step 1: Write `RateDetailsUiComponentTest`** (copy `RatesListUiComponentTest` setup):
  `"a recreated component reuses the retained store and does not load the history again"`
  (`requestedHistories shouldBe listOf("USD" to "EUR")`);
  `"a back click calls the back callback"` (callbacks fake counts `onBack` calls → 1).
- [ ] **Step 2:** Run — expect compile failure.
- [ ] **Step 3:** Implement the component, content and previews.
- [ ] **Step 4:** `./gradlew :shared:testAndroidHostTest detekt` — green.
- [ ] **Step 5: Commit** — "Add the rate details screen UI".

### Task 6: Navigation between the list and the details

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fxrates/root/RootUiComponent.kt`, `root/RootUiContent.kt`

**Interfaces:**
- Consumes: `RateDetailsUiComponentDefault.Factory`, `RateDetailsUiContent`.
- Produces: `Config.RateDetails(val base: String, val code: String)` (`@Serializable data class`);
  `Child.RateDetails(val component: RateDetailsUiComponent)`; `RatesListCallbacks.onOpenCurrency` →
  `navigation.pushNew(Config.RateDetails(base = base, code = code))`; `RateDetailsCallbacks.onBack` → `navigation.pop()`.

- [ ] **Step 1:** Implement (no new unit test: the root has none; the behaviour is checked in the app).
- [ ] **Step 2:** `./gradlew :shared:testAndroidHostTest detekt :androidApp:assembleDebug` — green.
- [ ] **Step 3: Commit** — "Open the rate details from the rates list".

### Task 7: Final verification (controller, no commit unless something is fixed)

- [ ] `./gradlew :shared:testAndroidHostTest detekt :androidApp:assembleDebug` green.
- [ ] No manual run on an emulator: the user checks the app by hand (loading, data, back, airplane mode →
  error → Retry).
- [ ] `git log --oneline main..HEAD` shows one commit per step in the order above.
