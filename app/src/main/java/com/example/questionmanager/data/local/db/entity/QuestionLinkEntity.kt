package com.example.questionmanager.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_links",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_id"),
        Index("child_id")
    ]
)
data class QuestionLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "parent_id")
    val parentId: Long,

    @ColumnInfo(name = "child_id")
    val childId: Long,

    @ColumnInfo(name = "relation_type")
    val relationType: String = "DRILL_DOWN",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
