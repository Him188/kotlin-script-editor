package script

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlin.random.nextUInt

class KotlinScriptRunner private constructor(
    private val tempDir: Path,
    parentCoroutineContext: CoroutineContext,
) : ScriptRunner {
    private val scope = CoroutineScope(parentCoroutineContext + SupervisorJob(parentCoroutineContext.job))

    override fun startSession(script: Script): ExecutionSession {
        return KotlinExecutionSession(scope.coroutineContext).apply {
            val temp = tempDir.resolve(getNextTempFilename())
            temp.writeText(script.content)

            startProcess(
                ProcessBuilder()
                    .command(KOTLINC_PATH, "--script", temp.absolutePathString())
            )
        }
    }

    companion object {
        private const val TEMP_FILE_SUFFIX: String = "temp"
        private val KOTLINC_PATH: String = System.getProperty("script.runner.kotlinc", "kotlinc")

        private fun getNextTempFilename() = "$TEMP_FILE_SUFFIX${Random.nextUInt()}.kts"

        fun create(
            tempDir: Path,
            parentCoroutineContext: CoroutineContext,
        ): KotlinScriptRunner = KotlinScriptRunner(tempDir, parentCoroutineContext)
    }
}

private class KotlinExecutionSession(
    parentCoroutineContext: CoroutineContext,
) : ExecutionSession {
    private val scope = CoroutineScope(parentCoroutineContext)

    private val _outputs: MutableSharedFlow<String> = MutableSharedFlow()
    override val outputs: SharedFlow<String> = _outputs.asSharedFlow() // readonly

    private val _state: MutableStateFlow<ExecutionState> = MutableStateFlow(ExecutionState.Initialized)
    override val state: StateFlow<ExecutionState> get() = _state


    fun startProcess(processBuilder: ProcessBuilder) {
        check(state.value == ExecutionState.Initialized)
        _state.value = ExecutionState.Running

        val process: Process = try {
            processBuilder.start()
        } catch (e: Exception) {
            // like IOException
            _state.value = ExecutionState.Failed(e)
            return
        }

        val processScope = CoroutineScope(scope.coroutineContext)

        processScope.launch(Dispatchers.IO) {
            // read inputs
            try {
                process.inputStream.bufferedReader().lineSequence().forEach { line ->
                    _outputs.emit(line)
                }
            } catch (_: CancellationException) {
                // ignored
            } catch (e: Throwable) {
                _state.value = ExecutionState.Failed(e)
                processScope.cancel()
            }
        }

        processScope.launch {
            // wait for the process to exit
            val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }
            _state.value = ExecutionState.Completed(exitCode)
        }.apply {
            invokeOnCompletion {
                processScope.cancel()
            }
        }
    }

    override fun cancel() {
        scope.cancel()
    }
}