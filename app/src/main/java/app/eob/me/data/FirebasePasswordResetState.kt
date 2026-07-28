package app.eob.me.data

sealed class FirebasePasswordResetState {
    data object Idle : FirebasePasswordResetState()
    data object Loading : FirebasePasswordResetState()
    data class Success(val message: String) : FirebasePasswordResetState()
    data class Error(val message: String) : FirebasePasswordResetState()
}
