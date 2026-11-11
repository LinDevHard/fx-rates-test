package com.fxrates.feature.rates.details.mvi

import com.fxrates.feature.rates.details.mvi.RateDetailsStore.Message
import com.fxrates.feature.rates.details.mvi.RateDetailsStore.State

internal fun State.reduce(message: Message): State = when (message) {
    is Message.UpdateHistory -> copy(history = message.history)
}
