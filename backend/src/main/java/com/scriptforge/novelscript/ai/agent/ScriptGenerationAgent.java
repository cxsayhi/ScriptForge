package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.entity.ProjectWorkspace;

public interface ScriptGenerationAgent {

    ScriptGenerationResult generateResult(ProjectWorkspace project);

    default String generate(ProjectWorkspace project) {
        return generateResult(project).yaml();
    }
}
