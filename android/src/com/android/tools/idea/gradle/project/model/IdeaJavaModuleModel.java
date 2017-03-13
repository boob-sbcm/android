/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.model;

import com.android.annotations.VisibleForTesting;
import com.android.tools.idea.gradle.model.java.JarLibraryDependency;
import com.android.tools.idea.gradle.model.java.JavaModuleContentRoot;
import com.android.tools.idea.gradle.model.java.JavaModuleDependency;
import com.intellij.openapi.util.Pair;
import org.gradle.tooling.model.idea.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExtIdeaCompilerOutput;
import org.jetbrains.plugins.gradle.model.ModuleExtendedModel;

import java.io.File;
import java.util.*;

/**
 * Represents JavaModuleModel instance created from IdeaModule and ModuleExtendedModel.
 */
@SuppressWarnings("deprecation")
public class IdeaJavaModuleModel extends JavaModuleModel {
  public IdeaJavaModuleModel(@NotNull IdeaModule ideaModule,
                             @Nullable ModuleExtendedModel javaModel,
                             boolean androidModuleWithoutVariants) {
    this(ideaModule.getName(), getContentRoots(ideaModule, javaModel), getDependencies(ideaModule),
         getArtifactsByConfiguration(javaModel), getCompilerOutput(javaModel), ideaModule.getGradleProject().getBuildDirectory(),
         getLanguageLevel(javaModel), !androidModuleWithoutVariants && isBuildable(ideaModule.getGradleProject()),
         androidModuleWithoutVariants);
  }

  @VisibleForTesting
  public IdeaJavaModuleModel(@NotNull String name,
                             @NotNull Collection<JavaModuleContentRoot> contentRoots,
                             @NotNull Pair<Collection<JavaModuleDependency>, Collection<JarLibraryDependency>> dependencies,
                             @NotNull Map<String, Set<File>> artifactsByConfiguration,
                             @Nullable ExtIdeaCompilerOutput compilerOutput,
                             @Nullable File buildFolderPath,
                             @Nullable String languageLevel,
                             boolean buildable,
                             boolean androidModuleWithoutVariants) {
    super(name, contentRoots, dependencies.first, dependencies.second, artifactsByConfiguration, compilerOutput, buildFolderPath,
          languageLevel, buildable, androidModuleWithoutVariants);
  }

  @Nullable
  private static ExtIdeaCompilerOutput getCompilerOutput(@Nullable ModuleExtendedModel javaModel) {
    return javaModel != null ? javaModel.getCompilerOutput() : null;
  }

  @NotNull
  private static Collection<JavaModuleContentRoot> getContentRoots(@NotNull IdeaModule ideaModule,
                                                                   @Nullable ModuleExtendedModel javaModel) {
    Collection<? extends IdeaContentRoot> contentRoots = javaModel != null ? javaModel.getContentRoots() : null;
    if (contentRoots == null) {
      contentRoots = ideaModule.getContentRoots();
    }
    Collection<JavaModuleContentRoot> javaModuleContentRoots = new ArrayList<>();

    if (contentRoots != null) {
      for (IdeaContentRoot contentRoot : contentRoots) {
        if (contentRoot != null) {
          javaModuleContentRoots.add(JavaModuleContentRoot.copy(contentRoot));
        }
      }
    }
    return javaModuleContentRoots;
  }

  @NotNull
  private static Map<String, Set<File>> getArtifactsByConfiguration(@Nullable ModuleExtendedModel javaModel) {
    Map<String, Set<File>> artifactsByConfiguration = Collections.emptyMap();
    if (javaModel != null) {
      artifactsByConfiguration = javaModel.getArtifactsByConfiguration();
    }
    return artifactsByConfiguration != null ? artifactsByConfiguration : Collections.emptyMap();
  }

  @NotNull
  private static Pair<Collection<JavaModuleDependency>, Collection<JarLibraryDependency>> getDependencies(@NotNull IdeaModule ideaModule) {
    List<? extends IdeaDependency> dependencies = ideaModule.getDependencies().getAll();
    Collection<JavaModuleDependency> javaModuleDependencies = new ArrayList<>();
    Collection<JarLibraryDependency> jarLibraryDependencies = new ArrayList<>();

    if (dependencies != null) {
      for (IdeaDependency dependency : dependencies) {
        if (dependency instanceof IdeaSingleEntryLibraryDependency) {
          JarLibraryDependency libraryDependency = JarLibraryDependency.copy((IdeaSingleEntryLibraryDependency)dependency);
          if (libraryDependency != null) {
            jarLibraryDependencies.add(libraryDependency);
          }
        }
        else if (dependency instanceof IdeaModuleDependency) {
          JavaModuleDependency moduleDependency = JavaModuleDependency.copy((IdeaModuleDependency)dependency);
          if (moduleDependency != null) {
            javaModuleDependencies.add(moduleDependency);
          }
        }
      }
    }
    return Pair.create(javaModuleDependencies, jarLibraryDependencies);
  }

  @Nullable
  private static String getLanguageLevel(@Nullable ModuleExtendedModel javaModel) {
    return javaModel != null ? javaModel.getJavaSourceCompatibility() : null;
  }
}
