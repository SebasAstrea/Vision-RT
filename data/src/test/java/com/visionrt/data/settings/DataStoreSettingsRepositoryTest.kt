package com.visionrt.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.visionrt.core.domain.Verbosity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** One DataStore at a time per file: always cancel the previous scope. */
    private fun newStore(file: File): Pair<DataStore<Preferences>, CoroutineScope> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return store to scope
    }

    @Test
    fun defaultsAreFalseAndNormalVerbosity() {
        val (store, scope) = newStore(tmp.newFile("s.preferences_pb"))
        runBlocking {
            val repo = DataStoreSettingsRepository(store)
            assertFalse(repo.onboardingAcknowledged.first())
            assertFalse(repo.trainingCompleted.first())
            assertFalse(repo.speechMuted.first())
            assertEquals(Verbosity.NORMAL, repo.verbosity.first())
        }
        scope.cancel()
    }

    @Test
    fun acknowledgedFlagPersistsAcrossInstances() {
        val file = tmp.newFile("s.preferences_pb")
        runBlocking {
            val (first, firstScope) = newStore(file)
            DataStoreSettingsRepository(first).setOnboardingAcknowledged(true)
            firstScope.cancel()

            val (second, secondScope) = newStore(file)
            assertTrue(DataStoreSettingsRepository(second).onboardingAcknowledged.first())
            secondScope.cancel()
        }
    }

    @Test
    fun verbosityRoundTripsAllValues() {
        runBlocking {
            val (store, scope) = newStore(tmp.newFile("s.preferences_pb"))
            val repo = DataStoreSettingsRepository(store)
            Verbosity.entries.forEach { v ->
                repo.setVerbosity(v)
                assertEquals(v, repo.verbosity.first())
            }
            scope.cancel()
        }
    }

    @Test
    fun speechMutedPersistsAcrossInstances() {
        val file = tmp.newFile("s.preferences_pb")
        runBlocking {
            val (first, firstScope) = newStore(file)
            DataStoreSettingsRepository(first).setSpeechMuted(true)
            firstScope.cancel()

            val (second, secondScope) = newStore(file)
            assertTrue(DataStoreSettingsRepository(second).speechMuted.first())
            secondScope.cancel()
        }
    }

    @Test
    fun speechMutedRoundTrips() {
        runBlocking {
            val (store, scope) = newStore(tmp.newFile("s.preferences_pb"))
            val repo = DataStoreSettingsRepository(store)
            repo.setSpeechMuted(true)
            assertTrue(repo.speechMuted.first())
            repo.setSpeechMuted(false)
            assertFalse(repo.speechMuted.first())
            scope.cancel()
        }
    }
}
