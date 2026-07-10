package com.suvojeet.notenext.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val context: Context,
    private val activity: FragmentActivity
) {

    private val executor = ContextCompat.getMainExecutor(context)
    private val biometricManager = BiometricManager.from(context)

    fun canAuthenticate(): Int {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }

    fun showBiometricPrompt(
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onAuthSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthError: (String) -> Unit = {},
        onAuthFailed: () -> Unit = {}
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for NoteNext")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(
                if (cryptoObject != null) BiometricManager.Authenticators.BIOMETRIC_STRONG
                else BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onAuthError("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess(result)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthFailed()
                }
            })

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
