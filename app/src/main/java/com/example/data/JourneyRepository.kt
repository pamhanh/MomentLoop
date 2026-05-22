package com.example.data

import kotlinx.coroutines.flow.Flow

class JourneyRepository(
    private val projectDao: ProjectDao,
    private val momentDao: MomentDao
) {
    val allProjectsFlow: Flow<List<Project>> = projectDao.getAllProjectsFlow()
    val allMomentsFlow: Flow<List<Moment>> = momentDao.getAllMomentsFlow()

    suspend fun getAllProjects(): List<Project> {
        return projectDao.getAllProjects()
    }

    suspend fun getProjectById(id: String): Project? {
        return projectDao.getProjectById(id)
    }

    fun getProjectByIdFlow(id: String): Flow<Project?> {
        return projectDao.getProjectByIdFlow(id)
    }

    suspend fun insertProject(project: Project) {
        projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProjectById(id: String) {
        projectDao.deleteProjectById(id)
    }

    fun getMomentsForProjectFlow(projectId: String): Flow<List<Moment>> {
        return momentDao.getMomentsForProjectFlow(projectId)
    }

    suspend fun getMomentsForProject(projectId: String): List<Moment> {
        return momentDao.getMomentsForProject(projectId)
    }

    suspend fun insertMoment(moment: Moment) {
        momentDao.insertMoment(moment)
    }

    suspend fun updateMoment(moment: Moment) {
        momentDao.updateMoment(moment)
    }

    suspend fun deleteMoment(moment: Moment) {
        momentDao.deleteMoment(moment)
    }
}
