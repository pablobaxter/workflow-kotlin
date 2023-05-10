@file:OptIn(WorkflowExperimentalRuntime::class)

package com.squareup.sample.helloworkflow

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.workflow1.WorkflowExperimentalRuntime
import com.squareup.workflow1.config.AndroidRuntimeConfigTools
import com.squareup.workflow1.ui.WorkflowLayout
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.renderWorkflowIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(WorkflowUiExperimentalApi::class)
class HelloWorkflowActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    StateCollector.collect()

    // This ViewModel will survive configuration changes. It's instantiated
    // by the first call to viewModels(), and that original instance is returned by
    // succeeding calls.
    val model: HelloViewModel by viewModels()
    setContentView(
      WorkflowLayout(this).apply { take(lifecycle, model.renderings) }
    )
  }
}

class HelloViewModel(savedState: SavedStateHandle) : ViewModel() {
  @OptIn(WorkflowUiExperimentalApi::class, WorkflowExperimentalRuntime::class)
  val renderings by lazy {
    renderWorkflowIn(
      workflow = ParentWorkflow,
      scope = scope,
      savedStateHandle = savedState,
      runtimeConfig = AndroidRuntimeConfigTools.getAppWorkflowRuntimeConfig()
    )
  }
}
