/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.features

import io.ktor.http.content.*
import io.ktor.util.date.*


@PublishedApi
internal const val `30_DAYS_IN_MILLIS`: Long = 30 * 24 * 60 * 60

/**
 * Enable cache headers for [LocalFileContent]. It usually sends from [ApplicationCall.respondFile] method.
 *
 * @param [duration] specifies the expiration of file from the current timestamp.
 */
public fun CachingHeaders.Configuration.files(duration: Long = `30_DAYS_IN_MILLIS`) {
    expiresFor<LocalFileContent>(duration)
}


/**
 * Enable cache headers for a custom [OutgoingContent] type.
 *
 * @param [duration] specifies the expiration of file from the current timestamp.
 */
public inline fun <reified T: OutgoingContent> CachingHeaders.Configuration.expiresFor(
    duration: Long = `30_DAYS_IN_MILLIS`
) {
    options {
        return@options if (it is T) {
            CachingOptions(expires = GMTDate() + duration)
        } else null
    }
}
