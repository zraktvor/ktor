/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http


/**
 * Represents an HTTP status code and description.
 * @param value is a numeric code.
 * @param description is free form description of a status.
 */
@Suppress("unused", "UNCHECKED_CAST")
open class HttpStatusCode(val value: Int, val description: String) {
    override fun toString(): String = "$value $description"

    override fun equals(other: Any?): Boolean = other is HttpStatusCode && other.value == value

    override fun hashCode(): Int = value.hashCode()

    /**
     * Returns a copy of `this` code with a description changed to [value].
     */
    fun description(value: String): HttpStatusCode = HttpStatusCode(value = this.value, description = value)

    @Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
    companion object {
        // =============================================================================================================
        // Disclaimer
        // Adding a new status code here please remember [allStatusCodes] as well
        //

        class ContinueCode : HttpStatusCode(100, "ContinueCode")

        val Continue = ContinueCode()

        class SwitchingProtocolsCode : HttpStatusCode(101, "Switching Protocols")

        val SwitchingProtocols = SwitchingProtocolsCode()

        class ProcessingCode : HttpStatusCode(102, "ProcessingCode")

        val Processing = ProcessingCode()

        class OKCode : HttpStatusCode(200, "OKCode")

        val OK = OKCode()

        class CreatedCode : HttpStatusCode(201, "CreatedCode")

        val Created = CreatedCode()

        class AcceptedCode : HttpStatusCode(202, "AcceptedCode")

        val Accepted = AcceptedCode()

        class NonAuthoritativeInformationCode :
            HttpStatusCode(203, "Non-Authoritative Information")

        val NonAuthoritativeInformation = NonAuthoritativeInformationCode()

        class NoContentCode : HttpStatusCode(204, "No Content")

        val NoContent = NoContentCode()

        class ResetContentCode : HttpStatusCode(205, "Reset Content")

        val ResetContent = ResetContentCode()

        class PartialContentCode : HttpStatusCode(206, "Partial Content")

        val PartialContent = PartialContentCode()

        class MultiStatusCode : HttpStatusCode(207, "Multi-Status")

        val MultiStatus = MultiStatusCode()

        class MultipleChoicesCode : HttpStatusCode(300, "Multiple Choices")

        val MultipleChoices = MultipleChoicesCode()

        class MovedPermanentlyCode : HttpStatusCode(301, "Moved Permanently")

        val MovedPermanently = MovedPermanentlyCode()

        class FoundCode : HttpStatusCode(302, "FoundCode")

        val Found = FoundCode()

        class SeeOtherCode : HttpStatusCode(303, "See Other")

        val SeeOther = SeeOtherCode()

        class NotModifiedCode : HttpStatusCode(304, "Not Modified")

        val NotModified = NotModifiedCode()

        class UseProxyCode : HttpStatusCode(305, "Use Proxy")

        val UseProxy = UseProxyCode()

        class SwitchProxyCode : HttpStatusCode(306, "Switch Proxy")

        val SwitchProxy = SwitchProxyCode()

        class TemporaryRedirectCode : HttpStatusCode(307, "Temporary Redirect")

        val TemporaryRedirect = TemporaryRedirectCode()

        class PermanentRedirectCode : HttpStatusCode(308, "Permanent Redirect")

        val PermanentRedirect = PermanentRedirectCode()

        class BadRequestCode : HttpStatusCode(400, "Bad Request")

        val BadRequest = BadRequestCode()

        class UnauthorizedCode : HttpStatusCode(401, "UnauthorizedCode")

        val Unauthorized = UnauthorizedCode()

        class PaymentRequiredCode : HttpStatusCode(402, "Payment Required")

        val PaymentRequired = PaymentRequiredCode()

        class ForbiddenCode : HttpStatusCode(403, "ForbiddenCode")

        val Forbidden = ForbiddenCode()

        class NotFoundCode : HttpStatusCode(404, "Not Found")

        val NotFound = NotFoundCode()

        class MethodNotAllowedCode : HttpStatusCode(405, "Method Not Allowed")

        val MethodNotAllowed = MethodNotAllowedCode()

        class NotAcceptableCode : HttpStatusCode(406, "Not Acceptable")

        val NotAcceptable = NotAcceptableCode()

        class ProxyAuthenticationRequiredCode :
            HttpStatusCode(407, "Proxy Authentication Required")

        val ProxyAuthenticationRequired = ProxyAuthenticationRequiredCode()

        class RequestTimeoutCode : HttpStatusCode(408, "Request Timeout")

        val RequestTimeout = RequestTimeoutCode()

        class ConflictCode : HttpStatusCode(409, "ConflictCode")

        val Conflict = ConflictCode()

        class GoneCode : HttpStatusCode(410, "GoneCode")

        val Gone = GoneCode()

        class LengthRequiredCode : HttpStatusCode(411, "Length Required")

        val LengthRequired = LengthRequiredCode()

        class PreconditionFailedCode : HttpStatusCode(412, "Precondition Failed")

        val PreconditionFailed = PreconditionFailedCode()

        class PayloadTooLargeCode : HttpStatusCode(413, "Payload Too Large")

        val PayloadTooLarge = PayloadTooLargeCode()

        class RequestURITooLongCode : HttpStatusCode(414, "Request-URI Too Long")

        val RequestURITooLong = RequestURITooLongCode()

        class UnsupportedMediaTypeCode : HttpStatusCode(415, "Unsupported Media Type")

        val UnsupportedMediaType = UnsupportedMediaTypeCode()

        class RequestedRangeNotSatisfiableCode :
            HttpStatusCode(416, "Requested Range Not Satisfiable")

        val RequestedRangeNotSatisfiable = RequestedRangeNotSatisfiableCode()

        class ExpectationFailedCode : HttpStatusCode(417, "Expectation Failed")

        val ExpectationFailed = ExpectationFailedCode()

        class UnprocessableEntityCode : HttpStatusCode(422, "Unprocessable Entity")

        val UnprocessableEntity = UnprocessableEntityCode()

        class LockedCode : HttpStatusCode(423, "LockedCode")

        val Locked = LockedCode()

        class FailedDependencyCode : HttpStatusCode(424, "Failed Dependency")

        val FailedDependency = FailedDependencyCode()

        class UpgradeRequiredCode : HttpStatusCode(426, "Upgrade Required")

        val UpgradeRequired = UpgradeRequiredCode()

        class TooManyRequestsCode : HttpStatusCode(429, "Too Many Requests")

        val TooManyRequests = TooManyRequestsCode()

        class RequestHeaderFieldTooLargeCode :
            HttpStatusCode(431, "Request Header Fields Too Large")

        val RequestHeaderFieldTooLarge = RequestHeaderFieldTooLargeCode()

        class InternalServerErrorCode : HttpStatusCode(500, "Internal Server Error")

        val InternalServerError = InternalServerErrorCode()

        class NotImplementedCode : HttpStatusCode(501, "Not Implemented")

        val NotImplemented = NotImplementedCode()

        class BadGatewayCode : HttpStatusCode(502, "Bad Gateway")

        val BadGateway = BadGatewayCode()

        class ServiceUnavailableCode : HttpStatusCode(503, "Service Unavailable")

        val ServiceUnavailable = ServiceUnavailableCode()

        class GatewayTimeoutCode : HttpStatusCode(504, "Gateway Timeout")

        val GatewayTimeout = GatewayTimeoutCode()

        class VersionNotSupportedCode : HttpStatusCode(505, "HTTP Version Not Supported")

        val VersionNotSupported = VersionNotSupportedCode()

        class VariantAlsoNegotiatesCode : HttpStatusCode(506, "Variant Also Negotiates")

        val VariantAlsoNegotiates = VariantAlsoNegotiatesCode()

        class InsufficientStorageCode : HttpStatusCode(507, "Insufficient Storage")

        val InsufficientStorage = InsufficientStorageCode()

        /**
         * All known status codes
         */
        val allStatusCodes: List<HttpStatusCode> = io.ktor.http.allStatusCodes()

        private val byValue: Array<HttpStatusCode?> = Array(1000) { idx ->
            allStatusCodes.firstOrNull { it.value == idx }
        }

        class UnknownHttpStatusCode(value: Int) : HttpStatusCode(value, "Unknown Status Code")

        /**
         * Creates an instance of [HttpStatusCode] with the given numeric value.
         */
        fun fromValue(value: Int): HttpStatusCode {
            val knownStatus = if (value in 1 until 1000) byValue[value] else null
            return knownStatus ?: UnknownHttpStatusCode(value)
        }
    }
}

@Suppress("UNUSED", "KDocMissingDocumentation")
@Deprecated(
    "Use ExpectationFailed instead",
    ReplaceWith("ExpectationFailed", "io.ktor.http.HttpStatusCode.Companion.ExpectationFailed"),
    level = DeprecationLevel.ERROR
)
inline val HttpStatusCode.Companion.ExceptionFailed
    get() = HttpStatusCode.ExpectationFailed

internal fun allStatusCodes(): List<HttpStatusCode> = listOf(
    HttpStatusCode.Continue,
    HttpStatusCode.SwitchingProtocols,
    HttpStatusCode.Processing,
    HttpStatusCode.OK,
    HttpStatusCode.Created,
    HttpStatusCode.Accepted,
    HttpStatusCode.NonAuthoritativeInformation,
    HttpStatusCode.NoContent,
    HttpStatusCode.ResetContent,
    HttpStatusCode.PartialContent,
    HttpStatusCode.MultiStatus,
    HttpStatusCode.MultipleChoices,
    HttpStatusCode.MovedPermanently,
    HttpStatusCode.Found,
    HttpStatusCode.SeeOther,
    HttpStatusCode.NotModified,
    HttpStatusCode.UseProxy,
    HttpStatusCode.SwitchProxy,
    HttpStatusCode.TemporaryRedirect,
    HttpStatusCode.PermanentRedirect,
    HttpStatusCode.BadRequest,
    HttpStatusCode.Unauthorized,
    HttpStatusCode.PaymentRequired,
    HttpStatusCode.Forbidden,
    HttpStatusCode.NotFound,
    HttpStatusCode.MethodNotAllowed,
    HttpStatusCode.NotAcceptable,
    HttpStatusCode.ProxyAuthenticationRequired,
    HttpStatusCode.RequestTimeout,
    HttpStatusCode.Conflict,
    HttpStatusCode.Gone,
    HttpStatusCode.LengthRequired,
    HttpStatusCode.PreconditionFailed,
    HttpStatusCode.PayloadTooLarge,
    HttpStatusCode.RequestURITooLong,
    HttpStatusCode.UnsupportedMediaType,
    HttpStatusCode.RequestedRangeNotSatisfiable,
    HttpStatusCode.ExpectationFailed,
    HttpStatusCode.UnprocessableEntity,
    HttpStatusCode.Locked,
    HttpStatusCode.FailedDependency,
    HttpStatusCode.UpgradeRequired,
    HttpStatusCode.TooManyRequests,
    HttpStatusCode.RequestHeaderFieldTooLarge,
    HttpStatusCode.InternalServerError,
    HttpStatusCode.NotImplemented,
    HttpStatusCode.BadGateway,
    HttpStatusCode.ServiceUnavailable,
    HttpStatusCode.GatewayTimeout,
    HttpStatusCode.VersionNotSupported,
    HttpStatusCode.VariantAlsoNegotiates,
    HttpStatusCode.InsufficientStorage
)

/**
 * Checks if a given status code is a success code according to HTTP standards.
 *
 * Codes from 200 to 299 are considered to be successful.
 */
fun HttpStatusCode.isSuccess(): Boolean = value in (200 until 300)
