package org.openedx.course.data.storage

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import org.openedx.core.data.model.GradingPolicyDb
import org.openedx.core.data.model.SectionScoreDb
import org.openedx.core.data.model.room.BlockDb
import org.openedx.core.data.model.room.CourseModeDB
import org.openedx.core.data.model.room.VideoInfoDb
import org.openedx.core.data.model.room.discovery.CourseDateBlockDb
import org.openedx.core.extension.genericType

class CourseConverter {

    @TypeConverter
    fun fromVideoDb(value: VideoInfoDb?): String {
        if (value == null) return ""
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toVideoDb(value: String): VideoInfoDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, VideoInfoDb::class.java)
    }

    @TypeConverter
    fun fromListOfString(value: List<String>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfString(value: String): List<String> {
        val type = genericType<List<String>>()
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromListOfBlockDbEntity(value: List<BlockDb>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfBlockDbEntity(value: String): List<BlockDb> {
        val type = genericType<List<BlockDb>>()
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String> {
        val mapType = genericType<HashMap<String, String>>()
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, String>): String {
        val gson = Gson()
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromListOfCourseDateBlockDb(value: List<CourseDateBlockDb>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfCourseDateBlockDb(value: String): List<CourseDateBlockDb> {
        val type = genericType<List<CourseDateBlockDb>>()
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromCourseModeList(value: List<CourseModeDB>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toCourseModeList(value: String?): List<CourseModeDB>? {
        val type = genericType<List<CourseModeDB>>()
        return Gson().fromJson(value, type)
    }
    @TypeConverter
    fun fromSectionScoreDbList(value: List<SectionScoreDb>?): String =
        Gson().toJson(value)

    @TypeConverter
    fun toSectionScoreDbList(value: String): List<SectionScoreDb> =
        Gson().fromJson(value, object : TypeToken<List<SectionScoreDb>>() {}.type)

    @TypeConverter
    fun fromAssignmentPolicyDbList(value: List<GradingPolicyDb.AssignmentPolicyDb>?): String =
        Gson().toJson(value)

    @TypeConverter
    fun toAssignmentPolicyDbList(value: String): List<GradingPolicyDb.AssignmentPolicyDb> =
        Gson().fromJson(
            value,
            object : TypeToken<List<GradingPolicyDb.AssignmentPolicyDb>>() {}.type
        )

    @TypeConverter
    fun fromGradeRangeMap(value: Map<String, Float>?): String =
        Gson().toJson(value)

    @TypeConverter
    fun toGradeRangeMap(value: String): Map<String, Float> =
        Gson().fromJson(value, object : TypeToken<Map<String, Float>>() {}.type)
}
