/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync.idea;

import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.android.tools.idea.gradle.project.sync.messages.GradleSyncMessages;
import com.android.tools.idea.gradle.project.sync.setup.post.PostSyncProjectSetup;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.util.ExceptionUtil.getRootCause;
import static com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded;

public class IdeaSyncPopulateProjectTask {
  @NotNull private final Project myProject;
  @NotNull private final PostSyncProjectSetup myProjectSetup;
  @NotNull private final GradleSyncState mySyncState;
  @NotNull private final ProjectDataManager myDataManager;

  public IdeaSyncPopulateProjectTask(@NotNull Project project) {
    this(project, PostSyncProjectSetup.getInstance(project), GradleSyncState.getInstance(project),
         ProjectDataManager.getInstance());
  }

  @VisibleForTesting
  IdeaSyncPopulateProjectTask(@NotNull Project project,
                              @NotNull PostSyncProjectSetup projectSetup,
                              @NotNull GradleSyncState syncState,
                              @NotNull ProjectDataManager dataManager) {
    myProject = project;
    myProjectSetup = projectSetup;
    mySyncState = syncState;
    myDataManager = dataManager;
  }

  public void populateProject(@NotNull DataNode<ProjectData> projectInfo) {
    populateProject(projectInfo, null, null);
  }

  public void populateProject(@NotNull DataNode<ProjectData> projectInfo,
                              @Nullable PostSyncProjectSetup.Request setupRequest,
                              @Nullable Runnable syncFinishedCallback) {
    doPopulateProject(projectInfo, setupRequest, syncFinishedCallback);
  }

  private void doPopulateProject(@NotNull DataNode<ProjectData> projectInfo,
                                 @Nullable PostSyncProjectSetup.Request setupRequest,
                                 @Nullable Runnable syncFinishedCallback) {
    invokeAndWaitIfNeeded((Runnable)() -> GradleSyncMessages.getInstance(myProject).removeProjectMessages());

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      populate(projectInfo, new EmptyProgressIndicator(), setupRequest, syncFinishedCallback);
      return;
    }

    Task.Backgroundable task = new Task.Backgroundable(myProject, "Project Setup", false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        populate(projectInfo, indicator, setupRequest, syncFinishedCallback);
      }
    };
    task.queue();
  }

  private void populate(@NotNull DataNode<ProjectData> projectInfo,
                        @NotNull ProgressIndicator indicator,
                        @Nullable PostSyncProjectSetup.Request setupRequest,
                        @Nullable Runnable syncFinishedCallback) {
    doPopulateProject(projectInfo, myProject, setupRequest);
    if (syncFinishedCallback != null) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        syncFinishedCallback.run();
      }
      else {
        TransactionGuard.getInstance().submitTransactionLater(myProject, syncFinishedCallback);
      }
    }
    if (setupRequest != null) {
      PostSyncProjectSetup.getInstance(myProject).setUpProject(setupRequest, indicator);
    }
  }

  /**
   * Reuse external system 'selective import' feature for importing of the project sub-set.
   * And do not ignore projectNode children data, e.g. project libraries
   */
  @VisibleForTesting
  void doPopulateProject(@NotNull DataNode<ProjectData> projectInfo,
                         @NotNull Project project,
                         @Nullable PostSyncProjectSetup.Request setupRequest) {
    try {
      myDataManager.importData(projectInfo, project, true /* synchronous */);
    }
    catch (Throwable unexpected) {
      String message = getRootCause(unexpected).getMessage();
      Logger.getInstance(getClass()).warn("Sync failed: " + message, unexpected);

      // See https://code.google.com/p/android/issues/detail?id=268806
      if (setupRequest != null && setupRequest.usingCachedGradleModels) {
        // This happened when a newer version of IDEA cannot read the cache of a Gradle project created with an older IDE version.
        // Request a full sync.
        myProjectSetup.onCachedModelsSetupFailure(setupRequest);
        return;
      }

      // Notify sync failed, so the "Sync" action is enabled again.
      mySyncState.syncFailed(message);
    }
  }
}
