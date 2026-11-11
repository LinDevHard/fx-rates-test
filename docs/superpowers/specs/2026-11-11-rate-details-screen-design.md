# Rate details screen — design

## Intent

Tapping a currency in the rates list opens a screen about that one currency against the base
currency of the list. The project is a test assignment, and it is judged first of all on how
closely the code follows `AGENTS.md` and `docs/`. The screen therefore copies the reference
screen `feature/rates/list/` file for file, name for name and layer for layer; where a rule
exists, the rule is followed, not a local idea.

## What the screen shows

- **Current rate** of the currency against the base: the rate of the latest point of the
  history, with its date (`1 USD = 0.8916 EUR`, `2026-11-10`).
- **Rates for the last 30 days** as a list: one row per business day, date and rate, newest
  first.
- **Change over the period**, absolute and in percent, both with a sign: the latest point
  against the earliest point of the window, through `RateChangeCalculator` and
  `NumberFormatter.formatSignedRate` / `formatSignedPercent`. Coloured up / down / neutral like
  the list.
- **Loading** state, and an **error** state with a Retry button.
- A **back** button in the header; system back works through the stack.

### Edge cases (decided by the user)

| History points from the API | Screen                                                                          |
|-----------------------------|---------------------------------------------------------------------------------|
| 0                           | A "No data for the period" message instead of the content (not an error, no Retry). |
| 1                           | Current rate and the one-row history are shown; the change block says "Not enough data to calculate the change". |
| 2 or more                   | Everything, including the change.                                               |

When the earliest rate is zero, `RateChangeCalculator` returns `null`; the change block then says
"Not enough data to calculate the change" too.

The change block is labelled with the actual dates it spans (`2026-10-13 – 2026-11-10`), not
"30 days", because the first business day of the window may be a few days later than day 30.

A change that rounds to zero is shown without a sign (`0.0000`, `0.00%`) and as neutral — this is
what `NumberFormatter` already does.

## Goals

- The screen above, reachable from the list and back.
- Unit tests for the screen logic: a Store test, a UiStateConverter test and a UiComponent test,
  following `RatesListStoreTest`, `RatesListUiStateConverterTest`, `RatesListUiComponentTest`;
  plus tests for the new converter method in `RatesConverterTest`.
- `./gradlew :shared:testAndroidHostTest detekt :androidApp:assembleDebug` is green.

## Non-goals

- A chart (planned later, not now).
- Choosing a different period or a different base on this screen.
- A day-to-day change per history row.
- A refresh button in the header (Retry in the error state is enough).
- The currency's full name (it would need a second request to `currencies`; the code is shown).

## Anti-goals

- Any deviation from the documented rules: a second formatter or a hand-made percent, `Double`
  for rates, formatting in a composable, a screen that navigates itself, a model in a `Config`,
  a new repository instead of a new method, a Store created without `instanceKeeper.getStore`.
- New dependencies.
- Large mixed commits.

## How it is built

### Domain

- New model `domain/model/RatePoint.kt`: `data class RatePoint(val date: LocalDate, val rate: BigDecimal)`.
- New method on the existing `RatesRepository`:
  `suspend fun getRateHistory(base: String, code: String): List<RatePoint>` — the rates of `code`
  against `base` for the last 30 calendar days, sorted by date ascending; empty when the API has
  none.

### Data

- `FrankfurterApi.getTimeSeries` gets an optional `symbols: String? = null` parameter, sent as
  the `symbols` query parameter when not null, so the history request downloads one currency
  instead of all of them. The existing caller is unchanged.
- `RatesConverter.toRateHistory(timeSeries: TimeSeriesDto, code: String): List<RatePoint>` —
  takes the `code` entry of each date, skips dates without it, sorts ascending, converts through
  the string form to `BigDecimal` (the existing `toDecimal()`).
- `RatesRepositoryImpl.getRateHistory` computes `start = today(UTC) - 30 days` with the injected
  `Clock` (same pattern as `LOOKBACK_DAYS`), calls `getTimeSeries(base, start, symbols = code)`
  and the converter.
- Frankfurter answers a window without data with `404 {"message":"not found"}` (checked by hand),
  and the client has `expectSuccess = true`. `getRateHistory` catches a `ClientRequestException`
  with status `404 Not Found` and returns an empty list, so the screen shows "No data for the
  period" (decided by the user). Every other failure still propagates and becomes the error state.
  Known trade-off: a 404 for an unknown currency code also reads as "no data".

### Shared UI model (refactoring)

`Trend` (now in `feature/rates/list/model/RateItem.kt`) is needed by both screens. It moves to
its own file shared by the rates feature, `feature/rates/model/Trend.kt`, in a separate commit
before the new screen. The trend rule (zero/null → Flat, positive → Up, else Down) is not
duplicated in the second converter: it becomes one function next to `Trend`
(e.g. `RateChange?.toTrend()`), used by both converters.

### Screen — `feature/rates/details/`

Same files as the reference screen, prefix `RateDetails`:

```text
feature/rates/details/
├── mvi/
│   ├── RateDetailsStore.kt          Intent: OnRetry, OnBackClick
│   │                                Action: LoadHistory(base, code)
│   │                                Message: UpdateHistory(Resource<List<RatePoint>>)
│   │                                Label: Back
│   │                                State(base, code, history: Resource<List<RatePoint>> = Loading)
│   ├── RateDetailsReducer.kt
│   ├── RateDetailsExecutor.kt       loadHistory(action) via resourceFlow { … }.onEach { dispatch }.launchIn(scope)
│   └── RateDetailsStoreFactory.kt   SimpleBootstrapper(Action.LoadHistory(base, code))
├── model/                           @Immutable HistoryItem(date, rate) — formatted strings
├── RateDetailsUiState.kt            title (`USD / EUR`), content: Loading | Error | NoData | Data(...)
├── RateDetailsUiStateConverter.kt   all formatting and the change calculation
├── RateDetailsUiComponent.kt        onRetry(), onBackClick(); Default with Factory and Callbacks.onBack()
├── RateDetailsUiContent.kt          public composable + private stateless overload + previews
└── RateDetailsUiStatePreviewProvider.kt  loading, error, no data, one point, full
```

`Content.Data` holds the formatted current rate and its date, a sealed change block
(`Change.Value(absolute, percent, trend)` / `Change.NotEnoughData`) and the history items.

### Navigation

- `RootUiComponent.Config.RateDetails(base: String, code: String)` and
  `Child.RateDetails(component)`.
- `RatesListCallbacks.onOpenCurrency` pushes `Config.RateDetails(base, code)` with `pushNew`: in
  Decompose 3.5 it does nothing when the same config is already on top, so a double tap does not
  open two screens (checked in the library sources).
- `RateDetailsCallbacks.onBack` pops.
- `RootUiContent` draws the new child.

### Strings and DI

- New strings in `ui/Strings.kt` (title format, "No data for the period", "Not enough data to
  calculate the change", the change label with its dates, "Back"). The existing `ERROR_TITLE` and `RETRY`
  are reused.
- No new bindings in `AppComponent`: the repository is already bound, the new classes have
  `@Inject` constructors.

### Tests

- `FakeRatesRepository` gets `rateHistory: Result<List<RatePoint>>` and records the requested
  `(base, code)` pairs; `TestData` gets a `ratePoint(...)` builder.
- Store: loads on start with the right base/code; error then retry loads again; back publishes
  `Label.Back`.
- Converter: loading, error, 0 points → NoData, 1 point → rate + NotEnoughData, 2+ points →
  signed absolute and percent and trend, rise / fall / zero change, history newest first,
  formatting of rate and date.
- UiComponent: Store retained across recreation (as in `RatesListUiComponentTest`), `Label.Back`
  calls `Callbacks.onBack`.
- Repository: the 404 → empty list mapping is covered by a test of `RatesRepositoryImpl` with
  Ktor's `MockEngine` (`ktor-client-mock` is already a test dependency of `:shared`).
- `RatesConverterTest`: `toRateHistory` sorts ascending, skips dates without the code, empty
  input → empty list.

## Commits

One logical step per commit, English messages saying what and why, in this order:

1. this design document;
2. `Trend` moved out of the list and the trend rule shared;
3. `symbols` parameter on `getTimeSeries`;
4. `RatePoint` + `RatesRepository.getRateHistory` + converter + impl + tests;
5. the screen's Store (Store, Reducer, Executor, Factory, fake, Store test);
6. UiState, converter, strings, converter test;
7. UiComponent, content, previews, component test;
8. navigation from the list and back.

Every commit compiles and keeps the tests and detekt green.

## Calls made by Claude (not discussed with the user)

- Screen name and package: `RateDetails`, `feature/rates/details/`.
- "30 days" = 30 calendar days back from today (UTC), so about 21 business-day points.
- Change = latest point vs earliest point of the window.
- History newest first, date shown as ISO `yyyy-MM-dd` like the list subtitle.
- With one point the one-row history is still shown.
- `Trend` moved to `feature/rates/model/` and the trend rule shared; commit 2 also switches
  `RatesListUiStateConverter` to the shared rule.
- Title `USD / EUR`, no subtitle; change label shows the actual first and last dates.
- A zero earliest rate is treated as "not enough data".
- No refresh button, no currency name.

## Left to the builder

Exact layout and spacing (within `AppTheme`), exact string wording, the wording of KDoc.
