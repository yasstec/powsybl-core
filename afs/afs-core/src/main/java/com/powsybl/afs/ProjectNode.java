/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs;

import com.powsybl.afs.storage.NodeInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ProjectNode extends AbstractNodeBase<ProjectFolder> {

    protected final Project project;

    protected final boolean folder;

    protected final ParentCache parentCache;

    protected ProjectNode(ProjectFileCreationContext context, int codeVersion, boolean folder) {
        super(context.getInfo(), context.getStorage(), codeVersion);
        this.project = Objects.requireNonNull(context.getProject());
        this.folder = folder;
        parentCache = new ParentCache(context.getInfo(), context.getStorage(), context.getProject());
    }

    @Override
    public boolean isFolder() {
        return folder;
    }

    @Override
    public Optional<ProjectFolder> getParent() {
        return parentCache.get();
    }

    void setParent(ProjectFolder parent) {
        parentCache.set(parent);
    }

    private static boolean pathStop(ProjectNode projectNode) {
        return !projectNode.getParent().isPresent();
    }

    private static String pathToString(List<String> path) {
        return path.stream().skip(1).collect(Collectors.joining(AppFileSystem.PATH_SEPARATOR));
    }

    @Override
    public NodePath getPath() {
        return NodePath.find(this, ProjectNode::pathStop, ProjectNode::pathToString);
    }

    public Project getProject() {
        return project;
    }

    public void moveTo(ProjectFolder folder) {
        Objects.requireNonNull(folder);
        storage.setParentNode(info.getId(), folder.getId());
        storage.flush();
    }

    public void delete() {
        // has to be done before delete!!!
        invalidate();

        storage.deleteNode(info.getId());
        storage.flush();
    }

    public List<ProjectFile> getBackwardDependencies() {
        return storage.getBackwardDependencies(info.getId())
                .stream()
                .map(this::createProjectFile)
                .collect(Collectors.toList());
    }

    protected void invalidate() {
        // propagate
        getBackwardDependencies().forEach(ProjectNode::invalidate);
    }

    public AppFileSystem getFileSystem() {
        return project.getFileSystem();
    }

    ProjectNode createProjectNode(NodeInfo nodeInfo) {
        Objects.requireNonNull(nodeInfo);
        if (ProjectFolder.PSEUDO_CLASS.equals(nodeInfo.getPseudoClass())) {
            ProjectFolder projectFolder = new ProjectFolder(new ProjectFileCreationContext(nodeInfo, storage, project));
            return projectFolder;
        } else {
            return createProjectFile(nodeInfo);
        }
    }

    ProjectFile createProjectFile(NodeInfo nodeInfo) {
        Objects.requireNonNull(nodeInfo);
        ProjectFileCreationContext context = new ProjectFileCreationContext(nodeInfo, storage, project);
        ProjectFileExtension extension = project.getFileSystem().getData().getProjectFileExtensionByPseudoClass(nodeInfo.getPseudoClass());
        if (extension != null) {
            return extension.createProjectFile(context);
        } else {
            return new UnknownProjectFile(context);
        }
    }
}
