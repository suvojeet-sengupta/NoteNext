package com.suvojeet.notenext.data

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectWithChildren(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subProjects: List<Project> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val notes: List<NoteSummaryWithAttachments> = emptyList()
) {
    val hasChildren: Boolean get() = subProjects.isNotEmpty() || notes.isNotEmpty()
    val depth: Int = 0 
    
    fun flattenWithDepth(currentDepth: Int = 0): List<ProjectWithDepth> {
        val result = mutableListOf<ProjectWithDepth>()
        result.add(ProjectWithDepth(project, currentDepth, notes))
        
        for (subProject in subProjects) {
            val subProjectWithChildren = ProjectWithChildren(
                project = subProject,
                subProjects = emptyList(),
                notes = emptyList()
            )
            result.addAll(subProjectWithChildren.flattenWithDepth(currentDepth + 1))
        }
        
        return result
    }
}

data class ProjectWithDepth(
    val project: Project,
    val depth: Int,
    val notes: List<NoteSummaryWithAttachments> = emptyList()
)
