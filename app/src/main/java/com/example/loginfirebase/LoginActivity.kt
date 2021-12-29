package com.example.loginfirebase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.loginfirebase.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val GOOGLE_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Analytics Event
        val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message", "Integracin de Firebase completa")
        analytics.logEvent("InitScreen", bundle)

        // Setup
        setup()
        session()

        binding.backImage.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        binding.authLayout.setOnClickListener { v ->
            hideKeyboard(v)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.authLayout.visibility = View.VISIBLE


    }

    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file),
            Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email != null && provider != null) {
            binding.authLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup() {
        title = "Autenticacion"
        binding.loginButton.setOnClickListener {

            if (binding.emailEt.text!!.isNotEmpty() && binding.passWordet.text!!.isNotEmpty()) {

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        binding.emailEt.text.toString(),
                        binding.passWordet.text.toString()
                    ).addOnCompleteListener {

                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.EMAIL_PASSWORD)
                        } else {
                            showAlert()
                        }
                    }
            }


        }

        binding.googleButton.setOnClickListener {
            // COnfirguracion
            val googleConf: GoogleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_id))
                    .requestEmail()
                    .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }


    }


    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando el usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)

                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken,
                        null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                showHome(account.email ?: "", ProviderType.GOOGLE)

                            } else {
                                showAlert()

                            }
                        }
                }

            } catch (e: ApiException) {
                showAlert()
            }

        }
    }


    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)


    }

    fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

    }


}