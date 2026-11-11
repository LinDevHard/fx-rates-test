package com.fxrates.feature.rates.details

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.feature.rates.details.mvi.RateDetailsStoreFactory
import com.fxrates.testing.FakeRatesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RateDetailsUiComponentTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    class FakeCallbacks : RateDetailsUiComponentDefault.Callbacks {

        var backCount = 0

        override fun onBack() {
            backCount++
        }

    }

    fun createComponent(
        repository: FakeRatesRepository,
        instanceKeeper: InstanceKeeperDispatcher,
        callbacks: RateDetailsUiComponentDefault.Callbacks = FakeCallbacks(),
    ): RateDetailsUiComponentDefault {
        val lifecycle = LifecycleRegistry().apply { create() }
        return RateDetailsUiComponentDefault(
            componentContext = DefaultComponentContext(lifecycle = lifecycle, instanceKeeper = instanceKeeper),
            base = "USD",
            code = "EUR",
            callbacks = callbacks,
            storeFactory = RateDetailsStoreFactory(storeFactory = DefaultStoreFactory(), repository = repository),
            uiStateConverter = RateDetailsUiStateConverter(),
            computationDispatcher = testDispatcher,
        )
    }

    test("a recreated component reuses the retained store and does not load the history again") {
        val repository = FakeRatesRepository()
        val instanceKeeper = InstanceKeeperDispatcher()
        createComponent(repository, instanceKeeper)

        createComponent(repository, instanceKeeper)

        repository.requestedHistories shouldBe listOf("USD" to "EUR")
    }

    test("a back click calls the back callback") {
        val callbacks = FakeCallbacks()
        val component = createComponent(FakeRatesRepository(), InstanceKeeperDispatcher(), callbacks)

        component.onBackClick()

        callbacks.backCount shouldBe 1
    }

})
