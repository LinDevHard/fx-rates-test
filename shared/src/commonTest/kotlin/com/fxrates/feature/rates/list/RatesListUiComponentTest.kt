package com.fxrates.feature.rates.list

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.feature.rates.list.mvi.RatesListStoreFactory
import com.fxrates.testing.FakeRatesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RatesListUiComponentTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    val callbacks = object : RatesListUiComponentDefault.Callbacks {

        override fun onOpenCurrency(base: String, code: String) = Unit

    }

    fun createComponent(
        repository: FakeRatesRepository,
        instanceKeeper: InstanceKeeperDispatcher,
    ): RatesListUiComponentDefault {
        val lifecycle = LifecycleRegistry().apply { create() }
        return RatesListUiComponentDefault(
            componentContext = DefaultComponentContext(lifecycle = lifecycle, instanceKeeper = instanceKeeper),
            base = "USD",
            callbacks = callbacks,
            storeFactory = RatesListStoreFactory(storeFactory = DefaultStoreFactory(), repository = repository),
            uiStateConverter = RatesListUiStateConverter(),
            computationDispatcher = testDispatcher,
        )
    }

    test("a recreated component reuses the retained store and does not load the rates again") {
        val repository = FakeRatesRepository()
        val instanceKeeper = InstanceKeeperDispatcher()
        val first = createComponent(repository, instanceKeeper)
        first.onQueryChange("eu")

        val second = createComponent(repository, instanceKeeper)

        repository.requestedBases shouldBe listOf("USD")
        second.state.value.query shouldBe "eu"
    }

    test("destroying the instance keeper disposes the store so intents no longer reach the repository") {
        val repository = FakeRatesRepository()
        val instanceKeeper = InstanceKeeperDispatcher()
        val component = createComponent(repository, instanceKeeper)

        instanceKeeper.destroy()
        component.onRefresh()

        repository.requestedBases shouldBe listOf("USD")
    }

})
