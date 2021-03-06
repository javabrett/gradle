/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.artifacts;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.InputArtifactDependencies;
import org.gradle.api.internal.artifacts.ivyservice.DefaultIvyContextManager;
import org.gradle.api.internal.artifacts.ivyservice.IvyContextManager;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.DefaultLocalComponentMetadataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.LocalComponentMetadataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.DefaultDependencyDescriptorFactory;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.DefaultExcludeRuleConverter;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.DefaultLocalConfigurationMetadataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.DependencyDescriptorFactory;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.ExcludeRuleConverter;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.ExternalModuleIvyDependencyDescriptorFactory;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.LocalConfigurationMetadataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.ProjectIvyDependencyDescriptorFactory;
import org.gradle.api.internal.artifacts.transform.ArtifactTransformActionScheme;
import org.gradle.api.internal.artifacts.transform.ArtifactTransformParameterScheme;
import org.gradle.api.internal.artifacts.transform.InputArtifactAnnotationHandler;
import org.gradle.api.internal.artifacts.transform.InputArtifactDependenciesAnnotationHandler;
import org.gradle.api.internal.tasks.properties.InspectionScheme;
import org.gradle.api.internal.tasks.properties.InspectionSchemeFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.cache.internal.ProducerGuard;
import org.gradle.internal.instantiation.InstantiationScheme;
import org.gradle.internal.instantiation.InstantiatorFactory;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.local.FileResourceConnector;
import org.gradle.internal.resource.local.FileResourceRepository;
import org.gradle.internal.resource.transport.file.FileConnectorFactory;

import javax.inject.Inject;

class DependencyManagementGlobalScopeServices {
    FileResourceRepository createFileResourceRepository(FileSystem fileSystem) {
        return new FileResourceConnector(fileSystem);
    }

    ImmutableModuleIdentifierFactory createModuleIdentifierFactory() {
        return new DefaultImmutableModuleIdentifierFactory();
    }

    IvyContextManager createIvyContextManager() {
        return new DefaultIvyContextManager();
    }

    ExcludeRuleConverter createExcludeRuleConverter(ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
        return new DefaultExcludeRuleConverter(moduleIdentifierFactory);
    }

    VersionComparator createVersionComparator() {
        return new DefaultVersionComparator();
    }

    ExternalModuleIvyDependencyDescriptorFactory createExternalModuleDependencyDescriptorFactory(ExcludeRuleConverter excludeRuleConverter) {
        return new ExternalModuleIvyDependencyDescriptorFactory(excludeRuleConverter);
    }

    DependencyDescriptorFactory createDependencyDescriptorFactory(ExcludeRuleConverter excludeRuleConverter, ExternalModuleIvyDependencyDescriptorFactory descriptorFactory) {
        return new DefaultDependencyDescriptorFactory(
            new ProjectIvyDependencyDescriptorFactory(excludeRuleConverter),
            descriptorFactory);
    }

    LocalConfigurationMetadataBuilder createLocalConfigurationMetadataBuilder(DependencyDescriptorFactory dependencyDescriptorFactory,
                                                                              ExcludeRuleConverter excludeRuleConverter) {
        return new DefaultLocalConfigurationMetadataBuilder(dependencyDescriptorFactory, excludeRuleConverter);
    }

    LocalComponentMetadataBuilder createLocalComponentMetaDataBuilder(LocalConfigurationMetadataBuilder localConfigurationMetadataBuilder) {
        return new DefaultLocalComponentMetadataBuilder(localConfigurationMetadataBuilder);
    }

    ResourceConnectorFactory createFileConnectorFactory() {
        return new FileConnectorFactory();
    }

    ProducerGuard<ExternalResourceName> createProducerAccess() {
        return ProducerGuard.adaptive();
    }

    InputArtifactAnnotationHandler createInputArtifactAnnotationHandler() {
        return new InputArtifactAnnotationHandler();
    }

    InputArtifactDependenciesAnnotationHandler createInputArtifactDependenciesAnnotationHandler() {
        return new InputArtifactDependenciesAnnotationHandler();
    }

    ArtifactTransformParameterScheme createArtifactTransformParameterScheme(InspectionSchemeFactory inspectionSchemeFactory, InstantiatorFactory instantiatorFactory) {
        // TODO - should decorate
        InstantiationScheme instantiationScheme = instantiatorFactory.injectScheme();
        InspectionScheme inspectionScheme = inspectionSchemeFactory.inspectionScheme(ImmutableSet.of(Input.class, InputFile.class, InputFiles.class, InputDirectory.class, Classpath.class, CompileClasspath.class, Nested.class, Inject.class, Console.class, Internal.class));
        return new ArtifactTransformParameterScheme(instantiationScheme, inspectionScheme);
    }

    ArtifactTransformActionScheme createArtifactTransformActionScheme(InspectionSchemeFactory inspectionSchemeFactory, InstantiatorFactory instantiatorFactory) {
        InstantiationScheme instantiationScheme = instantiatorFactory.injectScheme(ImmutableSet.of(InputArtifact.class, InputArtifactDependencies.class));
        InstantiationScheme legacyInstantiationScheme = instantiatorFactory.injectScheme();
        InspectionScheme inspectionScheme = inspectionSchemeFactory.inspectionScheme(ImmutableSet.of(InputArtifact.class, InputArtifactDependencies.class, Inject.class, Classpath.class, CompileClasspath.class));
        return new ArtifactTransformActionScheme(instantiationScheme, inspectionScheme, legacyInstantiationScheme);
    }
}
