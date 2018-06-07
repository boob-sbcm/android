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
package com.android.tools.idea.gradle.dsl.api.android.externalNativeBuild;

import com.android.tools.idea.gradle.dsl.api.values.GradleNotNullValue;
import com.android.tools.idea.gradle.dsl.api.values.GradleNullableValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AdbOptionsModel {
  @Nullable
  List<GradleNotNullValue<String>> installOptions();

  @NotNull
  AdbOptionsModel addInstallOption(@NotNull String installOption);

  @NotNull
  AdbOptionsModel removeInstallOption(@NotNull String installOption);

  @NotNull
  AdbOptionsModel removeAllInstallOptions();

  @NotNull
  AdbOptionsModel replaceInstallOption(@NotNull String oldInstallOption, @NotNull String newInstallOption);

  @NotNull
  GradleNullableValue<Integer> timeOutInMs();

  @NotNull
  AdbOptionsModel setTimeOutInMs(int timeOutInMs);

  @NotNull
  AdbOptionsModel removeTimeOutInMs();
}