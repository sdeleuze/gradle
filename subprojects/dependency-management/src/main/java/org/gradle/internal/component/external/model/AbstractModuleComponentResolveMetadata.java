/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.component.external.model;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributesSchemaInternal;
import org.gradle.api.internal.attributes.EmptySchema;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.internal.component.external.descriptor.Configuration;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.DefaultIvyArtifactName;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.hash.HashValue;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class AbstractModuleComponentResolveMetadata implements ModuleComponentResolveMetadata {

    private static ImmutableAttributes extractAttributes(AbstractMutableModuleComponentResolveMetadata metadata) {
        return ((AttributeContainerInternal) metadata.getAttributes()).asImmutable();
    }

    private final ImmutableAttributesFactory attributesFactory;
    private final ModuleVersionIdentifier moduleVersionIdentifier;
    private final ModuleComponentIdentifier componentIdentifier;
    private final boolean changing;
    private final boolean missing;
    private final List<String> statusScheme;
    @Nullable
    private final ModuleSource moduleSource;
    private final ImmutableMap<String, Configuration> configurationDefinitions;
    private final ImmutableList<? extends ComponentVariant> variants;
    private final HashValue contentHash;
    private final ImmutableAttributes attributes;

    // Configurations are built on-demand, but only once.
    private final Map<String, ConfigurationMetadata> configurations = Maps.newHashMap();

    public AbstractModuleComponentResolveMetadata(AbstractMutableModuleComponentResolveMetadata metadata) {
        this.componentIdentifier = metadata.getId();
        this.moduleVersionIdentifier = metadata.getModuleVersionId();
        changing = metadata.isChanging();
        missing = metadata.isMissing();
        statusScheme = metadata.getStatusScheme();
        moduleSource = metadata.getSource();
        configurationDefinitions = metadata.getConfigurationDefinitions();
        attributesFactory = metadata.getAttributesFactory();
        contentHash = metadata.getContentHash();
        attributes = extractAttributes(metadata);
        variants = metadata.getVariants();
    }

    public AbstractModuleComponentResolveMetadata(AbstractMutableModuleComponentResolveMetadata metadata, ImmutableList<? extends ComponentVariant> variants) {
        this.componentIdentifier = metadata.getId();
        this.moduleVersionIdentifier = metadata.getModuleVersionId();
        changing = metadata.isChanging();
        missing = metadata.isMissing();
        statusScheme = metadata.getStatusScheme();
        moduleSource = metadata.getSource();
        configurationDefinitions = metadata.getConfigurationDefinitions();
        attributesFactory = metadata.getAttributesFactory();
        contentHash = metadata.getContentHash();
        attributes = extractAttributes(metadata);
        this.variants = variants;
    }

    public AbstractModuleComponentResolveMetadata(AbstractModuleComponentResolveMetadata metadata) {
        this.componentIdentifier = metadata.componentIdentifier;
        this.moduleVersionIdentifier = metadata.moduleVersionIdentifier;
        changing = metadata.changing;
        missing = metadata.missing;
        statusScheme = metadata.statusScheme;
        moduleSource = metadata.moduleSource;
        configurationDefinitions = metadata.configurationDefinitions;
        attributesFactory = metadata.attributesFactory;
        contentHash = metadata.contentHash;
        attributes = metadata.attributes;
        variants = metadata.variants;
    }

    public AbstractModuleComponentResolveMetadata(AbstractModuleComponentResolveMetadata metadata, ModuleSource source) {
        this.componentIdentifier = metadata.componentIdentifier;
        this.moduleVersionIdentifier = metadata.moduleVersionIdentifier;
        changing = metadata.changing;
        missing = metadata.missing;
        statusScheme = metadata.statusScheme;
        configurationDefinitions = metadata.configurationDefinitions;
        attributesFactory = metadata.attributesFactory;
        contentHash = metadata.contentHash;
        attributes = metadata.attributes;
        variants = metadata.variants;

        moduleSource = source;
    }

    /**
     * Clear any cached state, for the case where the inputs are invalidated.
     * This only happens when constructing a copy
     */
    protected void copyCachedState(AbstractModuleComponentResolveMetadata metadata) {
        // Copy built-on-demand state
        configurations.putAll(metadata.configurations);
    }

    @Override
    public ImmutableAttributesFactory getAttributesFactory() {
        return attributesFactory;
    }

    @Override
    public boolean isChanging() {
        return changing;
    }

    @Override
    public boolean isMissing() {
        return missing;
    }

    @Override
    public List<String> getStatusScheme() {
        return statusScheme;
    }

    @Override
    public ModuleComponentIdentifier getId() {
        return componentIdentifier;
    }

    @Override
    public ModuleVersionIdentifier getModuleVersionId() {
        return moduleVersionIdentifier;
    }

    @Override
    public ModuleSource getSource() {
        return moduleSource;
    }

    @Override
    public String toString() {
        return componentIdentifier.getDisplayName();
    }

    @Override
    public Set<String> getConfigurationNames() {
        return configurationDefinitions.keySet();
    }

    @Nullable
    @Override
    public AttributesSchemaInternal getAttributesSchema() {
        return EmptySchema.INSTANCE;
    }

    @Override
    public ImmutableAttributes getAttributes() {
        return attributes;
    }

    @Override
    public HashValue getContentHash() {
        return contentHash;
    }

    @Override
    public String getStatus() {
        return attributes.getAttribute(ProjectInternal.STATUS_ATTRIBUTE);
    }

    @Override
    public ImmutableList<? extends ComponentVariant> getVariants() {
        return variants;
    }

    @Override
    public ModuleComponentArtifactMetadata artifact(String type, @Nullable String extension, @Nullable String classifier) {
        IvyArtifactName ivyArtifactName = new DefaultIvyArtifactName(getModuleVersionId().getName(), type, extension, classifier);
        return new DefaultModuleComponentArtifactMetadata(getId(), ivyArtifactName);
    }

    protected ImmutableMap<String, Configuration> getConfigurationDefinitions() {
        return configurationDefinitions;
    }

    /**
     * If there are no variants defined in the metadata, but the implementation knows how to provide variants it can do that here.
     * If it can not provide variants, absent must be returned to fall back to traditional configuration selection.
     */
    protected Optional<ImmutableList<? extends ConfigurationMetadata>> maybeDeriveVariants() {
        return Optional.absent();
    }


    @Override
    public synchronized ConfigurationMetadata getConfiguration(final String name) {
        return populateConfigurationFromDescriptor(name, configurationDefinitions, configurations);
    }

    private ConfigurationMetadata populateConfigurationFromDescriptor(String name, Map<String, Configuration> configurationDefinitions, Map<String, ConfigurationMetadata> configurations) {
        ConfigurationMetadata populated = configurations.get(name);
        if (populated != null) {
            return populated;
        }

        Configuration descriptorConfiguration = configurationDefinitions.get(name);
        if (descriptorConfiguration == null) {
            return null;
        }

        ImmutableList<String> hierarchy = constructHierarchy(descriptorConfiguration);
        boolean transitive = descriptorConfiguration.isTransitive();
        boolean visible = descriptorConfiguration.isVisible();
        populated = createConfiguration(getId(), name, transitive, visible, hierarchy);
        configurations.put(name, populated);
        return populated;
    }

    private ImmutableList<String> constructHierarchy(Configuration descriptorConfiguration) {
        if (descriptorConfiguration.getExtendsFrom().isEmpty()) {
            return ImmutableList.of(descriptorConfiguration.getName());
        }
        Set<String> accumulator = new LinkedHashSet<String>();
        populateHierarchy(descriptorConfiguration, accumulator);
        return ImmutableList.copyOf(accumulator);
    }

    private void populateHierarchy(Configuration metadata, Set<String> accumulator) {
        accumulator.add(metadata.getName());
        for (String parentName : metadata.getExtendsFrom()) {
            Configuration parent = getConfigurationDefinitions().get(parentName);
            populateHierarchy(parent, accumulator);
        }
    }

    /**
     * Creates a {@link org.gradle.internal.component.model.ConfigurationMetadata} implementation for this component.
     */
    protected abstract ConfigurationMetadata createConfiguration(ModuleComponentIdentifier componentId, String name, boolean transitive, boolean visible, ImmutableList<String> hierarchy);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractModuleComponentResolveMetadata that = (AbstractModuleComponentResolveMetadata) o;
        return changing == that.changing
            && missing == that.missing
            && Objects.equal(moduleVersionIdentifier, that.moduleVersionIdentifier)
            && Objects.equal(componentIdentifier, that.componentIdentifier)
            && Objects.equal(statusScheme, that.statusScheme)
            && Objects.equal(moduleSource, that.moduleSource)
            && Objects.equal(configurationDefinitions, that.configurationDefinitions)
            && Objects.equal(attributes, that.attributes)
            && Objects.equal(variants, that.variants)
            && Objects.equal(contentHash, that.contentHash);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
            moduleVersionIdentifier,
            componentIdentifier,
            changing,
            missing,
            statusScheme,
            moduleSource,
            configurationDefinitions,
            attributes,
            variants,
            contentHash);
    }
}
