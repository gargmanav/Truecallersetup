package com.example.testtruecaller

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.example.testoauth.databinding.ActivitySignedInBinding
import com.example.testoauth.networking.accessToken.OAuthAccessTokenResponse
import com.example.testoauth.networking.accessToken.OAuthAccessTokenService
import com.example.testoauth.networking.profile.OAuthProfileService
import com.example.testtruecaller.networking.RetrofitAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.truecaller.android.sdk.oAuth.TcOAuthData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class SignedInActivity : Activity() {
    private var accessTokenService: OAuthAccessTokenService? = null
    private var profileService: OAuthProfileService? = null
    private var binding: ActivitySignedInBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignedInBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        val oAuthData = intent.getParcelableExtra<TcOAuthData>("data")
        val requestedState = intent.getStringExtra("state")
        val codeVerifier = intent.getStringExtra("cv")
        binding.accessTokenBtn.setOnClickListener { view ->
            fetchAccessToken(
                oAuthData,
                codeVerifier
            )
        }
        binding.signedInTv.setText(
            String.format(
                getString(R.string.large_text),
                oAuthData!!.authorizationCode, codeVerifier,
                oAuthData.state, requestedState, oAuthData.scopesGranted
            )
        )
        createRetrofitService()
    }

    private fun createRetrofitService() {
        accessTokenService = RetrofitAdapter.createService(
            OAuthAccessTokenService.BaseUrl.FETCH_ACCESS_TOKEN_BASE_URL,
            OAuthAccessTokenService::class.java
        )
        profileService = RetrofitAdapter.createService(
            OAuthProfileService.BaseUrl.FETCH_PROFILE_BASE_URL,
            OAuthProfileService::class.java
        )
    }

    private fun fetchAccessToken(oAuthData: TcOAuthData?, codeVerifier: String?) {
        setAccessTokenText("Fetching access token...")
        accessTokenService!!.fetchAccessToken(
            "authorization_code",
            getString(R.string.clientId),
            oAuthData!!.authorizationCode,
            codeVerifier!!
        ).enqueue(object : Callback<OAuthAccessTokenResponse?> {
            override fun onResponse(
                call: Call<OAuthAccessTokenResponse?>,
                response: Response<OAuthAccessTokenResponse?>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val accessToken = response.body()!!.accessToken
                    setAccessTokenText(String.format(getString(R.string.access_token), accessToken))
                    binding.profileBtn.setOnClickListener { view -> fetchProfile(accessToken) }
                } else {
                    Toast.makeText(
                        this@SignedInActivity,
                        "Unable to fetch access token",
                        Toast.LENGTH_SHORT
                    ).show()
                    setAccessTokenText("error: " + response.message())
                }
            }

            override fun onFailure(call: Call<OAuthAccessTokenResponse?>, t: Throwable) {
                val msg = "Unable to fetch access token: " + t.message
                Toast.makeText(this@SignedInActivity, msg, Toast.LENGTH_SHORT).show()
                setAccessTokenText(msg)
            }
        })
    }

    private fun fetchProfile(accessToken: String) {
        setProfileText("Fetching profile...")
        val bearerToken = String.format(getString(R.string.bearer_token), accessToken)
        profileService!!.fetchProfile(bearerToken).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonElement = JsonParser().parse(
                            response.body()!!.string()
                        )
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        setProfileText(gson.toJson(jsonElement))
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@SignedInActivity,
                            "Unable to fetch profile exception",
                            Toast.LENGTH_SHORT
                        ).show()
                        setProfileText("exception: " + e.message)
                    }
                } else {
                    Toast.makeText(
                        this@SignedInActivity,
                        "Unable to fetch profile",
                        Toast.LENGTH_SHORT
                    ).show()
                    setProfileText("error: " + response.message())
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                val msg = "Unable to fetch profile: " + t.message
                Toast.makeText(this@SignedInActivity, msg, Toast.LENGTH_SHORT).show()
                setProfileText(msg)
            }
        })
    }

    private fun setAccessTokenText(text: String) {
        binding.accessTokenTv.setText(text)
    }

    private fun setProfileText(text: String) {
        binding.profileDetailsTv.setText(text)
    }
}

