package com.ashes.dev.works.ai.neural.brain.medha.domain.model

sealed class User {
    data object Person : User()
    data object AI : User()
}