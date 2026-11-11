package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.fxrates.core.utils.resourceFlow
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Action
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Intent
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Label
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Message
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.State
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class RateDetailsExecutor(
    private val repository: RatesRepository,
) : CoroutineExecutor<Intent, Action, State, Message, Label>() {

    override fun executeAction(action: Action) = when (action) {
        is Action.LoadHistory -> loadHistory(action)
    }

    override fun executeIntent(intent: Intent) = when (intent) {
        is Intent.OnRetry -> forward(Action.LoadHistory(base = state().base, code = state().code))
        is Intent.OnBackClick -> publish(Label.Back)
    }

    private fun loadHistory(action: Action.LoadHistory) {
        resourceFlow { repository.getRateHistory(base = action.base, code = action.code) }
            .onEach { history -> dispatch(Message.UpdateHistory(history)) }
            .launchIn(scope)
    }

}
