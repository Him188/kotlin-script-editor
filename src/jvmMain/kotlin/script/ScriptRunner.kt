package script

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

interface ScriptRunner {

    @Throws(IOException::class) // if failed to write temp file
    fun startSession(script: Script): ExecutionSession
}


// not thread-safe
class SingleInstanceScriptRunner(
    private val delegate: ScriptRunner,
) : ScriptRunner {
    private var _currentSession: MutableStateFlow<ExecutionSession?> = MutableStateFlow(null)

    @Stable
    val currentSession: StateFlow<ExecutionSession?> = _currentSession.asStateFlow() // readonly

    override fun startSession(script: Script): ExecutionSession {
        _currentSession.value?.cancel()

        return delegate.startSession(script).also {
            _currentSession.value = it
        }
    }
}

data class Script(
    val content: String,
)


interface ExecutionSession {
    @Stable
    val outputs: SharedFlow<String>

    @Stable
    val state: StateFlow<ExecutionState>

    fun cancel()
}

sealed class ExecutionState {
    object Initialized : ExecutionState()
    object Running : ExecutionState()


    class Completed(
        val exitCode: Int,
    ) : ExecutionState()

    // Failed with internal errors
    class Failed(val exception: Throwable?) : ExecutionState()
}

val ExecutionState.Completed.isSuccess get() = exitCode == 0