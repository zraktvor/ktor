/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.scheduler

import java.nio.channels.*

suspend fun SelectingScope.selectRead() {
    return selectionEntry.select(SelectionKey.OP_READ)
}

suspend fun SelectingScope.selectWrite() {
    return selectionEntry.select(SelectionKey.OP_WRITE)
}

suspend fun SelectingScope.selectAccept() {
    return selectionEntry.select(SelectionKey.OP_ACCEPT)
}

suspend fun SelectingScope.selectConnect() {
    return selectionEntry.select(SelectionKey.OP_CONNECT)
}
