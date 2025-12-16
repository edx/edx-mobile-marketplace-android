
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.presentation.iap.IAPRequestType
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.isInternetError(): Boolean {
    return this is SocketTimeoutException || this is UnknownHostException
}

fun Throwable.toIAPException(
    requestType: IAPRequestType,
    defaultMessage: String
): IAPException {
    return this as? IAPException
        ?: IAPException(
            requestType = requestType,
            errorMessage = this.message ?: defaultMessage
        )
}
