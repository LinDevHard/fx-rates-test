package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Action
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.State
import me.tatarka.inject.annotations.Inject

@Inject
internal class RateDetailsStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: RatesRepository,
) {

    fun createStore(base: String, code: String): RateDetailsStore {
        val store = storeFactory.create(
            name = "RateDetailsStore",
            initialState = State(base = base, code = code),
            bootstrapper = SimpleBootstrapper(Action.LoadHistory(base = base, code = code)),
            executorFactory = { RateDetailsExecutor(repository = repository) },
            reducer = { message -> reduce(message) },
        )
        return RateDetailsStore(store)
    }

}
