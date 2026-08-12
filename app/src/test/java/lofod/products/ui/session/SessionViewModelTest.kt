package lofod.products.ui.session

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import lofod.products.MainDispatcherRule
import lofod.products.data.remote.SessionExpiredNotifier
import lofod.products.data.repository.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sessionExpired_401_clearsSessionAndNavigatesToLogin() = runTest(mainDispatcherRule.dispatcher) {
        val authRepository: AuthRepository = mockk(relaxed = true)
        coEvery { authRepository.restoreSessionIfValid() } returns true
        val sessionExpiredNotifier = SessionExpiredNotifier()

        val viewModel = SessionViewModel(authRepository, sessionExpiredNotifier)
        assertEquals(SessionBootstrapState.Authenticated, viewModel.bootstrapState.value)

        val events = mutableListOf<SessionNavEvent>()
        backgroundScope.launch {
            viewModel.navEvents.collect { events.add(it) }
        }

        sessionExpiredNotifier.notifySessionExpired()

        assertEquals(listOf(SessionNavEvent.ToLogin), events)
        assertEquals(SessionBootstrapState.Unauthenticated, viewModel.bootstrapState.value)
        coVerify(exactly = 1) { authRepository.clearSession() }
    }

    @Test
    fun restoreSession_invalid_leavesUnauthenticated() = runTest(mainDispatcherRule.dispatcher) {
        val authRepository: AuthRepository = mockk(relaxed = true)
        coEvery { authRepository.restoreSessionIfValid() } returns false

        val viewModel = SessionViewModel(authRepository, SessionExpiredNotifier())

        assertEquals(SessionBootstrapState.Unauthenticated, viewModel.bootstrapState.value)
    }
}
