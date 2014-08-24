package br.com.caelum.panettone.eclipse.builder;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import br.com.caelum.vraptor.panettone.VRaptorCompiler;

public class Builder {

	private final VRaptorCompiler compiler;
	private final IProject project;

	public Builder(IProject project) {
		this.project = project;
		this.compiler = getCompiler(project);
	}
	
	private static VRaptorCompiler getCompiler(IProject project) {
		URI projectPath = project.getLocationURI();
		File baseDir = new File(projectPath);
		return new VRaptorCompiler(baseDir, new ArrayList<>());
	}

	void full() throws CoreException {
		clear();
		project.accept(new VisitToners(this::compile));
	}

	void incremental(IResourceDelta delta) throws CoreException {
		delta.accept(new DeltaVisitor(this::remove, this::compile));
	}

	private void clear() throws CoreException {
		project.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	private void remove(IFile file) {
		compiler.removeJavaVersionOf(file.getFullPath().toPortableString());
	}

	private void compile(IFile file) {
		deleteMarkers(file);
		try {
			Optional<Exception> ex = compiler.compile(file.getLocation().toFile());
			ex.ifPresent(e -> addCompilationMarker(file, e));
		} catch (Exception e1) {
			addCompilationMarker(file, e1);
		}
	}

	private void addCompilationMarker(IFile file, Exception e) {
		addMarker(file, e.getMessage(), 1, IMarker.SEVERITY_ERROR);
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	private static final String MARKER_TYPE = "panettone-eclipse.toneProblem";

	private void addMarker(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

}