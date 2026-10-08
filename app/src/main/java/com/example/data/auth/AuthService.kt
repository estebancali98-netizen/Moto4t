package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppUser
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthService(private val context: Context) {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }
    private val firestore: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val prefs by lazy {
        context.getSharedPreferences("mototaller_auth_prefs", Context.MODE_PRIVATE)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val _currentUserState = MutableStateFlow<AppUser?>(null)
    val currentUserState: StateFlow<AppUser?> = _currentUserState.asStateFlow()

    init {
        // Inicializa el usuario actual si Firebase Auth ya tiene una sesión persistida
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                _currentUserState.value = null
            } else {
                serviceScope.launch {
                    val appUser = resolveUserRole(user, null)
                    _currentUserState.value = appUser
                }
            }
        }
    }

    /**
     * Obtiene el Web Client ID generado automáticamente desde google-services.json
     */
    private fun getServerClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e("AuthService", "No se encontró R.string.default_web_client_id", e)
            null
        }
    }

    /**
     * Inicia sesión interactiva con Google mediante Credential Manager
     * @param selectedRole Rol preferido si es la primera vez que se registra el usuario (ADMIN o MECHANIC)
     */
    suspend fun signInWithGoogle(activity: Activity, selectedRole: UserRole? = null): Result<AppUser> {
        val clientId = getServerClientId()
            ?: return Result.failure(IllegalStateException("Configuración de Google Sign-In incompleta (default_web_client_id no disponible)"))

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("No se pudo obtener el usuario de Firebase"))

                val appUser = resolveUserRole(firebaseUser, selectedRole)
                _currentUserState.value = appUser
                Result.success(appUser)
            } else {
                Result.failure(IllegalStateException("Tipo de credencial no esperado"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthService", "Inicio de sesión con Google cancelado por el usuario: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthService", "Error durante inicio de sesión con Google", e)
            Result.failure(e)
        }
    }

    /**
     * Intenta auto-inicio de sesión silencioso en segundo plano
     */
    suspend fun attemptSilentSignIn(): Result<AppUser> {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val appUser = resolveUserRole(currentUser, null)
            _currentUserState.value = appUser
            return Result.success(appUser)
        }

        val clientId = getServerClientId()
            ?: return Result.failure(IllegalStateException("default_web_client_id no encontrado"))

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("No se obtuvo usuario de Firebase"))

                val appUser = resolveUserRole(firebaseUser, null)
                _currentUserState.value = appUser
                Result.success(appUser)
            } else {
                Result.failure(IllegalStateException("No hay credenciales previas guardadas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa en Firebase y limpia el estado de Credential Manager
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            prefs.edit().remove("cached_user_role").apply()
            _currentUserState.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthService", "Error cerrando sesión", e)
            Result.failure(e)
        }
    }

    /**
     * Consulta o inicializa el rol del usuario en Firestore (colección 'users/{uid}')
     * Diferencia claramente Administrador de Mecánico.
     */
    private suspend fun resolveUserRole(firebaseUser: FirebaseUser, requestedRole: UserRole?): AppUser {
        val uid = firebaseUser.uid
        val defaultRole = requestedRole ?: if (
            firebaseUser.email?.contains("mecanico", ignoreCase = true) == true ||
            firebaseUser.displayName?.contains("mecanico", ignoreCase = true) == true
        ) {
            UserRole.MECHANIC
        } else {
            UserRole.ADMIN
        }

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val role = if (doc.exists() && doc.contains("role")) {
                val roleStr = doc.getString("role") ?: defaultRole.name
                try {
                    UserRole.valueOf(roleStr)
                } catch (_: Exception) {
                    defaultRole
                }
            } else {
                // Registrar nuevo documento de usuario en Firestore
                firestore.collection("users").document(uid).set(
                    hashMapOf(
                        "uid" to uid,
                        "name" to (firebaseUser.displayName ?: "Usuario Taller"),
                        "email" to (firebaseUser.email ?: ""),
                        "role" to defaultRole.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                defaultRole
            }

            prefs.edit().putString("cached_user_role", role.name).apply()

            AppUser(
                id = uid,
                name = firebaseUser.displayName ?: "Usuario Taller",
                role = role,
                email = firebaseUser.email ?: ""
            )
        } catch (e: Exception) {
            Log.w("AuthService", "No se pudo sincronizar rol con Firestore, usando caché local", e)
            val cachedRoleStr = prefs.getString("cached_user_role", defaultRole.name)
            val role = try {
                UserRole.valueOf(cachedRoleStr ?: defaultRole.name)
            } catch (_: Exception) {
                defaultRole
            }
            AppUser(
                id = uid,
                name = firebaseUser.displayName ?: "Usuario Taller",
                role = role,
                email = firebaseUser.email ?: ""
            )
        }
    }

    /**
     * Permite cambiar y guardar el rol del usuario (Administrador vs Mecánico) tanto local como en Firestore
     */
    suspend fun setUserRole(uid: String, newRole: UserRole): Result<Unit> {
        return try {
            prefs.edit().putString("cached_user_role", newRole.name).apply()

            val current = _currentUserState.value
            if (current != null && current.id == uid) {
                _currentUserState.value = current.copy(role = newRole)
            }

            firestore.collection("users").document(uid).set(
                hashMapOf(
                    "role" to newRole.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthService", "Error actualizando rol de usuario en Firestore", e)
            Result.failure(e)
        }
    }
}
