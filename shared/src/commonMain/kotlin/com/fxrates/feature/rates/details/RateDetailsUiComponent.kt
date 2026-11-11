package com.fxrates.feature.rates.details

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.fxrates.feature.rates.details.mvi.RateDetailsStore
import com.fxrates.feature.rates.details.mvi.RateDetailsStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject

abstract class RateDetailsUiComponent {

    internal abstract val state: StateFlow<RateDetailsUiState>

    internal abstract fun onRetry()

    internal abstract fun onBackClick()

}

@Inject
internal class RateDetailsUiComponentDefault(
    @Assisted componentContext: ComponentContext,
    @Assisted base: String,
    @Assisted code: String,
    @Assisted private val callbacks: Callbacks,
    storeFactory: RateDetailsStoreFactory,
    uiStateConverter: RateDetailsUiStateConverter,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RateDetailsUiComponent(), ComponentContext by componentContext {

    @AssistedFactory
    interface Factory {

        fun create(
            componentContext: ComponentContext,
            base: String,
            code: String,
            callbacks: Callbacks,
        ): RateDetailsUiComponentDefault

    }

    interface Callbacks {

        fun onBack()

    }

    private val scope = coroutineScope()

    private val store = instanceKeeper.getStore { storeFactory.createStore(base = base, code = code) }

    override val state: StateFlow<RateDetailsUiState> = store.states
        .map(uiStateConverter::createUiState)
        .flowOn(computationDispatcher)
        .stateIn(scope, SharingStarted.Eagerly, RateDetailsUiState())

    init {
        store.labels
            .onEach(::back)
            .launchIn(scope)
    }

    private fun back(label: RateDetailsStore.Label) {
        when (label) {
            is RateDetailsStore.Label.Back -> callbacks.onBack()
        }
    }

    override fun onRetry() {
        store.accept(RateDetailsStore.Intent.OnRetry)
    }

    override fun onBackClick() {
        store.accept(RateDetailsStore.Intent.OnBackClick)
    }

}
