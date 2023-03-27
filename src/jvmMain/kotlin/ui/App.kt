package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.runningReduce
import script.*
import java.nio.file.Paths
import kotlin.coroutines.EmptyCoroutineContext

@Composable
@Preview
fun App() {
    val backgroundScope = CoroutineScope(EmptyCoroutineContext)
    val scriptRunner = remember {
        SingleInstanceScriptRunner(KotlinScriptRunner.create(Paths.get(System.getProperty("user.dir")), backgroundScope.coroutineContext))
    }
    val currentScriptSession by scriptRunner.currentSession.collectAsState()
    val executionState by currentScriptSession?.state?.collectAsState() ?: nullState()

    var source by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        // Source area
        TextField(
            source,
            onValueChange = {
                source = it
            },
            placeholder = { Text("Enter source here") },
            modifier = Modifier.fillMaxHeight().padding(12.dp),
            enabled = true,
            readOnly = false,
        )

        // Bottoms in the middle
        Column(Modifier.fillMaxHeight().width(100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (!executionState.isRunningOrInitialized()) {
                        scriptRunner.startSession(Script(source))
                    }
                },
                modifier = Modifier.padding(12.dp).height(40.dp).width(80.dp),
            ) {
                if (executionState.isRunningOrInitialized()) {
                    CircularProgressIndicator(color = MaterialTheme.colors.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Run!")
                }
            }
        }

        val outputState = currentScriptSession?.outputs
            ?.runningReduce { accumulator, value -> accumulator + "" + value }
            ?.collectAsState("")

        // Outputs
        Box(Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (val state = executionState) {
                    is ExecutionState.Failed -> {
                        Text(
                            "Execution failed: \n" + state.exception?.message.orEmpty(),
                            modifier = Modifier.fillMaxHeight().padding(12.dp),
                        )
                    }

                    else -> {
                        if (state is ExecutionState.Completed && !state.isSuccess) {
                            Text("Exit code: ${state.exitCode}")
                        } else {
                            Text("Click 'Run!' to execute the script and see live output here")
                        }

                        OutlinedTextField(
                            outputState?.value ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxHeight().padding(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun ExecutionState?.isRunningOrInitialized() = this is ExecutionState.Initialized || this is ExecutionState.Running

private fun nullState() = mutableStateOf(null)

@Composable
@Preview
private fun PreviewApp() {
    Box(Modifier.size(width = 800.dp, height = 450.dp).border(2.dp, Color.Green)) {
        App()
    }
}