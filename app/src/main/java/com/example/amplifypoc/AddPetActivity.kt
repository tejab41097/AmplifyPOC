package com.example.amplifypoc

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.amplify.generated.graphql.CreatePetMutation
import com.amazonaws.amplify.generated.graphql.CreatePetMutation.CreatePet
import com.amazonaws.amplify.generated.graphql.ListPetsQuery
import com.amazonaws.amplify.generated.graphql.ListPetsQuery.ListPets
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.amplifypoc.ClientFactory.appSyncClient
import type.CreatePetInput
import java.util.*
import javax.annotation.Nonnull
import kotlin.collections.ArrayList


class AddPetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)
    }

    fun savePet(view: View) {
        save()
    }

    private fun save() {
        val name = (findViewById<EditText>(R.id.editTxt_name)).text.toString()
        val description =
            (findViewById<EditText>(R.id.editText_description)).text.toString()
        val input = CreatePetInput.builder()
            .name(name)
            .description(description)
            .build()
        val addPetMutation = CreatePetMutation.builder()
            .input(input)
            .build()
        //ClientFactory.appSyncClient()!!.mutate(addPetMutation).enqueue(mutateCallback)
        ClientFactory.appSyncClient()!!.mutate(addPetMutation)
            .refetchQueries(ListPetsQuery.builder().build()).enqueue(mutateCallback)
        addPetOffline(input)
    }

    // Mutation callback code
    private val mutateCallback: GraphQLCall.Callback<CreatePetMutation.Data?> =
        object : GraphQLCall.Callback<CreatePetMutation.Data?>() {
            override fun onFailure(@Nonnull e: ApolloException) {
                runOnUiThread {
                    Log.e("", "Failed to perform AddPetMutation", e)
                    Toast.makeText(this@AddPetActivity, "Failed to add pet", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }

            override fun onResponse(response: Response<CreatePetMutation.Data?>) {
                runOnUiThread {
                    Toast.makeText(this@AddPetActivity, "Added pet", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

    private fun addPetOffline(input: CreatePetInput) {
        val expected = CreatePet(
            "Pet",
            UUID.randomUUID().toString(),
            input.name(),
            input.description()
        )
        val awsAppSyncClient = appSyncClient()
        val listEventsQuery = ListPetsQuery.builder().build()
        awsAppSyncClient!!.query(
            listEventsQuery
        )
            .responseFetcher(AppSyncResponseFetchers.CACHE_ONLY)
            .enqueue(object : GraphQLCall.Callback<ListPetsQuery.Data?>() {
                override fun onResponse(@Nonnull response: Response<ListPetsQuery.Data?>) {
                    val items: MutableList<ListPetsQuery.Item> = ArrayList()
                    if (response.data() != null) {
                        response.data()!!.listPets()!!.items()?.let { items.addAll(it) }
                    }
                    items.add(
                        ListPetsQuery.Item(
                            expected.__typename(),
                            expected.id(),
                            expected.name(),
                            expected.description()
                        )
                    )
                    val data =
                        ListPetsQuery.Data(ListPets("ModelPetConnection", items, null))
                    awsAppSyncClient.store
                        .write(
                            listEventsQuery,
                            data
                        ).enqueue(null)
                    Log.d(
                        "Message",
                        "Successfully wrote item to local store while being offline."
                    )
                    finishIfOffline()
                }

                override fun onFailure(@Nonnull e: ApolloException) {
                    Log.e(
                        "Message",
                        "Failed to update event query list.",
                        e
                    )
                }
            })
    }

    private fun finishIfOffline() { // Close the add activity when offline otherwise allow callback to close
        val cm =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting
        if (!isConnected) {
            Log.d("Message", "App is offline. Returning to MainActivity .")
            finish()
        }
    }
}
