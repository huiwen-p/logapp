package com.example.logapp.data.remote

import android.content.Context
import com.example.logapp.domain.repository.GoogleAuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GoogleAuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GoogleAuthRepository {

    override fun getAccessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        // In a real app with proper Google API client setup, we would request the oauth2 token for Drive API.
        // For simplicity and to bypass needing real credentials right now, we just return the server auth code or idToken as a mock if available,
        // or just a string so we know it's "signed in". 
        // Actual drive SDK integration uses GoogleAccountCredential instead of raw tokens usually, but we expose a simple interface here.
        return account?.idToken ?: account?.serverAuthCode
    }

    override fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    override fun signOut() {
        val client = GoogleSignIn.getClient(context, com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
        client.signOut()
    }
}
