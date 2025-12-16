package org.openedx.app.room

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openedx.core.DatabaseManager
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.module.db.DownloadDao
import org.openedx.course.data.storage.CourseDao
import org.openedx.dashboard.data.DashboardDao
import org.openedx.discovery.data.storage.DiscoveryDao

class DatabaseManager(
    private val courseDao: CourseDao,
    private val dashboardDao: DashboardDao,
    private val discoveryDao: DiscoveryDao,
    private val downloadDao: DownloadDao,
    private val discoveryDao: DiscoveryDao
) : DatabaseManager {

    override fun clearTables() {
        CoroutineScope(Dispatchers.IO).launch {
            courseDao.clearStructureCachedData()
            courseDao.clearEnrollmentCachedData()
            courseDao.clearCachedData()
            dashboardDao.clearCachedData()
            downloadDao.clearOfflineProgress()
            discoveryDao.clearCachedData()
        }
    }
}
