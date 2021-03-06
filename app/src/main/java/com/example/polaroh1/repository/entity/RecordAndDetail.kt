package com.example.polaroh1.repository.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RecordAndDetail(

    @Embedded
    val record: RecordEntity = RecordEntity(),

    @Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = HREntity::class,
        projection = ["hr"]
    )
    val hrList: List<Int> = listOf(),

    @Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = PPGEntity::class,
        projection = ["ppg0"]
    )
    val ppgList: List<Int> = listOf(),

    /*@Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = PPIEntity::class,
        projection = ["ppi"]
    )
    val ppiList: List<Int> = listOf(),*/

    @Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = ACCEntity::class,
        projection = ["x"]
    )
    val accXList: List<Int> = listOf(),

    @Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = ACCEntity::class,
        projection = ["y"]
    )
    val accYList: List<Int> = listOf(),

    @Relation(
        parentColumn = "id",
        entityColumn = "recordId",
        entity = ACCEntity::class,
        projection = ["z"]
    )
    val accZList: List<Int> = listOf(),


    )
