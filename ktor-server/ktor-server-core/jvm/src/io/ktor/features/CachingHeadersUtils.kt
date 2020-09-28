/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.features

import io.ktor.http.content.*
import io.ktor.util.date.*


private const val `30_DAYS_IN_MILLIS`: Long = 30 * 24 * 60 * 60

/**
 * Enable cache headers for [LocalFileContent]. It usually sends from [ApplicationCall.respondFile] method.
 *
 * @param [duration] specifies the expiration of file from the current timestamp.
 */
public fun CachingHeaders.Configuration.files(duration: Long = `30_DAYS_IN_MILLIS`) {
    options {
        return@options if (it is LocalFileContent) {
            CachingOptions(expires = GMTDate() + duration)
        } else null
    }
}
