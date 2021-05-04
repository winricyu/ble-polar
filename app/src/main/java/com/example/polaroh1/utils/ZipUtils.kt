package com.example.polaroh1.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {


    fun zip(file: File, zipFile: File) {
        println("ZipUtils.zip , file = [${file}], zipFile = [${zipFile}]")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { output ->
            FileInputStream(file).use { input ->
                BufferedInputStream(input).use { origin ->
                    val entry = ZipEntry(file.name)
                    output.putNextEntry(entry)
                    origin.copyTo(output, 1024)
                }
            }
        }
    }
}