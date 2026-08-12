package lofod.products

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import lofod.products.data.local.SessionDataStore
import lofod.products.data.local.SessionTokenHolder
import javax.inject.Inject

@HiltAndroidApp
class ProductsApp : Application() {

    @Inject
    lateinit var sessionDataStore: SessionDataStore

    @Inject
    lateinit var sessionTokenHolder: SessionTokenHolder

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            sessionTokenHolder.token = sessionDataStore.getToken()
        }
    }
}
