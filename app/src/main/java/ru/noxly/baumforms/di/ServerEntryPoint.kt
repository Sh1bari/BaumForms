package ru.noxly.baumforms.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.noxly.baumforms.server.ServerManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServerEntryPoint {
    fun serverManager(): ServerManager
}