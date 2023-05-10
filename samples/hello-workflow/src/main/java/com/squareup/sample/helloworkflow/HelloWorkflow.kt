package com.squareup.sample.helloworkflow

import com.squareup.sample.helloworkflow.HelloWorkflow.State
import com.squareup.sample.helloworkflow.HelloWorkflow.State.Goodbye
import com.squareup.sample.helloworkflow.HelloWorkflow.State.Hello
import com.squareup.sample.helloworkflow.HelloWorkflow.State.Initial
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.StatelessWorkflow
import com.squareup.workflow1.WorkflowAction
import com.squareup.workflow1.action
import com.squareup.workflow1.parse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val globalState = MutableStateFlow(State.Initial)

val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
object StateCollector {
  val collectedState = MutableStateFlow(State.Initial)
  fun collect() = scope
    .launch { globalState.collect { collectedState.value = it } }
}

object HelloWorkflow : StatefulWorkflow<Unit, State, State, HelloRendering>() {
  enum class State {
    Initial,
    Hello,
    Goodbye
  }

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = snapshot?.bytes?.parse { source -> if (source.readInt() == 1) Hello else Goodbye }
    ?: Hello

  override fun render(
    renderProps: Unit,
    renderState: State,
    context: RenderContext
  ): HelloRendering {
    return HelloRendering(
      message = renderState.name,
      onClick = {
        context.actionSink.send(helloAction)
      }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.of(if (state == Hello) 1 else 0)

  private val helloAction = action {
    state = when (state) {
      Hello -> Goodbye
      Goodbye -> Hello
      Initial -> Hello
    }

    globalState.value = state

    setOutput(state)
  }
}

object ParentWorkflow : StatelessWorkflow<Unit, Unit, HelloRendering>() {
  override fun render(
    renderProps: Unit,
    context: RenderContext
  ): HelloRendering {
    return context.renderChild(HelloWorkflow, Unit) { output ->
      val newState = StateCollector.collectedState.value
      assert(output == newState) {
        "Expected $output, but was $newState"
      }

      WorkflowAction.noAction()
    }
  }
}
