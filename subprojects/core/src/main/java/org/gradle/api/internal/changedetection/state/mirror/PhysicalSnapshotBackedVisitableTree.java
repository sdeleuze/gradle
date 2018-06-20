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

package org.gradle.api.internal.changedetection.state.mirror;

import org.gradle.api.internal.changedetection.state.FileContentSnapshot;

import java.nio.file.Path;
import java.util.ArrayDeque;

@SuppressWarnings("Since15")
public class PhysicalSnapshotBackedVisitableTree implements VisitableDirectoryTree {
    private final String basePath;
    private final PhysicalSnapshot rootDirectory;

    public static final VisitableDirectoryTree EMPTY = new VisitableDirectoryTree() {
        @Override
        public void visit(PhysicalFileTreeVisitor visitor) {
        }
    };

    public PhysicalSnapshotBackedVisitableTree(String path, PhysicalSnapshot rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.basePath = path;
    }

    @Override
    public void visit(final PhysicalFileTreeVisitor visitor) {
        rootDirectory.visitTree(new PhysicalFileVisitor() {
            @Override
            public void visit(Path path, String name, Iterable<String> relativePath, FileContentSnapshot content) {
                visitor.visit(path, basePath, name, relativePath, content);
            }
        }, new ArrayDeque<String>());
    }
}
