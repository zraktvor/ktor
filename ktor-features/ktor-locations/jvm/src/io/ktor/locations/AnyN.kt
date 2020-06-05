/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.locations

import io.ktor.application.*
import io.ktor.util.pipeline.*

// Interfaces for @Location-annotated class to implement:

@KtorExperimentalLocationsAPI
interface RespondsUnit

@KtorExperimentalLocationsAPI
interface Responds<T1 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny2<T1 : Any, T2 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny3<T1 : Any, T2 : Any, T3 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny4<T1 : Any, T2 : Any, T3 : Any, T4 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny5<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny6<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny7<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny8<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny9<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, T9 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny10<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, T9 : Any, T10 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny11<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, T9 : Any, T10 : Any, T11 : Any>

@KtorExperimentalLocationsAPI
interface RespondsAny12<T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, T9 : Any, T10 : Any, T11 : Any, T12 : Any>
