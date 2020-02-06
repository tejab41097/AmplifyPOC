package com.example.amplifypoc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.amplify.generated.graphql.ListPetsQuery
import com.amazonaws.amplify.generated.graphql.OnCreatePetSubscription
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.amplifypoc.ClientFactory.appSyncClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import javax.annotation.Nonnull


class MainActivity : AppCompatActivity() {

    var mRecyclerView: RecyclerView? = null
    var mAdapter: MyAdapter? = null
    private var subscriptionWatcher: AppSyncSubscriptionCall<*>? = null

    private var mPets: ArrayList<ListPetsQuery.Item>? = null
    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
        mAdapter = MyAdapter(this)
        mRecyclerView!!.adapter = mAdapter
        ClientFactory.init(this)

        val btnAddPet: FloatingActionButton = findViewById(R.id.btn_addPet)
        btnAddPet.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val addPetIntent = Intent(this@MainActivity, AddPetActivity::class.java)
                this@MainActivity.startActivity(addPetIntent)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        query()
        subscribe()
    }

    fun query() {
        ClientFactory.appSyncClient()!!.query(ListPetsQuery.builder().build())
            .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            .enqueue(queryCallback)
    }

    private val queryCallback: GraphQLCall.Callback<ListPetsQuery.Data?> =
        object : GraphQLCall.Callback<ListPetsQuery.Data?>() {


            override fun onFailure(@Nonnull e: ApolloException) {
                Log.e(TAG, e.toString())
            }

            override fun onResponse(response: com.apollographql.apollo.api.Response<ListPetsQuery.Data?>) {
                mPets = response.data()!!.listPets()!!.items()?.let { ArrayList(it) }
                Log.i(TAG, "Retrieved list items: " + mPets.toString())
                runOnUiThread {
                    mAdapter!!.setItems(mPets!!)
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        }

    private fun subscribe() {
        val subscription = OnCreatePetSubscription.builder().build()
        subscriptionWatcher = appSyncClient()!!
            .subscribe(
                subscription
            )
        (subscriptionWatcher as AppSyncSubscriptionCall<OnCreatePetSubscription.Data>?)!!.execute(
            subCallback as AppSyncSubscriptionCall.Callback<OnCreatePetSubscription.Data>
        )
    }

    private val subCallback: AppSyncSubscriptionCall.Callback<*> =
        object : AppSyncSubscriptionCall.Callback<Any?> {
            override fun onFailure(@Nonnull e: ApolloException) {
                Log.e("Error", e.toString())
            }

            override fun onCompleted() {
                Log.i("Completed", "Subscription completed")
            }

            override fun onResponse(response: Response<Any?>) {
                Log.i(
                    "Response",
                    "Received subscription notification: " + response.data().toString()
                )
                val data =
                    (response.data() as OnCreatePetSubscription.Data).onCreatePet()
                val addedItem = ListPetsQuery.Item(
                    data!!.__typename(),
                    data.id(),
                    data.name(),
                    data.description()
                )
                runOnUiThread {
                    mPets!!.add(addedItem)
                    mAdapter!!.notifyItemInserted(mPets!!.size - 1)
                }
            }
        }

    override fun onStop() {
        super.onStop()
        subscriptionWatcher!!.cancel()
    }
}
