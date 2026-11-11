package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.core.utils.Resource
import com.fxrates.testing.FakeRatesRepository
import com.fxrates.testing.ratePoint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RateDetailsStoreTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    fun createStore(repository: FakeRatesRepository): RateDetailsStore {
        val factory = RateDetailsStoreFactory(storeFactory = DefaultStoreFactory(), repository = repository)
        return factory.createStore(base = "USD", code = "EUR")
    }

    test("the history is loaded for the currency and base on start") {
        val repository = FakeRatesRepository().apply { rateHistory = Result.success(listOf(ratePoint())) }

        val store = createStore(repository)

        repository.requestedHistories shouldBe listOf("USD" to "EUR")
        store.state.history shouldBe Resource.Data(listOf(ratePoint()))
    }

    test("a failed load ends in an error and a retry loads again") {
        val repository = FakeRatesRepository().apply { rateHistory = Result.failure(IllegalStateException("offline")) }
        val store = createStore(repository)
        store.state.history.shouldBeInstanceOf<Resource.Error>()

        repository.rateHistory = Result.success(listOf(ratePoint()))
        store.accept(RateDetailsStore.Intent.OnRetry)

        repository.requestedHistories shouldBe listOf("USD" to "EUR", "USD" to "EUR")
        store.state.history shouldBe Resource.Data(listOf(ratePoint()))
    }

    test("a back click publishes the back label") {
        runTest(testDispatcher) {
            val store = createStore(FakeRatesRepository())
            var label: RateDetailsStore.Label? = null
            val job = launch { label = store.labels.first() }

            store.accept(RateDetailsStore.Intent.OnBackClick)
            job.join()

            label shouldBe RateDetailsStore.Label.Back
        }
    }

})
