package ru.noxly.baumforms.helper

import kotlinx.coroutines.flow.MutableSharedFlow
import ru.noxly.baumforms.server.ServerEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 1)
    val events = _events

    suspend fun send(event: ServerEvent) {
        println("📡 Отправка события: $event")
        _events.emit(event)
    }
}