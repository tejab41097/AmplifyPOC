package com.example.amplifypoc

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.amplify.generated.graphql.CreatePetMutation
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.exception.ApolloException
import type.CreatePetInput
import javax.annotation.Nonnull


class AddPetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)
//        val btnAddItem: Button = findViewById(R.id.btn_save)

//        btnAddItem.setOnClickListener(object : View.OnClickListener {
//            override fun onClick(view: View?) {
//                save()
//            }
//        })
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
        ClientFactory.appSyncClient()!!.mutate(addPetMutation).enqueue(mutateCallback)
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

            override fun onResponse(response: com.apollographql.apollo.api.Response<CreatePetMutation.Data?>) {
                runOnUiThread {
                    Toast.makeText(this@AddPetActivity, "Added pet", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
}
