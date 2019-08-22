//https://github.com/commonsguy/cw-omnibus/tree/master/ContentProvider/Pipe/app/src/main/java/com/commonsware/android/cp/pipe

package app.zipternet.custom

/***
 * Copyright (c) 2014-2015 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * Covered in detail in the book _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection

abstract class AbstractFileProvider : ContentProvider() {

    override fun query(
        uri: Uri, _projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        var projection = _projection
        if (projection == null) {
            projection = OPENABLE_PROJECTION
        }

        val cursor = MatrixCursor(projection, 1)

        val b = cursor.newRow()

        for (col in projection) {
            if (OpenableColumns.DISPLAY_NAME == col) {
                b.add(getFileName(uri))
            } else if (OpenableColumns.SIZE == col) {
                b.add(getDataLength(uri))
            } else { // unknown, so just add null
                b.add(null)
            }
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return URLConnection.guessContentTypeFromName(uri.toString())
    }

    protected fun getFileName(uri: Uri): String? {
        return uri.lastPathSegment
    }

    protected fun getDataLength(uri: Uri): Long {
        return AssetFileDescriptor.UNKNOWN_LENGTH
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        throw RuntimeException("Operation not supported")
    }

    override fun update(
        uri: Uri, values: ContentValues?, where: String?,
        whereArgs: Array<String>?
    ): Int {
        throw RuntimeException("Operation not supported")
    }

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        throw RuntimeException("Operation not supported")
    }

    companion object {
        private val OPENABLE_PROJECTION = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    }
}
