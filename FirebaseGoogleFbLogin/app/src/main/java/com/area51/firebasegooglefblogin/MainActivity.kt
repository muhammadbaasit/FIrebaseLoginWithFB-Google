package com.area51.firebasegooglefblogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(){

    private var callbackManager: CallbackManager?=null
    private lateinit var auth: FirebaseAuth
    private var accessToken:AccessToken?=null
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var loginButton = findViewById<LoginButton>(R.id.login_button)

        accessToken = AccessToken.getCurrentAccessToken()
        //val isLoggedIn = accessToken != null && accessToken!!.isExpired

        auth = FirebaseAuth.getInstance()

        callbackManager = CallbackManager.Factory.create()

        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {

            override fun onSuccess(loginResult: LoginResult?) {
                Log.d("APPINFO","LoggedIn success :"+loginResult)
                handleFacebookAccessToken(loginResult!!.accessToken)

            }
            override fun onCancel() {
                Log.d("APPINFO","LoggedIn Cancle :")
            }
            override fun onError(exception: FacebookException) {

                Log.d("APPINFO","LoggedIn Error :")
            }
        })

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        Log.d("APPINFO","USER:"+currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                handleSignInResult(task)
                val account = task.getResult(ApiException::class.java)!!
                Log.d("APPINFO", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("APPINFO", "Google sign in failed", e)
                // ...
            }
        }

        login_button.setOnClickListener{
            accessToken = AccessToken.getCurrentAccessToken()

            if(accessToken == null){
                Log.d("APPINFO","NULL : $accessToken")
            } else {
                Log.d("APPINFO","NOT NULL :$accessToken")
                FirebaseAuth.getInstance().signOut()
            }
        }

        sign_in_button.setOnClickListener {
            signIn()
        }

        gLogout.setOnClickListener {
            signOut()
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("APPINFO", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("APPINFO", "signInWithCredential:success")
                    val user = auth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("APPINFO", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this, object : OnCompleteListener<AuthResult?>{

                override fun onComplete(task: Task<AuthResult?>) {

                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("APPINFO", "signInWithCredential:success")
                        val user: FirebaseUser = auth.getCurrentUser()!!

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.d(
                            "APPINFO",
                            "signInWithCredential:failure",
                            task.exception
                        )
                    }
                }
            })
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {

        if (completedTask.isSuccessful){
            /*var intent = Intent(applicationContext,GoogleActivity::class.java)
            startActivity(intent)*/
            sign_in_button.visibility = View.GONE
            login_button.visibility = View.GONE
            gLogout.visibility = View.VISIBLE
        }

        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully
            val googleId = account?.id ?: ""
            Log.i("Google ID",googleId)

            val googleFirstName = account?.givenName ?: ""
            Log.i("Google First Name", googleFirstName)

            val googleLastName = account?.familyName ?: ""
            Log.i("Google Last Name", googleLastName)

            val googleEmail = account?.email ?: ""
            Log.i("Google Email", googleEmail)

            val googleProfilePicURL = account?.photoUrl.toString()
            Log.i("Google Profile Pic URL", googleProfilePicURL)

            val googleIdToken = account?.idToken ?: ""
            Log.i("Google ID Token", googleIdToken)

        } catch (e: ApiException) {
            // Sign in was unsuccessful
            Log.e("failed code=", e.statusCode.toString())
        }
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                sign_in_button.visibility = View.VISIBLE
                login_button.visibility = View.VISIBLE
                gLogout.visibility = View.GONE
                FirebaseAuth.getInstance().signOut()
            }
    }
}
