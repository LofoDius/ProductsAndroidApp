package lofod.products.ui.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import lofod.products.MainDispatcherRule
import lofod.products.data.repository.AuthRepository
import lofod.products.domain.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    private fun createViewModel() {
        authRepository = mockk(relaxed = true)
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun login_success_callsRepositoryAndNavigatesToCatalog() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel()
        coEvery { authRepository.login("alice", "secret1") } returns UserSession(
            userId = "u1",
            username = "alice",
            token = "tok-1"
        )
        viewModel.onLoginUsernameChange("alice")
        viewModel.onLoginPasswordChange("secret1")

        val events = mutableListOf<AuthNavEvent>()
        backgroundScope.launch {
            viewModel.navEvents.collect { events.add(it) }
        }

        viewModel.login()

        assertEquals(listOf(AuthNavEvent.ToCatalog), events)
        assertFalse(viewModel.loginState.value.isLoading)
        assertNull(viewModel.loginState.value.errorMessage)
        coVerify(exactly = 1) { authRepository.login("alice", "secret1") }
    }

    @Test
    fun login_error_setsErrorMessageAndDoesNotNavigate() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel()
        coEvery { authRepository.login(any(), any()) } throws IOException("offline")
        viewModel.onLoginUsernameChange("alice")
        viewModel.onLoginPasswordChange("secret1")

        val events = mutableListOf<AuthNavEvent>()
        backgroundScope.launch {
            viewModel.navEvents.collect { events.add(it) }
        }

        viewModel.login()

        assertTrue(events.isEmpty())
        assertFalse(viewModel.loginState.value.isLoading)
        assertEquals("Нет соединения с сервером", viewModel.loginState.value.errorMessage)
        coVerify(exactly = 1) { authRepository.login("alice", "secret1") }
    }

    @Test
    fun login_validationFailure_doesNotCallRepository() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel()
        viewModel.onLoginUsernameChange("")
        viewModel.onLoginPasswordChange("short")

        viewModel.login()

        assertEquals("Введите имя пользователя", viewModel.loginState.value.usernameError)
        assertTrue(viewModel.loginState.value.passwordError != null)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun login_trimsUsernameBeforeRepositoryCall() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel()
        coEvery { authRepository.login("bob", "secret1") } returns UserSession(
            userId = "u2",
            username = "bob",
            token = "tok-2"
        )
        viewModel.onLoginUsernameChange("  bob  ")
        viewModel.onLoginPasswordChange("secret1")

        viewModel.login()

        coVerify(exactly = 1) { authRepository.login("bob", "secret1") }
    }
}
